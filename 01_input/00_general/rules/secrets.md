# Secrets (`.env`)

> For: agent · Owner: Security officer · Agent: read-only

You never need to see a secret value; the tools that run need it, not you.

- Do not read `.env` with a file tool or any program you start (`node -e`, a script that prints it), and do not print, copy or log its values: no `cat`, `echo`, `env`, `printenv` or `docker compose config` on it, in commands, files, logs, commit messages or the conversation.
- Which keys exist, and whether each is filled: `node system/run.mjs --env-check` (key names and filled, empty or missing; never a value). Whether a secret leaked: `node system/run.mjs --leak-check` (file names and a count only; it looks for the values of keys named like a secret, such as `*_PASSWORD`, `*_KEY`, `*_TOKEN`, `*_SECRET`, so name secret keys that way). These two are the only parts of `system/` a session runs; never any other runner command.
- Pass values to tools without showing them: `docker compose --env-file .env`, or a project script that loads `.env` itself (Node 20.12 or newer: `process.loadEnvFile(".env")`; older: parse the file in the script) and never prints it.
- Before saving output from a command that may print an environment, filter it so no value is written.
- The phase 6 secret-leak check (`skills/sdd-verify`) must find nothing.
