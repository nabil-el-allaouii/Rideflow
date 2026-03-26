FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration docker

FROM nginx:1.27-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist/RideFlow/browser/ /usr/share/nginx/html/
RUN if [ -f /usr/share/nginx/html/index.docker.html ]; then mv /usr/share/nginx/html/index.docker.html /usr/share/nginx/html/index.html; fi

EXPOSE 80
