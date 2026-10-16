FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG SITE_URL
RUN npm run build -- --configuration production && if [ -n "$SITE_URL" ]; then SITE_URL="$SITE_URL" npm run seo; fi
FROM nginx:stable-alpine
COPY --from=build /app/dist/web/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx-site.conf /etc/nginx/codenova-site.conf
# Asset copies can retain mode 0600 from a checkout created with umask 077.
# Nginx workers run without root privileges and must read every public file.
RUN find /usr/share/nginx/html -type d -exec chmod 755 {} + \
    && find /usr/share/nginx/html -type f -exec chmod 644 {} + \
    && chmod 644 /etc/nginx/conf.d/default.conf /etc/nginx/codenova-site.conf
EXPOSE 80
