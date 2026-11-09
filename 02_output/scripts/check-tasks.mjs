// Phase 2 gate: "every story is in exactly one increment" and no R, AC or EX of a story is left without
// a task. Reads docs/02_tasks.md (task table rows and the increment table), expands ranges, and reports.
// Usage: node scripts/check-tasks.mjs   (writes logs/tasks-check.log)
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = join(import.meta.dirname, '..', '..');
const storyDir = join(root, '01_input', '01_project', '01_requirements', 'stories');
const tasks = readFileSync(join(root, '02_output', 'docs', '02_tasks.md'), 'utf8');

const stories = readdirSync(storyDir).filter((n) => n.endsWith('.md')).map((n) => n.replace('.md', '')).sort();
const defined = new Set();
for (const story of stories) {
  for (const line of readFileSync(join(storyDir, `${story}.md`), 'utf8').split('\n')) {
    const match = /^\|\s*((?:R|AC|EX)-\d{3}-\d{2})\s*\|/.exec(line);
    if (match) defined.add(match[1]);
  }
}

const taskRows = tasks.split('\n').filter((line) => /^\| T-(?:\d+|N\d+|X\d+) \|/.test(line));
const covered = new Set();
for (const row of taskRows) {
  for (const m of row.matchAll(/\b(R|AC|EX)-(\d{3})-(\d{2})(?:\.\.(\d{2}))?/g)) {
    const [, prefix, story, from, to] = m;
    for (let n = Number(from); n <= Number(to ?? from); n++) covered.add(`${prefix}-${story}-${String(n).padStart(2, '0')}`);
  }
}
const uncovered = [...defined].filter((id) => !covered.has(id)).sort();

const incrementTable = tasks.slice(tasks.indexOf('## Increments'), tasks.indexOf('## Tasks'));
const rows = incrementTable.split('\n').filter((line) => /^\| INC-/.test(line));
const placement = Object.fromEntries(stories.map((s) => [s, rows.filter((row) => row.includes(s)).map((row) => row.split('|')[1].trim())]));
const badPlacement = stories.filter((s) => placement[s].length !== 1);

const taskStories = new Set(taskRows.map((row) => row.split('|')[2].trim()));
const storiesWithoutTask = stories.filter((s) => !taskStories.has(s));
const perStory = Object.fromEntries(stories.map((s) => [s, taskRows.filter((row) => row.split('|')[2].trim() === s).length]));
const tooMany = stories.filter((s) => perStory[s] > 6);

const log = [
  `Stories: ${stories.length}; tasks: ${taskRows.length}; ids defined in stories: ${defined.size}`,
  uncovered.length ? `NOT COVERED BY A TASK: ${uncovered.join(' ')}` : 'Every R, AC and EX of every story is covered by a task.',
  badPlacement.length ? `NOT IN EXACTLY ONE INCREMENT: ${badPlacement.join(' ')}` : 'Every story is in exactly one increment.',
  storiesWithoutTask.length ? `STORIES WITHOUT A TASK: ${storiesWithoutTask.join(' ')}` : 'Every story has at least one task.',
  tooMany.length ? `MORE THAN SIX TASKS (split into groups): ${tooMany.join(' ')}` : 'No story has more than six tasks.',
];
writeFileSync(join(root, '02_output', 'logs', 'tasks-check.log'), log.join('\n') + '\n');
console.log(log.join('\n'));
process.exit(uncovered.length || badPlacement.length || storiesWithoutTask.length || tooMany.length ? 1 : 0);
