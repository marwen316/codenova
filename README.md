# CodeNova

Site vitrine en français pour CodeNova, SASU de conseil en systèmes et logiciels informatiques à Sartrouville. Angular 21, Spring Boot 3.5 / Java 21 et PostgreSQL 17.

## Fonctionnalités

- Identité graphique ivoire / vert citron, logo SVG original, mise en page responsive, polices auto-hébergées.
- Présentation, expertises, approche, adresse et itinéraire OpenStreetMap, téléphone et e-mail.
- Formulaire avec validation côté navigateur et serveur, consentement obligatoire, champ anti-robot et limitation des envois.
- Administration `/admin` : authentification par session, boîte de réception, statut lu/non lu, filtre, suppression, édition du titre de présentation, de l’introduction et de la disponibilité.
- Mot de passe BCrypt, protection CSRF des opérations authentifiées, cookie de session HttpOnly / SameSite Strict, aucune inscription publique.
- PostgreSQL et migrations Flyway. Purge quotidienne des messages de plus de 365 jours.
- Accueil prérendu, HTML sémantique, métadonnées et données structurées ProfessionalService, robots.txt, image de partage et génération du sitemap une fois le domaine choisi.

## Développement

Prérequis : Node.js 24, npm, Docker avec Compose, Python 3 et curl. Java 21 **JDK** et Maven 3.9.11 sont installés localement dans `.tools` si nécessaire. Aucun fichier secret n’est versionné.

```bash
./scripts/setup.sh
./scripts/dev.sh
```

Le script d’installation installe les dépendances, compile les deux applications et lance PostgreSQL. Il est réexécutable et préserve un `.env` existant. Arrêter le serveur de développement avant de réinstaller les dépendances.

Le script de développement attend la disponibilité réelle de l’API avant de lancer Angular sur le port 4200. L’API utilise le port 8080 ; PostgreSQL, 5432. Les logs de l’API sont dans `/tmp/codenova-api.log`.

Le `.env` local est créé avec des mots de passe aléatoires. Les identifiants de l’administration sont les champs `ADMIN_USERNAME` et `ADMIN_PASSWORD` de ce fichier : les consulter localement, sans les publier. Pour changer le mot de passe, modifier ce fichier puis redémarrer l’API. Ne pas utiliser les valeurs d’exemple de `.env.example` en production.

Chaque tâche cloud est déjà isolée : utiliser ce checkout, sans créer de worktree sauf demande explicite. Les fichiers installés persistent, les processus doivent être relancés.

## Vérifications

PostgreSQL doit être disponible pour les tests Spring. Ceux-ci utilisent des transactions annulées en fin de test ; utiliser une base dédiée pour une CI.

```bash
set -a
source .env
set +a
./scripts/mvn.sh -B verify
cd frontend
npm run build -- --configuration production
npm run test:e2e
```

Les tests navigateur nécessitent les serveurs démarrés et Chromium dans `/usr/bin/chromium`. Ailleurs, installer Chromium et définir `CHROMIUM_PATH`. Le test complet utilise les identifiants de l’environnement, envoie un message de test puis le supprime. Ne pas l’exécuter sur la production. Un test interrompu peut laisser son propre message de test.

## Déploiement

```bash
# Renseigner .env avec des mots de passe uniques et forts.
docker compose --profile app up -d --build
```

Le frontend Nginx est lié à l’interface locale, port 8088, pour être placé derrière un reverse proxy HTTPS. L’API n’est pas publiée par Docker. Installer le certificat TLS sur le proxy public et définir `COOKIE_SECURE=true` pour le domaine HTTPS. Ne pas exposer le serveur Angular de développement. Nginx apporte les limites de connexion, des en-têtes de sécurité et le proxy API. Le backend limite les contacts à 5 par heure et par IP ; ajuster si nécessaire pour vos usages. La limite applicative est en mémoire et prévue pour une instance ; un déploiement multi-instance nécessitera un stockage partagé.

Pour le domaine choisi :

```bash
cd frontend
npm run build -- --configuration production
SITE_URL=https://votre-domaine.fr npm run seo
```

Cette dernière commande ajoute l’URL canonique, l’URL de partage, l’image sociale, le sitemap et sa déclaration dans robots.txt aux fichiers compilés. Elle n’invente pas de domaine. Avec Docker, passer `--build-arg SITE_URL=https://votre-domaine.fr` au build frontend (ou définir l’argument dans Compose). Déployer les fichiers compilés après génération. Soumettre le sitemap à Search Console après publication. Un changement du contenu via l’administration apparaît immédiatement dans le navigateur ; relancer le build pour actualiser aussi le HTML prérendu lu par les robots. Le prérendu reprend les valeurs initiales du code : synchroniser le texte éditorial avant un build SEO destiné à la production.

Avant publication, compléter les mentions légales : SIREN/RCS, capital social, hébergeur et ses coordonnées. Informations fournies : SASU, responsable de publication Marwen SHILI, adresse 13 B rue de la Féculerie, 78500 Sartrouville, téléphone +33 7 58 62 86 89, e-mail temporaire marwen.shili.codenova@gmail.com. Remplacer cet e-mail dans le template et les données structurées lorsque l’adresse définitive est disponible. Faire valider les mentions légales et la politique de confidentialité pour l’activité réelle.

Prévoir sauvegardes PostgreSQL, supervision, mises à jour des dépendances et politique de restauration avant production. Le formulaire stocke les messages dans l’administration ; aucune notification e-mail automatique n’est configurée.

## Structure

- `frontend/src` : composants et styles Angular.
- `frontend/public/logo.svg` : logo vectoriel téléchargeable ; `social-card.png` : image sociale.
- `backend/src/main` : API, sécurité, entités et migration initiale.
- `backend/src/test` et `frontend/e2e` : tests d’intégration et navigateur.
- `scripts` : installation reproductible, Maven et démarrage.
- `compose.yaml` : PostgreSQL et déploiement des applications via le profil `app`.
