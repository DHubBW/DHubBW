FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --legacy-peer-deps
COPY . .
ARG EXPO_PUBLIC_API_BASE_URL=/api
ENV EXPO_PUBLIC_API_BASE_URL=${EXPO_PUBLIC_API_BASE_URL}
ARG NODE_OPTIONS="--max-old-space-size=1200"
ENV NODE_OPTIONS=${NODE_OPTIONS}
RUN npm run build-web

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
EXPOSE 443
