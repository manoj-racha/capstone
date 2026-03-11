import fs from 'fs';

const pTs = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features\\declaration\\components\\declaration-fill\\declaration-fill.component.ts';
let tsContent = fs.readFileSync(pTs, 'utf8');

const fields = ['electricityUnits', 'solarUnits', 'lpgCylinders', 'pngUnits', 'biomassKgPerDay', 'numAcUnits', 'acHoursPerDay', 'generatorHoursPerMonth', 'publicTransportKm', 'commercialVehicleKm', 'thirdPartyShipments', 'employeesPrivateVehicle', 'employeesPublicTransport', 'generatorLitersPerMonth', 'boilerCoalKg', 'boilerGasScm', 'paperReamsPerMonth', 'rawMaterialKg'];

fields.forEach(f => {
    tsContent = tsContent.replace(new RegExp(`\\s+${f}: \\[null\\]`, 'g'), `\n    ${f}: [null, [Validators.min(0)]]`);
});
// make electricityUnits required
tsContent = tsContent.replace(/electricityUnits: \[null, \[Validators.min\(0\)\]\]/, "electricityUnits: [null, [Validators.required, Validators.min(0)]]");

if (!tsContent.includes('Validators')) {
    tsContent = tsContent.replace(/import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular\/forms';/, "import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';");
}
fs.writeFileSync(pTs, tsContent);

const pHtml = 'c:\\Hartford\\capstone\\greensure-frontend\\src\\app\\features\\declaration\\components\\declaration-fill\\declaration-fill.component.html';
let htmlContent = fs.readFileSync(pHtml, 'utf8');

// Add min="0" to all type="number"
htmlContent = htmlContent.replace(/type="number"/g, 'type="number" min="0"');

// Add * to Electricity Units
htmlContent = htmlContent.replace(/<label class="block text-gs-muted mb-1 text-sm">Electricity Units \(Monthly\)<\/label>/, '<label class="block text-gs-muted mb-1 text-sm font-semibold">Electricity Units (Monthly) <span class="text-red-500">*</span></label>');

// Let's also ensure electricityUnits input gets red border on invalid
htmlContent = htmlContent.replace(/<input type="number" formControlName="electricityUnits" min="0"[^>]*class="(.*?)"/, (match, classParams) => {
    return `<input type="number" formControlName="electricityUnits" min="0" required class="${classParams}" [class.border-red-500]="fillForm.get('electricityUnits')?.invalid && (fillForm.get('electricityUnits')?.dirty || fillForm.get('electricityUnits')?.touched)" [class.focus:border-red-600]="fillForm.get('electricityUnits')?.invalid && (fillForm.get('electricityUnits')?.dirty || fillForm.get('electricityUnits')?.touched)"`;
});

fs.writeFileSync(pHtml, htmlContent);
console.log('Done script');
