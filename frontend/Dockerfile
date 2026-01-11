# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Build arguments for environment configuration
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_API_URL=http://localhost:8080

# Set as environment variables for Vite build
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_API_URL=$VITE_API_URL

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source
COPY . .

# Build production bundle
RUN npm run build

# Production stage
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Copy built assets
COPY --from=build /app/dist .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Create pid file and set permissions for non-root user
RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid /usr/share/nginx/html /var/cache/nginx /etc/nginx/conf.d /var/log/nginx

# Switch to non-root user
USER nginx
# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:8080 || exit 1

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
