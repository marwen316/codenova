# Alertes e-mail du formulaire de contact

Les messages restent enregistrés dans l’administration. Lorsqu’elles sont activées, les alertes sont mises en attente dans PostgreSQL avec le message, puis envoyées en arrière-plan (vérification toutes les 30 secondes). Une panne SMTP ne fait pas échouer le formulaire et ne perd pas le message.

La redirection `contact@codenova-solutions.fr` vers Gmail sert à recevoir du courrier ; elle ne fournit pas de compte SMTP pour l’envoi. La configuration proposée utilise donc Gmail pour l’envoi et la réception directe des alertes. L’adresse expéditrice doit être le compte Gmail authentifié ; ne pas lui substituer une simple adresse de redirection. Le champ Reply-To contient l’adresse du visiteur pour lui répondre depuis la messagerie.

## Configurer Gmail sur le VPS

1. Activer la validation en deux étapes sur le compte Google, puis créer un [mot de passe d’application](https://myaccount.google.com/apppasswords). Cette fonction peut être indisponible sur certains comptes Google ; dans ce cas utiliser un autre fournisseur SMTP. Le mot de passe habituel du compte Gmail n’est pas adapté.
2. Modifier uniquement le `.env` du VPS, sans publier ses valeurs dans Git ou le chat. Ajouter :

```dotenv
CONTACT_ALERTS_ENABLED=true
CONTACT_ALERTS_FROM=marwen.shili.codenova@gmail.com
CONTACT_ALERTS_TO=marwen.shili.codenova@gmail.com
CONTACT_ALERTS_ADMIN_URL=https://codenova-solutions.fr/admin
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=marwen.shili.codenova@gmail.com
SMTP_PASSWORD=remplacer-par-le-mot-de-passe-application-google
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_SSL=false
```

Saisir le mot de passe d’application sans les espaces de présentation. `CONTACT_ALERTS_TO` peut être remplacé par `contact@codenova-solutions.fr` pour utiliser la redirection existante ; la réception directe sur Gmail évite cette étape supplémentaire. L’adresse de destination est unique et ne provient pas du formulaire public.

3. Déployer le backend en conservant le projet et les données :

```bash
cd /opt/codenova-recette
git pull --ff-only origin main
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml \
  --profile app up -d --build --no-deps --force-recreate backend
```

La migration V4 ajoute les colonnes de suivi des alertes, sans modifier les messages existants. Les anciens messages ne déclenchent pas d’alertes rétroactives.

4. Attendre la disponibilité du backend puis envoyer un nouveau message depuis le formulaire public. Vérifier sa présence dans l’administration, puis la réception dans Gmail (y compris Spam) après environ 30 secondes. Le bouton Répondre doit viser le visiteur. La validation des tests locaux ne garantit pas la délivrabilité Gmail : ce test réel nécessite la configuration SMTP sur le VPS.

## Fonctionnement et diagnostic

L’envoi est tenté au maximum cinq fois : nouvel essai après 1, 2, 4 puis 8 minutes, en plus de la fréquence de traitement de 30 secondes. La file reste en base après un redémarrage. Après cinq échecs, l’alerte passe en `FAILED` et le message reste disponible dans l’administration. La livraison SMTP est effectuée au moins une fois : une interruption entre l’acceptation SMTP et l’enregistrement du succès peut entraîner un doublon. Le traitement est prévu pour une seule instance du backend.

Le serveur utilise TLS avec contrôle du certificat, authentification SMTP et délais réseau limités. Les logs d’échec indiquent uniquement l’identifiant du message, le numéro de tentative et le statut, sans contenu du message ni identifiants SMTP :

```bash
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml \
  --profile app logs --tail=100 backend
```

Contrôler le mot de passe d’application, les adresses, les restrictions du compte Google et l’autorisation des connexions sortantes TCP 587 chez l’hébergeur. Une acceptation SMTP n’est pas une garantie d’arrivée dans la boîte principale. Ne pas activer les logs de debug SMTP en production, qui peuvent exposer des informations personnelles.

Pour désactiver, définir `CONTACT_ALERTS_ENABLED=false` puis recréer le backend. Les nouvelles demandes sont alors uniquement enregistrées dans l’administration. Les alertes déjà en attente restent en base et seront reprises si l’on réactive l’envoi. Les secrets SMTP sont des paramètres serveur, indépendants du contenu éditable du site.

Un autre fournisseur SMTP peut être configuré avec les mêmes variables. Pour du TLS implicite en port 465, définir `SMTP_PORT=465`, `SMTP_SSL=true` et `SMTP_STARTTLS=false`, selon les instructions du fournisseur.

Les échecs indiquent une catégorie `reason` sans publier la réponse SMTP complète :

- `AUTHENTICATION_FAILED` : contrôler le compte SMTP et le mot de passe d’application Google (validation en deux étapes nécessaire), puis recréer le backend pour recharger `.env`.
- `NETWORK_TIMEOUT` / `CONNECTION_FAILED` : vérifier la connectivité sortante du VPS vers le port SMTP choisi.
- `DNS_FAILURE` : vérifier la résolution DNS du conteneur.
- `TLS_ERROR` : vérifier l’heure du VPS, les certificats et le couple port/STARTTLS/SSL ; ne pas désactiver la vérification TLS.
- `SMTP_REJECTED` : vérifier l’adresse expéditrice, le destinataire et les règles du fournisseur.
- `INVALID_ADDRESS_OR_CONFIGURATION` : vérifier le format des adresses.
- `SMTP_ERROR` : échec non classé nécessitant un diagnostic SMTP complémentaire ; ne pas transmettre les identifiants ou activer le debug SMTP public.

Une alerte en `FAILED` ne se relance pas automatiquement au redémarrage. Après correction SMTP, effectuer le test avec un nouveau message de contact ; les anciens messages restent consultables dans l’administration.
