import fs from 'fs';
const p = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features\\auth\\components\\register\\register.component.html';
let content = fs.readFileSync(p, 'utf8');

// Fix the corrupted tags.
// The script created things like: ` / #emailInput="ngModel">`
// Or: ` / required #emailInput="ngModel">`
// First, let's just restore all `<input>` tags to normal.
content = content.replace(/ \/ required #([a-zA-Z]+)Input="ngModel">/g, ' required #$1Input="ngModel" />');
content = content.replace(/ \/ #([a-zA-Z]+)Input="ngModel">/g, ' #$1Input="ngModel" />');

// Selects don't use `/>`, they use `>`.
content = content.replace(/ \/ #([a-zA-Z]+)Input="ngModel">/g, ' #$1Input="ngModel">'); // But wait, selects didn't have `/` ending. The script matched `[^>]*>`. So p1 was `<select ... `. Thus it did `<select ...  required #f="ngModel">`. That is perfectly valid for select.

fs.writeFileSync(p, content);
console.log('Fixed tags');
