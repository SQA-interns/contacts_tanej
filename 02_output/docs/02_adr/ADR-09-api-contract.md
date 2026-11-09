# ADR-09: The API contract and generated client types

- Status: accepted · Phase 2 · Drivers: AR-13, R-017-11, AC-017-11, AC-017-07, DoD-P05

## Context

The architecture requires the OpenAPI description to be generated from the code, and the web app's types to be generated from it so UI and API cannot drift (AR-13). The phase 2 gate also requires a validated contract for every interface before the code exists.

## Options

1. Code first: write controllers, let springdoc generate the description, generate types from it. No contract exists in phase 2.
2. Contract first: write `docs/02_contracts/openapi.yaml`, generate the client types from it, and make a backend test fail when the description springdoc generates differs from the contract.
3. Contract first with server code generated from it. Rejected: generated controllers fight the layering of ADR-01.

## Decision

Option 2.

- `docs/02_contracts/openapi.yaml` (+ `schemas.yaml`) is the agreed interface. It is validated by a parser (`npm run contracts:validate`, log in `out/logs/contracts-validation.log`).
- `springdoc-openapi-starter-webmvc-ui` serves the description of the running code at `/v3/api-docs` (R-017-11); the Swagger UI is switched off.
- **Drift test** (task T-03, `OpenApiContractIT`): fetches `/v3/api-docs` and compares, per path and method, the operationId, parameters, request schema, response statuses and schema property names with the contract; any difference fails the build. This is the check for AR-13 "generated from the code" in both directions.
- **Web app types** are generated from the contract with `openapi-typescript` (`npm run api:types`) into `src/lib/api/schema.d.ts` (committed, not formatted or linted); CI regenerates them and fails when `git diff` shows a change.
- **Isolation tests** (AC-017-07, DoD-P05) read the contract: every operation whose path contains `{groupId}` is called by a non-member and must answer 404; the test list is generated from the same file, so a new endpoint cannot be forgotten.
- A contract change is a commit of its own and comes before the code that needs it.

## Consequences

- One dependency is added for the type generator (`openapi-typescript`, decision D-24). `@apidevtools/swagger-parser`, `ajv`, `ajv-formats` validate the contracts.
- The contract is the first thing `/implement` reads for an endpoint; where the contract and a story disagree, the story wins and the contract is corrected in its own commit.
