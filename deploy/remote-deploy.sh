#!/usr/bin/env bash
# Exécuté sur l'EC2 par GitHub Actions (utilisateur ubuntu).
#
#   bash remote-deploy.sh deployer <version>   installe /tmp/delibera-api.jar comme nouvelle version
#   bash remote-deploy.sh revenir              réactive la version qui tournait avant l'actuelle
#
# Déploiement :
#   1. sauvegarde de la base (les migrations Flyway s'appliquent au démarrage de la nouvelle version) ;
#   2. nouvelle version dans /opt/delibera-api/releases/<version>, lien « current » basculé ;
#   3. redémarrage du service et contrôle de santé (/actuator/health = UP) ;
#   4. en cas d'échec : retour automatique à la version précédente.
# L'instance est préparée par Terraform (dépôt infra, scripts/backend-setup.sh).
set -euo pipefail

ACTION="${1:?action manquante : deployer <version> ou revenir}"
VERSION="${2:-}"

SERVICE=delibera-api
BASE=/opt/delibera-api
RELEASES="$BASE/releases"
JAR_RECU=/tmp/delibera-api.jar
SAUVEGARDES=/var/backups/delibera-api
SANTE_URL=http://127.0.0.1:8080/actuator/health
ATTENTE_MAX=180    # secondes : démarrage de Spring Boot sur une petite instance
GARDER_VERSIONS=5
GARDER_SAUVEGARDES=10

echec() { echo "✖ $*" >&2; exit 1; }

[ -f "$BASE/.provisioned" ] || echec "l'instance n'est pas préparée pour l'API : lancer « terraform apply » dans le dépôt infra"

en_bonne_sante() { curl -fs --max-time 3 "$SANTE_URL" | grep -q '"status":"UP"'; }

attendre_sante() {
  local i
  for i in $(seq 1 "$ATTENTE_MAX"); do
    if en_bonne_sante; then
      echo "  santé : UP après ${i} s"
      return 0
    fi
    sleep 1
  done
  return 1
}

# Bascule « current » vers une version ; « previous » garde celle qui tournait avant.
activer() {
  local cible="$1" actuelle
  actuelle="$(readlink -e "$BASE/current" || true)"
  if [ -n "$actuelle" ] && [ "$actuelle" != "$cible" ]; then
    ln -sfn "$actuelle" "$BASE/previous"
  fi
  ln -sfn "$cible" "$BASE/current"
  sudo /usr/bin/systemctl restart "$SERVICE"
}

# Supprime les anciennes versions, sauf « current » et « previous »
nettoyer_versions() {
  local garder_a garder_b
  garder_a="$(readlink -e "$BASE/current" || true)"
  garder_b="$(readlink -e "$BASE/previous" || true)"
  find "$RELEASES" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' | sort -rn | cut -d' ' -f2- \
    | tail -n +$((GARDER_VERSIONS + 1)) | while read -r ancienne; do
      if [ "$ancienne" != "$garder_a" ] && [ "$ancienne" != "$garder_b" ]; then
        rm -rf -- "$ancienne"
      fi
    done
}

journal() {
  echo "--- dernières lignes du journal de $SERVICE ---"
  sudo /usr/bin/journalctl -u "$SERVICE" -n 80 --no-pager || true
}

sauvegarder_base() {
  local fichier
  fichier="$SAUVEGARDES/avant-$1-$(date -u +%Y%m%dT%H%M%SZ).dump"
  sudo -u postgres pg_dump -Fc -d delibera -f "$fichier"
  echo "  sauvegarde : $fichier"
  # Garde les $GARDER_SAUVEGARDES dernières sauvegardes d'avant déploiement (dossier réservé à postgres)
  sudo find "$SAUVEGARDES" -maxdepth 1 -name 'avant-*.dump' -printf '%T@ %p\n' | sort -rn | cut -d' ' -f2- \
    | tail -n +$((GARDER_SAUVEGARDES + 1)) | while read -r ancienne; do
      sudo rm -f -- "$ancienne"
    done
}

conseil_restauration() {
  cat <<EOF
Si l'ancienne version refuse le schéma déjà migré, restaurer la sauvegarde d'avant déploiement :
  sudo systemctl stop $SERVICE
  sudo -u postgres pg_restore --clean --if-exists -d delibera <fichier .dump de $SAUVEGARDES>
  sudo systemctl start $SERVICE
EOF
}

case "$ACTION" in
  deployer)
    [ -n "$VERSION" ] || echec "identifiant de version manquant"
    [ -f "$JAR_RECU" ] || echec "$JAR_RECU absent"
    NOUVELLE="$RELEASES/$VERSION"
    PRECEDENTE="$(readlink -e "$BASE/current" || true)"
    LIEN_PREVIOUS="$(readlink -e "$BASE/previous" || true)"
    echo "▶ déploiement de la version $VERSION"

    sauvegarder_base "$VERSION"

    rm -rf "$NOUVELLE" && mkdir -p "$NOUVELLE"
    install -m 644 "$JAR_RECU" "$NOUVELLE/delibera-api.jar"
    rm -f "$JAR_RECU"
    touch "$NOUVELLE"   # date de la version = date du déploiement (tri des versions)

    activer "$NOUVELLE"
    if attendre_sante; then
      nettoyer_versions
      echo "✔ version $VERSION en ligne"
      exit 0
    fi

    echo "✖ la version $VERSION ne répond pas"
    journal
    if [ -n "$PRECEDENTE" ] && [ -d "$PRECEDENTE" ] && [ "$PRECEDENTE" != "$NOUVELLE" ]; then
      echo "↩ retour à la version $(basename "$PRECEDENTE")"
      activer "$PRECEDENTE"
      # La version en échec ne doit pas devenir la cible de « revenir »
      if [ -n "$LIEN_PREVIOUS" ]; then ln -sfn "$LIEN_PREVIOUS" "$BASE/previous"; else rm -f "$BASE/previous"; fi
      if attendre_sante; then
        echo "✔ version $(basename "$PRECEDENTE") de nouveau en ligne"
      else
        echo "✖ la version précédente ne répond pas non plus"
        journal
        conseil_restauration
      fi
    fi
    exit 1
    ;;

  revenir)
    ACTUELLE="$(readlink -e "$BASE/current" || true)"
    PRECEDENTE="$(readlink -e "$BASE/previous" || true)"
    [ -n "$PRECEDENTE" ] && [ -d "$PRECEDENTE" ] || echec "aucune version précédente disponible"
    echo "↩ retour de $(basename "${ACTUELLE:-aucune}") à $(basename "$PRECEDENTE")"
    activer "$PRECEDENTE"
    if attendre_sante; then
      echo "✔ version $(basename "$PRECEDENTE") en ligne"
      exit 0
    fi
    journal
    conseil_restauration
    exit 1
    ;;

  *)
    echec "action inconnue : $ACTION (deployer <version> ou revenir)"
    ;;
esac
