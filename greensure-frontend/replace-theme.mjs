import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

const projectDir = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features';

const replacements = [
    { regex: /bg-gs-card/g, replace: 'bg-gs-surface' },
    { regex: /border-green-900\/[0-9]+/g, replace: 'border-gs-border' },
    { regex: /border-gs-green\/[0-9]+/g, replace: 'border-gs-dark/20' },
    { regex: /bg-gs-green\/[0-9]+/g, replace: 'bg-gs-dark/10' },
    { regex: /hover:border-gs-green\/[0-9]+/g, replace: 'hover:border-gs-dark/30' },
    { regex: /shadow-gs-green\/[0-9]+/g, replace: 'shadow-gs-dark/10' },
    { regex: /shadow-\[0_.*?rgba\(0,200,90,.*?\)\]/g, replace: 'shadow-md shadow-gs-dark/5' },
    { regex: /gs-green-hover/g, replace: 'gs-mid' },
    { regex: /text-gs-green/g, replace: 'text-gs-dark' },
    { regex: /bg-gs-green/g, replace: 'bg-gs-dark' },
    { regex: /border-gs-green/g, replace: 'border-gs-dark' },
    { regex: /group-hover:text-gs-green/g, replace: 'group-hover:text-gs-dark' },
    { regex: /group-hover:border-gs-green\/[0-9]+/g, replace: 'group-hover:border-gs-dark/30' },
    { regex: /text-gs-bg/g, replace: 'text-white' },
];

function processDirectory(dir) {
    const files = readdirSync(dir);
    for (const file of files) {
        const fullPath = join(dir, file);
        if (statSync(fullPath).isDirectory()) {
            processDirectory(fullPath);
        } else if (fullPath.endsWith('.html') || fullPath.endsWith('.ts')) {
            let content = readFileSync(fullPath, 'utf8');
            let original = content;

            for (const { regex, replace } of replacements) {
                content = content.replace(regex, replace);
            }

            if (content !== original) {
                writeFileSync(fullPath, content, 'utf8');
                console.log(`Updated: ${fullPath}`);
            }
        }
    }
}

console.log('Starting theme replacement...');
processDirectory(projectDir);
// Also do shared components if needed
const sharedDir = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\shared';
processDirectory(sharedDir);
console.log('Done.');
