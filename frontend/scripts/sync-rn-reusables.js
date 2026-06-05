#!/usr/bin/env node
/* eslint-disable */
/**
 * sync-rn-reusables.js
 *
 * Mirrors `npx @react-native-reusables/cli@latest add <component>` against
 * the public registry at https://reactnativereusables.com/r/nativewind/<name>.json.
 *
 * Why this script exists:
 *  - rn-reusables CLI v1.0.0 is greenfield-only and creates a new my-app/ folder
 *    even when run in an existing Expo Router project (FE-S2 Sub-B finding,
 *    2026-06-03). See docs/rn-reusables-sync.md for context.
 *  - The CLI internally fetches the same JSON endpoints used here.
 *
 * Re-sync: edit COMPONENTS list, run `node scripts/sync-rn-reusables.js`.
 */
const fs = require('fs');
const path = require('path');
const https = require('https');

const REGISTRY = 'https://reactnativereusables.com/r/nativewind';
const COMPONENTS = [
  'button', 'card', 'dialog', 'input', 'label',
  'avatar', 'badge', 'separator', 'tooltip', 'icon',
];
const ROOT = path.resolve(__dirname, '..');
const UI_DIR = path.join(ROOT, 'components', 'ui');
const LIB_DIR = path.join(ROOT, 'lib');

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, { headers: { Accept: 'application/json' } }, (res) => {
        if (res.statusCode !== 200) {
          return reject(new Error(`${url} -> HTTP ${res.statusCode}`));
        }
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            reject(e);
          }
        });
      })
      .on('error', reject);
  });
}

function rewriteImports(src) {
  return src
    .replace(/@\/registry\/nativewind\/components\/ui\//g, '@/components/ui/')
    .replace(/@\/registry\/nativewind\/lib\//g, '@/lib/')
    .replace(/@\/registry\/nativewind\//g, '@/');
}

function targetFor(filePath) {
  const filename = path.basename(filePath);
  if (filePath.includes('/lib/')) return path.join(LIB_DIR, filename);
  return path.join(UI_DIR, filename);
}

async function processComponent(name, seen, written) {
  if (seen.has(name)) return;
  seen.add(name);
  const url = `${REGISTRY}/${name}.json`;
  process.stdout.write(`Fetching ${name}... `);
  const json = await fetchJson(url);
  for (const file of json.files || []) {
    const target = targetFor(file.path);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, rewriteImports(file.content));
    written.push(path.relative(ROOT, target));
  }
  console.log('ok');
  for (const dep of json.registryDependencies || []) {
    const depName = path.basename(dep, '.json');
    await processComponent(depName, seen, written);
  }
}

(async () => {
  fs.mkdirSync(UI_DIR, { recursive: true });
  const seen = new Set();
  const written = [];
  for (const name of COMPONENTS) {
    await processComponent(name, seen, written);
  }
  console.log(`\nWrote ${written.length} files:`);
  for (const f of written.sort()) console.log(`  ${f}`);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
