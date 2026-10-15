# CodeNova

Site vitrine en français pour CodeNova, SASU de conseil en systèmes et logiciels informatiques à Sartrouville. Angular 21, Spring Boot 3.5 / Java 21 et PostgreSQL 17.

## Fonctionnalités

- Identité graphique futuriste bleu nuit, violet et cyan, logo SVG inspiré des chevrons de code, mise en page responsive et polices auto-hébergées. Animations lentes du noyau et des orbites, apparition de l’accueil et réactions au survol ; désactivation via `prefers-reduced-motion`.
- Présentation, expertises, approche, adresse et itinéraire OpenStreetMap, téléphone et e-mail.
- Formulaire avec validation côté navigateur et serveur, consentement obligatoire, champ anti-robot et limitation des envois.
- Administration `/admin` : authentification par session, boîte de réception, statut lu/non lu, filtre, suppression, onglet « Contenu du site » séparé des messages, avec recherche et édition de 93 champs publics, du titre de présentation, de l’introduction et de la disponibilité.
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

Avant publication, compléter les mentions légales : SIREN/RCS, capital social, hébergeur et ses coordonnées. Informations fournies : SASU, responsable de publication Marwen SHILI, adresse 13 B rue de la Féculerie, 78500 Sartrouville, téléphone +33 7 58 62 86 89, e-mail temporaire marwen.shili.codenova@gmail.com. Remplacer cet e-mail dans l’onglet « Contenu du site » lorsque l’adresse définitive est disponible ; les liens de contact et les données structurées du navigateur sont actualisés. Faire valider les mentions légales et la politique de confidentialité pour l’activité réelle.

Prévoir sauvegardes PostgreSQL, supervision, mises à jour des dépendances et politique de restauration avant production. Le formulaire stocke les messages dans l’administration ; aucune notification e-mail automatique n’est configurée.

## Structure

- `frontend/src` : composants et styles Angular.
- `frontend/public/logo.svg` : symbole vectoriel ; `logo-full.svg` : logo complet circulaire téléchargeable ; `social-card.png` : image sociale.
- `backend/src/main` : API, sécurité, entités et migration initiale.
- `backend/src/test` et `frontend/e2e` : tests d’intégration et navigateur.
- `scripts` : installation reproductible, Maven et démarrage.
- `compose.yaml` : PostgreSQL et déploiement des applications via le profil `app`.

## Instance de recette isolée et tests de bout en bout

```bash
./scripts/test-deploy.sh
```

Cette commande compile les artefacts de production, démarre le projet Docker `codenova-test` (Nginx, JVM et une base PostgreSQL dédiée) et exécute les tests Playwright contre Nginx sur le port local 8088. Elle utilise les identifiants de `.env` sans les afficher. Le volume `codenova-test_test_data` est indépendant de la base de développement. PostgreSQL et l’API de recette ne sont pas publiés sur l’hôte. Les applications utilisent les artefacts compilés montés en lecture seule, sans reconstruction des Dockerfiles de production.

`TEST_PORT` permet de choisir un autre port. `E2E_BASE_URL` permet de lancer Playwright contre une autre instance de recette ; les serveurs de développement ne sont pas démarrés lorsque cette variable est définie. Utiliser une instance réservée aux tests, car le parcours crée puis supprime son message.

```bash
# État, logs et arrêt de la recette (les données sont conservées).
docker compose -p codenova-test -f compose.test.yaml ps
docker compose -p codenova-test -f compose.test.yaml logs --tail=100
docker compose -p codenova-test -f compose.test.yaml down
```

L’instance cloud ne fournit pas de lien public. Pour une recette sur VPS, il faut un accès SSH configuré et un reverse proxy HTTPS ou un tunnel SSH. Ne pas exposer directement l’administration en HTTP sur Internet. L’accès administrateur et la base de recette doivent utiliser des identifiants propres au serveur cible.


## Gestion du contenu

L’administration comprend deux onglets : **Messages** et **Contenu du site**. Le second organise les informations en rubriques repliables : entreprise et coordonnées, accueil et navigation, expertises, approche et présentation, contact et formulaire, pied de page et mentions légales, logos et référencement. Une recherche filtre les champs. Les changements sont publiés ensemble par « Enregistrer les modifications » ; « Annuler les modifications » rétablit le dernier enregistrement. Passer d’un onglet à l’autre conserve le brouillon, mais recharger la page abandonne les modifications non enregistrées.

Le titre, l’introduction et la disponibilité existants sont préservés par la migration Flyway V2. Les nouveaux champs sont stockés en JSONB dans PostgreSQL, avec des valeurs initiales si aucun remplacement n’est enregistré. Les anciens appels API à trois champs restent compatibles. Les écritures exigent une session administrateur, un jeton CSRF et des champs valides. Les contenus sont rendus comme texte, sans HTML arbitraire.

Les modifications sont visibles après rechargement du site public, sans redéploiement. L’e-mail, le téléphone et l’adresse actualisent aussi les liens correspondants. Le titre SEO, la description, le favicon et les données structurées sont actualisés dans le navigateur. Le HTML prérendu fourni avant l’exécution JavaScript reste un instantané de construction ; son actualisation pour les robots sans JavaScript nécessite une étape de publication adaptée (voir la section référencement ci-dessus).

Les logos se configurent par un chemin de fichier déjà hébergé sur le site (`/logo.svg`, `/logo-full.svg`). Le téléversement de fichiers et le changement du nombre de cartes/sections ne font pas partie de cet éditeur ; les trois expertises et les trois étapes sont modifiables. Les identifiants administrateur, les paramètres de base de données et la rétention automatique des messages restent des paramètres serveur.

Pour déployer une version modifiant l’API ou la base, reconstruire **les deux applications** :

```bash
git pull --ff-only origin main
docker compose -p codenova-recette -f compose.yaml -f compose.recette.yaml --profile app up -d --build backend web
```

La migration est appliquée au démarrage du backend ; ne pas supprimer le volume PostgreSQL.
