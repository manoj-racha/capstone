import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../core/services/admin.service';

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
        agentType: ['FIELD_AGENT', Validators.required],
        fullName: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        mobile: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
        password: ['', [Validators.required, Validators.minLength(6)]],
        employeeId: ['', Validators.required],
        assignedZones: ['', Validators.required]
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

        this.adminService.createAgent(this.createForm.value).subscribe({
            next: (res) => {
                this.submitting.set(false);
                if (res.success) {
                    this.router.navigate(['/admin/agents']);
                } else {
                    this.error.set(res.error || 'Failed to create agent.');
                }
            },
            error: (err) => {
                this.submitting.set(false);
                this.error.set(err.error?.error || 'An error occurred during agent creation.');
            }
        });
    }
}
