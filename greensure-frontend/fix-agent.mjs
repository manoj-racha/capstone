import fs from 'fs';

const p = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features\\admin\\components\\create-agent\\create-agent.component.html';
let content = fs.readFileSync(p, 'utf8');

// The fields are fullName, employeeId, agentType, email, mobile, password, assignedZones. All are required.
const fields = ['fullName', 'employeeId', 'agentType', 'email', 'mobile', 'password', 'assignedZones'];

fields.forEach(f => {
    // Add star to label if not there
    const labelRx = new RegExp(`(<label[^>]*>[^<]+)<\\/label>`, 'g');
    content = content.replace(labelRx, (match, p1) => {
        // since we can't easily map the field name to the exact label by ID (they don't have IDs),
        // we'll just globally add * if it's not present. ALL fields in create agent are required.
        if (!p1.includes('text-red-500')) {
            return p1 + ` <span class="text-red-500">*</span></label>`;
        }
        return match;
    });

    // Replace static class with dynamic conditional
    const fieldRx = new RegExp(`(<(?:input|select)[^>]*formControlName="${f}"[^>]*class=")([^"]*)(")`, 'g');
    content = content.replace(fieldRx, (match, prefix, classList, suffix) => {
        // remove existing static border bindings
        classList = classList.replace(/border-gs-border/g, 'border-gs-border'); 
        
        const dynamicClass = `[class]="(createForm.get('${f}')?.invalid && (createForm.get('${f}')?.dirty || createForm.get('${f}')?.touched || submitting())) ? '${classList.replace('border-gs-border', 'border-red-500 focus:border-red-600')} w-full bg-gs-bg text-gs-text rounded-lg py-3 focus:outline-none focus:ring-1 transition-colors' : '${classList}'"`;
        
        // Let's make it simpler
        return prefix + classList + suffix + ` [class.border-red-500]="createForm.get('${f}')?.invalid && (createForm.get('${f}')?.dirty || createForm.get('${f}')?.touched || submitting())" [class.focus:border-red-600]="createForm.get('${f}')?.invalid && (createForm.get('${f}')?.dirty || createForm.get('${f}')?.touched || submitting())"`;
    });
});

fs.writeFileSync(p, content);
console.log('Done agent form');
