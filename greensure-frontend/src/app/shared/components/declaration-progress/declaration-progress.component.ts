import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-declaration-progress',
  imports: [],
  template: `
    <div class="w-full mb-8">
      <div class="flex items-center justify-between">
        @for (step of steps; track step.num; let i = $index) {
          <div class="flex flex-col items-center flex-1">
            <!-- Circle -->
            <div [class]="getStepClass(step.num)">
              @if (completedSteps.includes(step.num)) {
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
              } @else {
                <span class="text-sm font-bold">{{ step.num }}</span>
              }
            </div>
            <!-- Label -->
            <span [class]="currentStep === step.num ? 'text-xs mt-1.5 font-semibold text-green-700' : 'text-xs mt-1.5 text-gray-500'">
              {{ step.label }}
            </span>
            @if (step.optional) {
              <span class="text-[10px] text-gray-400 italic">(optional)</span>
            }
          </div>
          <!-- Connector line -->
          @if (i < steps.length - 1) {
            <div [class]="completedSteps.includes(step.num) ? 'flex-1 h-0.5 bg-green-500 mx-1 mt-[-20px]' : 'flex-1 h-0.5 bg-gray-200 mx-1 mt-[-20px]'"></div>
          }
        }
      </div>
    </div>
  `
})
export class DeclarationProgressComponent {
  @Input() currentStep = 1;
  @Input() completedSteps: number[] = [];

  readonly steps = [
    { num: 1, label: 'Household', optional: false },
    { num: 2, label: 'Vehicle', optional: false },
    { num: 3, label: 'Electricity', optional: false },
    { num: 4, label: 'Solar', optional: true },
    { num: 5, label: 'Cooking', optional: false },
    { num: 6, label: 'Lifestyle', optional: true },
    { num: 7, label: 'Review', optional: false },
  ];

  getStepClass(stepNum: number): string {
    const base = 'w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold transition-all ';
    if (this.completedSteps.includes(stepNum)) {
      return base + 'bg-green-500 text-white';
    }
    if (this.currentStep === stepNum) {
      return base + 'bg-green-100 text-green-700 ring-2 ring-green-500';
    }
    return base + 'bg-gray-100 text-gray-400';
  }
}
