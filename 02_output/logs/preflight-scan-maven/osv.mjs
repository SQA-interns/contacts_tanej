import fs from 'node:fs';
const lines = fs.readFileSync(process.argv[2], 'utf8').split(/\r?\n/).map(s => s.trim()).filter(l => /^[\w.\-]+:[\w.\-]+:\w+:[\w.\-]+:\w+/.test(l));
const pkgs = lines.map(l => { const [g, a, , v, s] = l.split(':'); return { name: `${g}:${a}`, version: v, scope: s.split(' ')[0] }; });
const res = await fetch('https://api.osv.dev/v1/querybatch', {
  method: 'POST', headers: { 'content-type': 'application/json' },
  body: JSON.stringify({ queries: pkgs.map(p => ({ package: { ecosystem: 'Maven', name: p.name }, version: p.version })) }),
});
const j = await res.json();
const out = [];
for (let i = 0; i < pkgs.length; i++) {
  const ids = (j.results[i].vulns || []).map(v => v.id);
  if (!ids.length) continue;
  const sev = [];
  for (const id of ids) {
    const d = await (await fetch('https://api.osv.dev/v1/vulns/' + id)).json();
    sev.push(`${id}:${d.database_specific?.severity || '?'}`);
  }
  out.push(`${pkgs[i].name}@${pkgs[i].version} (${pkgs[i].scope}): ${sev.join(', ')}`);
}
console.log(`artifacts ${pkgs.length}, with findings ${out.length}`);
console.log(out.join('\n'));
