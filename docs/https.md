# HTTPS : codenova-solutions.fr

Configuration pour le VPS existant, sans changement de nom de projet Docker ni de volumes. Le projet reste **codenova-recette** : changer ce nom créerait d’autres volumes. Les fichiers `compose.recette.yaml` et `compose.https.yaml` sont deux alternatives, ne pas les combiner.

## Installer le certificat sur le VPS

Conserver les fichiers reçus dans `/root/codenova-certificates/`, hors du dépôt. Ne jamais publier la clé privée, le PFX ou leur mot de passe. Nginx utilise le `.cer` et le `.key` ; le PFX n’est pas requis. Le certificat doit couvrir `codenova-solutions.fr` et `www.codenova-solutions.fr` (contrôlés par le script). Le domaine sans sous-domaine doit être couvert explicitement : un certificat pour le seul `*.codenova-solutions.fr` ne suffit pas.

```bash
cd /opt/codenova-recette
git pull --ff-only origin main
./scripts/prepare-tls.sh \
  /root/codenova-certificates/codenova-solutions.fr_ssl_certificate.cer \
  '/root/codenova-certificates/_.codenova-solutions.fr_private_key.key'
```

Le script accepte les certificats PEM ou DER et les clés lisibles par OpenSSL. Si la clé est chiffrée, saisir sa phrase secrète localement à l’invite OpenSSL. Le script contrôle la correspondance clé/certificat, le domaine, les dates et la chaîne de confiance avant de modifier les fichiers installés. La clé installée est lisible uniquement par root, pour permettre le redémarrage automatique de Nginx.

Si la chaîne n’est pas validée, télécharger le **bundle de certificats intermédiaires (PEM)** auprès du fournisseur SSL et relancer avec son chemin comme troisième argument :

```bash
./scripts/prepare-tls.sh \
  /root/codenova-certificates/codenova-solutions.fr_ssl_certificate.cer \
  '/root/codenova-certificates/_.codenova-solutions.fr_private_key.key' \
  /root/codenova-certificates/chain.pem
```

Les fichiers validés sont installés dans `/etc/codenova/tls/fullchain.pem` et `/etc/codenova/tls/private.key`, mode 600, dossier 700. Un éventuel couple précédent est conservé avec le suffixe `.previous`. La confiance est vérifiée avec les autorités reconnues par OpenSSL sur le VPS ; ne pas contourner un échec de validation.

## Déployer

Les ports TCP 80 et 443 doivent être disponibles sur l’hôte et autorisés dans les pare-feux du VPS et de l’hébergeur. Le DNS A doit viser le VPS ; tout enregistrement AAAA doit correspondre à une connectivité IPv6 configurée (cette configuration Docker publie sur IPv4).

```bash
cd /opt/codenova-recette
docker compose -p codenova-recette \
  -f compose.yaml -f compose.https.yaml --profile app config --quiet
docker compose -p codenova-recette \
  -f compose.yaml -f compose.https.yaml --profile app \
  up -d --build backend web
```

Cette commande conserve PostgreSQL et les médias existants, remplace l’exposition `:8088` par les ports 80/443 et impose `COOKIE_SECURE=true` au backend. Aucun changement de mot de passe ni de `.env` n’est nécessaire. Les cookies Secure nécessitent ensuite une connexion sur le domaine HTTPS ; se reconnecter après le redémarrage du backend.

Le build frontend reçoit `SITE_URL=https://codenova-solutions.fr` pour générer les URL canoniques, le sitemap et robots.txt. Les limites du prérendu éditorial décrites dans le README restent applicables.

## Vérifier

```bash
curl -I http://codenova-solutions.fr/
curl -I https://codenova-solutions.fr/
curl --fail https://codenova-solutions.fr/api/content
curl --fail https://codenova-solutions.fr/api/csrf
curl --fail https://codenova-solutions.fr/sitemap.xml
```

La première réponse doit être 308 avec `Location: https://codenova-solutions.fr/`. HTTPS et les API doivent répondre 200 avec une chaîne reconnue, sans option `-k`. Ouvrir ensuite `https://codenova-solutions.fr/admin`, tester connexion, enregistrement et image, puis le contact public. Le certificat et la chaîne réellement fournis doivent encore être validés sur le VPS : les tests locaux utilisent un certificat de test.

En cas d’erreur :

```bash
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml --profile app ps
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml --profile app logs --tail=100 backend web
```

## Renouvellement

Le certificat fourni est géré manuellement : installer son renouvellement avant la date d’expiration avec le même script, puis tester et recharger Nginx :

```bash
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml --profile app exec web nginx -t
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml --profile app exec web nginx -s reload
```

Les fichiers sont montés par dossier, donc leur remplacement est visible sans reconstruire l’image. Sauvegarder les données existantes selon le README ; ne pas lancer `down -v` pour cette mise à jour.

## Adresse www

Chez le fournisseur DNS, ajouter `www` de type **CNAME**, cible **codenova-solutions.fr** (parfois avec un point final selon l’interface). Supprimer ou remplacer un éventuel enregistrement incompatible pour ce même nom ; un CNAME ne cohabite pas avec un A/AAAA sur `www`. Un A `www` vers `185.229.224.25` est une alternative si le fournisseur ne permet pas le CNAME.

Le certificat installé doit couvrir les deux noms. Le wildcard `*.codenova-solutions.fr` couvre `www`, mais il faut aussi la couverture du domaine sans sous-domaine. Vérifier sur le VPS :

```bash
openssl x509 -in /etc/codenova/tls/fullchain.pem -noout -checkhost www.codenova-solutions.fr
```

La sortie doit indiquer **does match certificate**. Si le certificat ne couvre pas www, le faire réémettre avec les deux noms puis l’installer avec le script avant d’activer cette adresse. Attention : selon la version d’OpenSSL, `-checkhost` peut renvoyer un code zéro même si le nom ne correspond pas ; lire le résultat. Le script contrôle lui-même le résultat textuel.

Après la mise à jour du dépôt, valider la configuration puis recréer uniquement le conteneur web sans reconstruire son image. La recréation garantit que le montage prend bien le nouveau fichier après le remplacement effectué par Git :

```bash
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml --profile app run --rm --no-deps web nginx -t
docker compose -p codenova-recette -f compose.yaml -f compose.https.yaml --profile app up -d --no-deps --force-recreate web
curl -I https://www.codenova-solutions.fr/
```

Réponse attendue : **308**, avec `Location: https://codenova-solutions.fr/`. Les chemins et paramètres sont conservés. L’adresse canonique, le sitemap et l’administration restent sur le domaine sans www. La connexion TLS est validée pour www avant la redirection ; ne pas contourner une erreur de certificat.
