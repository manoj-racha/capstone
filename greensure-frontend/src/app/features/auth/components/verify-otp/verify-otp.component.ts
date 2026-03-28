import { Component, signal, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import {
  LucideAngularModule,
  Leaf,
  AlertTriangle,
  CheckCircle,
  Mail,
  RefreshCw
} from 'lucide-angular';

@Component({
  selector: 'app-verify-otp',
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './verify-otp.component.html'
})
export class VerifyOtpComponent implements OnInit, OnDestroy {

  readonly Leaf = Leaf;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle = CheckCircle;
  readonly Mail = Mail;
  readonly RefreshCw = RefreshCw;

  email = signal('');
  errorMessage = signal('');
  successMessage = signal('');
  loading = signal(false);
  resendCooldown = signal(0);

  private cooldownInterval: ReturnType<typeof setInterval> | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  otpForm = this.fb.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  ngOnInit(): void {
    const storedEmail = sessionStorage.getItem('gs_otp_email');
    if (!storedEmail) {
      this.router.navigate(['/register']);
      return;
    }
    this.email.set(storedEmail);
    this.startCooldown();
  }

  ngOnDestroy(): void {
    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }
  }

  onVerify(): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    const otp = this.otpForm.controls.otp.value ?? '';

    this.authService.verifyOtp({ email: this.email(), otp }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.successMessage.set('Account verified! Redirecting...');
          sessionStorage.removeItem('gs_otp_email');
          const role = res.data.role;
          setTimeout(() => {
            if (role === 'ADMIN') {
              this.router.navigate(['/admin/dashboard']);
            } else if (role === 'AGENT') {
              this.router.navigate(['/agent/dashboard']);
            } else {
              this.router.navigate(['/user/welcome']);
            }
          }, 1000);
        } else {
          this.errorMessage.set(res.message || 'Verification failed');
        }
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Verification failed';
        if (msg.toLowerCase().includes('expired')) {
          this.errorMessage.set('OTP expired. Please click "Resend OTP" to get a new code.');
        } else {
          this.errorMessage.set(msg);
        }
      }
    });
  }

  onResend(): void {
    if (this.resendCooldown() > 0) return;
    this.errorMessage.set('');

    this.authService.resendOtp(this.email()).subscribe({
      next: (res) => {
        if (res.success) {
          this.toast.success('OTP resent to ' + this.email());
        } else {
          this.toast.info(res.message || 'If this email is registered, a new OTP has been sent.');
        }
        this.startCooldown();
      },
      error: (err) => {
        const message = err.error?.message || err.error?.error;
        if (message) {
          this.toast.info(message);
        } else {
          this.toast.info('If this email is registered, a new OTP has been sent.');
        }
        this.startCooldown();
      }
    });
  }

  private startCooldown(): void {
    this.resendCooldown.set(60);
    if (this.cooldownInterval) clearInterval(this.cooldownInterval);
    this.cooldownInterval = setInterval(() => {
      const current = this.resendCooldown();
      if (current <= 1) {
        this.resendCooldown.set(0);
        if (this.cooldownInterval) clearInterval(this.cooldownInterval);
      } else {
        this.resendCooldown.set(current - 1);
      }
    }, 1000);
  }

  get maskedEmail(): string {
    const e = this.email();
    if (!e) return '';
    const [local, domain] = e.split('@');
    if (local.length <= 2) return e;
    return local[0] + '***' + local[local.length - 1] + '@' + domain;
  }
}
