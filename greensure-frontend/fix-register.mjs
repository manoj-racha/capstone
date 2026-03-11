import fs from 'fs';
const p = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features\\auth\\components\\register\\register.component.html';
let content = fs.readFileSync(p, 'utf8');

// fix broken self-closing tags
content = content.replace(/\/ #([a-zA-Z]+)Input="ngModel">/g, '#$1Input="ngModel" />');

fs.writeFileSync(p, content);
console.log('Fixed syntax');
