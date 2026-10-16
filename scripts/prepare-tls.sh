#!/usr/bin/env bash
set -euo pipefail
# Run on the VPS. Never copy private keys into Git or chat.
if [[ $# -lt 2 || $# -gt 3 ]]; then
 echo "Usage: sudo $0 certificate.cer private_key.key [intermediate-chain.pem]" >&2
 exit 1
fi
command -v openssl >/dev/null || { echo 'OpenSSL est nécessaire.' >&2; exit 1; }
tls_target_dir="${TLS_DIR:-/etc/codenova/tls}"
tls_work_dir="$(mktemp -d)"
trap 'rm -rf "$tls_work_dir"' EXIT
umask 077
if ! openssl x509 -in "$1" -out "$tls_work_dir/certificate.pem" 2>/dev/null; then
 openssl x509 -inform DER -in "$1" -out "$tls_work_dir/certificate.pem"
fi
# If encrypted, OpenSSL asks for the passphrase locally, without displaying it.
openssl pkey -in "$2" -out "$tls_work_dir/private.key"
openssl x509 -in "$tls_work_dir/certificate.pem" -pubkey -noout | openssl pkey -pubin -outform DER > "$tls_work_dir/cert-public.der"
openssl pkey -in "$tls_work_dir/private.key" -pubout -outform DER > "$tls_work_dir/key-public.der"
cmp -s "$tls_work_dir/cert-public.der" "$tls_work_dir/key-public.der" || { echo 'Le certificat et la clé ne correspondent pas.' >&2; exit 1; }
for tls_hostname in codenova-solutions.fr www.codenova-solutions.fr; do
 openssl x509 -in "$tls_work_dir/certificate.pem" -checkhost "$tls_hostname" -noout | grep -q 'does match certificate' || { echo "Le certificat ne couvre pas $tls_hostname." >&2; exit 1; }
done
openssl x509 -in "$tls_work_dir/certificate.pem" -checkend 86400 -noout || { echo 'Certificat expiré ou expirant dans moins de 24 heures.' >&2; exit 1; }
# Preserve a supplied PEM bundle; for a DER certificate, use its converted leaf.
if openssl x509 -in "$1" -noout >/dev/null 2>&1; then
 cat "$1" > "$tls_work_dir/fullchain.pem"
else
 cat "$tls_work_dir/certificate.pem" > "$tls_work_dir/fullchain.pem"
fi
if [[ $# == 3 ]]; then
 printf '\n' >> "$tls_work_dir/fullchain.pem"
 cat "$3" >> "$tls_work_dir/fullchain.pem"
fi
if ! openssl verify -purpose sslserver -verify_hostname codenova-solutions.fr -untrusted "$tls_work_dir/fullchain.pem" "$tls_work_dir/certificate.pem"; then
 echo 'Chaîne non validée : récupérez le bundle des certificats intermédiaires auprès du fournisseur, puis passez-le en troisième argument (format PEM).' >&2
 exit 1
fi
# Validate everything before touching installed files. Retain one previous pair.
install -d -m 700 "$tls_target_dir"
for tls_file in fullchain.pem private.key; do
 if [[ -f "$tls_target_dir/$tls_file" ]]; then
   cp -p "$tls_target_dir/$tls_file" "$tls_target_dir/$tls_file.previous"
 fi
 install -m 600 "$tls_work_dir/$tls_file" "$tls_target_dir/$tls_file"
done
echo "Certificat et clé validés et installés dans $tls_target_dir."
openssl x509 -in "$tls_target_dir/fullchain.pem" -noout -subject -issuer -dates
