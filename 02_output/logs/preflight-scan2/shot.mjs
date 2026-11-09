import { chromium, firefox, webkit } from '@playwright/test';
const out = process.argv[2];
for (const [name, t] of [['chromium', chromium], ['firefox', firefox], ['webkit', webkit]]) {
  const b = await t.launch();
  const p = await b.newPage();
  await p.setContent('<html><body></body></html>');
  await p.screenshot({ path: name === 'chromium' ? out : out.replace('.png', `-${name}.png`) });
  console.log(name, b.version());
  await b.close();
}
