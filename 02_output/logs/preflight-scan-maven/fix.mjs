const ids = process.argv.slice(2);
for (const id of ids) {
  const d = await (await fetch('https://api.osv.dev/v1/vulns/' + id)).json();
  const fixes = [];
  for (const a of d.affected || []) for (const r of a.ranges || []) for (const e of r.events || []) if (e.fixed) fixes.push(`${a.package?.name}:${e.fixed}`);
  console.log(id, d.database_specific?.severity, (d.summary || '').slice(0, 80), '| fixed', [...new Set(fixes)].join(' '));
}
