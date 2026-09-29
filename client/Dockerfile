FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app
RUN npm install -g serve@14.2.4
COPY --from=build --chown=node:node /app/dist ./dist
COPY --chown=node:node runtime-config.cjs ./runtime-config.cjs
USER node
EXPOSE 8080
CMD ["sh", "-c", "node runtime-config.cjs && serve -s dist -l 8080"]
