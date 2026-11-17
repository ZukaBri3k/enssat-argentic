# Étape 1 : Build de l'application React avec Node 22
FROM node:22-alpine AS builder
WORKDIR /app

# Copier uniquement les fichiers de dépendances
COPY package.json pnpm-lock.yaml* package-lock.json* ./

# Installer les dépendances avec npm
RUN npm install

# Argument pour passer l'URL du backend au build Vite (défaut sur production Dokploy)
ARG VITE_API_URL=https://argentic-api.evan-saleck.com
ENV VITE_API_URL=$VITE_API_URL

# Copier le reste du code source et compiler
COPY . .
RUN npm run build

# Étape 2 : Serveur web Nginx ultra léger pour servir le front statique
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Supprimer la page d'accueil par défaut de nginx
RUN rm -rf ./*

# Copier les fichiers compilés depuis le builder
COPY --from=builder /app/dist ./

# Configuration Nginx pour le routage SPA (Single Page Application)
RUN echo 'server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
