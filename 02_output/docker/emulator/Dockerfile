# Firebase Authentication Emulator for the local and test stacks only (environments.md, SR-18).
# Authentication only: no deploy and no other Firebase service (tech-stack.md, firebase-tools).
FROM node:24.13.0-slim
RUN npm install --global --no-fund --no-audit firebase-tools@15.35.0 \
 && npm cache clean --force
WORKDIR /srv
COPY --chown=node:node firebase.json seed-emulator.mjs ./
RUN chown node:node /srv
USER node
ENV FIREBASE_TOOLS_DISABLE_UPDATE_CHECK=true
EXPOSE 9099
HEALTHCHECK --interval=5s --timeout=3s --retries=20 \
  CMD node -e "fetch('http://127.0.0.1:9099/').then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"
CMD ["firebase", "emulators:start", "--only", "auth", "--project", "demo-contacts"]
