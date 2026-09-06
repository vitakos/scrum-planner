# --- build stage ---
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
# Baked in at image build time; override by rebuilding with --build-arg if the
# backend URL changes.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# --- runtime stage ---
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
