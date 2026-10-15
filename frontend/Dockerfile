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
EXPOSE 80
