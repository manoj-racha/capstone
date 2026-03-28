import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../features/admin/services/admin.service';

@Component({
    selector: 'app-create-agent',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './create-agent.component.html'
})
export class CreateAgentComponent implements OnInit {
    private adminService = inject(AdminService);
    private fb = inject(FormBuilder);
    private router = inject(Router);

    createForm: FormGroup = this.fb.group({
        fullName: ['', [Validators.required, Validators.minLength(3)]],
        employeeId: ['', [Validators.required]],
        agentType: ['FIELD_AGENT', [Validators.required]],
        email: ['', [Validators.required, Validators.email]],
        mobile: ['', [Validators.required, Validators.pattern('^[6-9][0-9]{9}$')]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        assignedZones: ['', [Validators.required, Validators.pattern('^[1-9][0-9]{5}(,[1-9][0-9]{5})*$')]]
    });

    submitting = signal<boolean>(false);
    error = signal<string>('');

    ngOnInit(): void { }

    onSubmit(): void {
        if (this.createForm.invalid) {
            this.createForm.markAllAsTouched();
            return;
        }

        this.submitting.set(true);
        this.error.set('');

        const formValue = this.createForm.value;
        const payload: import('../../../../core/models/admin').CreateAgentRequest = {
            agentType: formValue.agentType,
            fullName: formValue.fullName,
            email: formValue.email,
            mobile: formValue.mobile,
            password: formValue.password,
            employeeId: formValue.employeeId,
            assignedZones: formValue.assignedZones
        };

        this.adminService.createAgent(payload).subscribe({
            next: (res) => {
                this.submitting.set(false);
                if (res.success) {
                    this.router.navigate(['/admin/agents']);
                } else {
                    this.error.set(res.message || 'Failed to create agent.');
                }
            },
            error: (err) => {
                this.submitting.set(false);
                const msg = err.error?.message || err.error?.error
                    || (err.error?.data ? Object.values(err.error.data).join(', ') : null)
                    || 'An error occurred during agent creation.';
                this.error.set(msg);
            }
        });
    }
}
