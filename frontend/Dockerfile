# --- build stage ---
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
# Empty by default: same-origin, proxied to the backend container by nginx.conf
# (see nginx.conf). Override with --build-arg only if you need the built
# assets to call a different backend directly.
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# --- runtime stage ---
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
