FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
# Use npm ci for faster, more reliable builds in CI/CD and docker
RUN npm ci --silent --loglevel=error
COPY . .
# Build with optimizations
RUN npm run build && rm -rf node_modules

FROM nginx:1.25-alpine
# Remove default nginx config
RUN rm -f /etc/nginx/conf.d/*.conf
# Copy optimized nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copy built application
COPY --from=builder /app/dist /usr/share/nginx/html

# Add health check
HEALTHCHECK --interval=15s --timeout=2s --start-period=5s --retries=2 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/index.html || exit 1

EXPOSE 80

# Run nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
