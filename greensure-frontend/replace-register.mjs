import fs from 'fs';

const p = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features\\auth\\components\\register\\register.component.html';
let content = fs.readFileSync(p, 'utf8');

// 1. form tag
content = content.replace('<form (ngSubmit)="onRegister()" class="space-y-5">', '<form #registerForm="ngForm" (ngSubmit)="onRegister(registerForm)" class="space-y-5">');

const fields = [
    'fullName', 'email', 'mobile', 'address', 'pinCode', 'city', 'state',
    'numberOfMembers', 'dwellingType', 'businessName', 'gstNumber', 'businessType',
    'numEmployees', 'password', 'confirmPassword'
];

fields.forEach(f => {
    // Add star to label
    const labelRx = new RegExp(`(<label for="${f}"[\\s\\S]*?)(<\\/label>)`);
    content = content.replace(labelRx, (match, p1, p2) => {
        if (!p1.includes('text-red-500')) {
            // strip existing trailing whitespace before </label> if any, but whatever
            return p1.replace(/\s+$/, '') + ` <span class="text-red-500">*</span>` + p2;
        }
        return match;
    });

    // Add #fInput="ngModel" if missing
    const inputRx = new RegExp(`(<(?:input|select)[^>]*name="${f}"[^>]*)>`);
    content = content.replace(inputRx, (match, p1) => {
        if (match.includes(`#${f}Input="ngModel"`)) return match;
        
        const isConditional = ['numberOfMembers', 'dwellingType', 'businessName', 'gstNumber', 'businessType', 'numEmployees'].includes(f);
        let reqAttr = (!isConditional && !match.includes('required')) ? ' required ' : ' ';
        // conditionally required fields are actually manually checked in TS, but if we add required they will block ngSubmit... actually, we use them inside *ngIf basically, so they are removed from DOM when not needed. Adding HTML5 required is safe inside @if block!
        reqAttr = (isConditional && !match.includes('required')) ? ' required ' : reqAttr;

        return p1 + reqAttr + `#${f}Input="ngModel">`;
    });

    // Replace static class with dynamic conditional
    const classStr = `class="w-full bg-white border-gs-border focus:border-gs-dark"`;
    // for pinCode, dwellingType it already has a dynamic [class] due to custom errors, let's leave them or update them.
    if (f === 'pinCode') {
        content = content.replace(/\[class\]="pinCodeError\(\) \? '[^']*' : '[^']*'"/, `[class]="(pinCodeError() || (pinCodeInput.invalid && (pinCodeInput.dirty || pinCodeInput.touched || submitted()))) ? 'w-full bg-white border-red-500 focus:border-red-600' : 'w-full bg-white border-gs-border focus:border-gs-dark'"`);
    } else if (f === 'dwellingType') {
        content = content.replace(/\[class\]="dwellingTypeError\(\) \? '[^']*' : '[^']*'"/, `[class]="(dwellingTypeError() || (dwellingTypeInput.invalid && (dwellingTypeInput.dirty || dwellingTypeInput.touched || submitted()))) ? 'w-full bg-white border-red-500 focus:border-red-600' : 'w-full bg-white border-gs-border focus:border-gs-dark'"`);
    } else {
        const fieldRx = new RegExp(`(<(?:input|select)[^>]*name="${f}"[^>]*)` + classStr.replace(/"/g, '\\"'));
        content = content.replace(fieldRx, `$1[class]="(${f}Input.invalid && (${f}Input.dirty || ${f}Input.touched || submitted())) ? 'w-full bg-white border-red-500 focus:border-red-600' : 'w-full bg-white border-gs-border focus:border-gs-dark'"`);
    }
});

fs.writeFileSync(p, content);
console.log('Done replacement');
