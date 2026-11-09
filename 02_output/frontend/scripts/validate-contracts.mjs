// Validates every contract of docs/02_contracts/ with a parser and writes the result to
// ../logs/contracts-validation.log (phase 2 gate: "every interface has a contract that validates").
import SwaggerParser from '@apidevtools/swagger-parser';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = join(import.meta.dirname, '..', '..');
const contracts = join(root, 'docs', '02_contracts');
const log = [];
let failed = false;

function report(name, ok, detail = '') {
  failed ||= !ok;
  log.push(`${ok ? 'OK  ' : 'FAIL'} ${name}${detail ? ': ' + detail : ''}`);
}

// 1. OpenAPI: parse, resolve every $ref and validate against the OpenAPI 3.1 schema.
try {
  const api = await SwaggerParser.validate(join(contracts, 'openapi.yaml'));
  const operations = Object.values(api.paths).flatMap((item) =>
    Object.entries(item).filter(([method]) => ['get', 'put', 'post', 'patch', 'delete'].includes(method)),
  );
  const ids = operations.map(([, operation]) => operation.operationId);
  report(
    'openapi.yaml',
    new Set(ids).size === ids.length,
    `${operations.length} operations, unique operationIds`,
  );
} catch (error) {
  report('openapi.yaml', false, error.message);
}

// 2. JSON Schemas: compile each and check one valid and one invalid example.
const ajv = new Ajv2020({ strict: false, allErrors: true });
addFormats(ajv);
const examples = {
  'configuration.schema.json': {
    valid: {
      DB_URL: 'jdbc:postgresql://db:5432/contacts',
      DB_USER: 'contacts',
      DB_PASSWORD: 'x',
      FIREBASE_PROJECT_ID: 'demo-contacts',
      FIREBASE_WEB_API_KEY: 'fake-api-key',
      FIREBASE_AUTH_DOMAIN: 'localhost',
      APP_ALLOWED_ORIGIN: 'http://localhost:5173',
    },
    invalid: { DB_URL: 'mysql://x' },
  },
  'id-token-claims.schema.json': {
    valid: {
      iss: 'https://securetoken.google.com/demo-contacts',
      aud: 'demo-contacts',
      sub: 'u1',
      exp: 2,
      iat: 1,
      auth_time: 1,
      email: 'ana.novak@example.com',
      email_verified: true,
    },
    invalid: { iss: 'x' },
  },
  'export-workbook.schema.json': {
    valid: {
      fileName: 'Konferenca 2026_2026-11-09.xlsx',
      sheet: {
        headers: [
          'Ime',
          'Priimek',
          'E-pošta',
          'Telefon',
          'Podjetje',
          'Delovno mesto',
          'Ulica in hišna številka',
          'Poštna številka',
          'Kraj',
          'Država',
          'Opombe',
          'Oznake',
          'kompetenca',
        ],
        frozenHeaderRows: 1,
        rows: [
          [
            'Ana',
            'Novak',
            'ana.novak@example.com',
            '',
            'Acme',
            '',
            '',
            '',
            '',
            '',
            '',
            'udelezenec2025',
            'spletnoProgramiranje',
          ],
        ],
      },
    },
    invalid: { fileName: 'A/B.xlsx', sheet: { headers: [], frozenHeaderRows: 0, rows: [] } },
  },
};
for (const [file, { valid, invalid }] of Object.entries(examples)) {
  try {
    const validate = ajv.compile(JSON.parse(readFileSync(join(contracts, file), 'utf8')));
    const okValid = validate(valid);
    const okInvalid = !validate(invalid);
    report(
      file,
      okValid && okInvalid,
      okValid
        ? okInvalid
          ? 'compiles; valid example accepted, invalid refused'
          : 'invalid example accepted'
        : JSON.stringify(validate.errors),
    );
  } catch (error) {
    report(file, false, error.message);
  }
}

writeFileSync(join(root, 'logs', 'contracts-validation.log'), log.join('\n') + '\n');
console.log(log.join('\n'));
process.exit(failed ? 1 : 0);
