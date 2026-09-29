const fs = require('node:fs');
const path = require('node:path');

const keys = [
  'VITE_AUTH0_DOMAIN',
  'VITE_AUTH0_CLIENT_ID',
  'VITE_AUTH0_AUDIENCE',
  'VITE_API_URL',
  'VITE_WS_URL',
  'VITE_DEMO_MODE',
];
const config = Object.fromEntries(
  keys.filter((key) => process.env[key] !== undefined).map((key) => [key, process.env[key]])
);
const outputPath = process.env.RUNTIME_CONFIG_PATH || path.join(__dirname, 'dist', 'runtime-config.js');
const serialized = JSON.stringify(config).replace(/</g, '\\u003c');

fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, `window.__SPORT_MATCHER_CONFIG__ = ${serialized};\n`);
