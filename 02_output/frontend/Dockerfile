# Web app image (ADR-10, ADR-07): built files served by nginx; configuration from the environment at start.
FROM node:24.13.0-slim AS build
WORKDIR /w
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit
COPY . .
RUN npm run build

FROM nginx:1.29.1
# SB-11: nginx runs as user 101 on port 8080; pid and caches live in writable places
RUN sed -i -e '/^user /d' -e 's|^pid .*;|pid /tmp/nginx.pid;|' /etc/nginx/nginx.conf \
 && rm -f /etc/nginx/conf.d/default.conf \
 && chown -R 101:0 /var/cache/nginx /etc/nginx/conf.d /usr/share/nginx/html \
 && chmod -R g+w /var/cache/nginx /etc/nginx/conf.d /usr/share/nginx/html
COPY docker/default.conf.template /etc/nginx/templates/default.conf.template
COPY docker/40-config.sh /docker-entrypoint.d/40-config.sh
COPY --from=build --chown=101:0 /w/dist /usr/share/nginx/html
# only the CSP_ variables are substituted in the nginx template; $uri and the like stay untouched
ENV NGINX_ENVSUBST_FILTER=^CSP_ CSP_CONNECT_EXTRA=
USER 101
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD curl -fsS http://localhost:8080/ || exit 1
