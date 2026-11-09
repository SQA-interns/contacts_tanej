import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import ts from 'typescript';

/** AR-08, UI-03, UI-04, NFR-10: structure in components, style in token files, text in language files. */
const SRC = join(import.meta.dirname);

function files(dir: string, extensions: string[]): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) return files(path, extensions);
    return extensions.some((extension) => name.endsWith(extension)) && !name.includes('.test.') ? [path] : [];
  });
}

const components = files(SRC, ['.tsx', '.ts']).filter((path) => !path.includes(`${join('src', 'test')}`));
const where = (path: string) => relative(SRC, path);

describe('architecture rules of the web app', () => {
  it('has no raw colour in a component (UI-04)', () => {
    const raw =
      /#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(|\b(?:bg|text|border|ring|fill|stroke)-(?:red|blue|green|gray|slate|zinc|neutral|indigo|yellow|orange|white|black)\b(?:-\d{2,3})?/;
    const offenders = components.filter((path) => raw.test(readFileSync(path, 'utf8'))).map(where);
    expect(offenders).toEqual([]);
  });

  it('has no literal text in JSX and no literal accessible name (AR-08, NFR-10)', () => {
    const offenders: string[] = [];
    for (const path of components.filter((p) => p.endsWith('.tsx'))) {
      const source = ts.createSourceFile(
        path,
        readFileSync(path, 'utf8'),
        ts.ScriptTarget.ES2022,
        true,
        ts.ScriptKind.TSX,
      );
      const visit = (node: ts.Node) => {
        if (ts.isJsxText(node) && /\p{L}/u.test(node.text))
          offenders.push(`${where(path)}: "${node.text.trim()}"`);
        if (
          ts.isJsxAttribute(node) &&
          ['aria-label', 'placeholder', 'title', 'alt'].includes(node.name.getText()) &&
          node.initializer &&
          ts.isStringLiteral(node.initializer)
        ) {
          offenders.push(`${where(path)}: ${node.name.getText()}=${node.initializer.getText()}`);
        }
        ts.forEachChild(node, visit);
      };
      visit(source);
    }
    expect(offenders).toEqual([]);
  });

  it('keeps colour values only in the token files (UI-03)', () => {
    const css = files(SRC, ['.css']);
    const withColours = css
      .filter((path) => /#[0-9a-fA-F]{3,8}\b/.test(readFileSync(path, 'utf8')))
      .map(where);
    expect(withColours.sort()).toEqual(
      [join('styles', 'tokens.final.css'), join('styles', 'tokens.wireframe.css')].sort(),
    );
  });

  it('talks to Firebase only for Authentication (AR-07)', () => {
    const offenders = components
      .filter((path) => /from 'firebase\/(?!app'|auth')/.test(readFileSync(path, 'utf8')))
      .map(where);
    expect(offenders).toEqual([]);
  });
});
