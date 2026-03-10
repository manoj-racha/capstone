import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-landing',
    imports: [RouterLink],
    templateUrl: './landing.component.html'
})
export class LandingComponent implements OnInit {

    private authService = inject(AuthService);
    private router = inject(Router);

    ngOnInit(): void {
        // If logged in, redirect away from landing page
        if (this.authService.isLoggedIn()) {
            const role = this.authService.getRole();
            if (role === 'ADMIN') {
                this.router.navigate(['/admin/dashboard']);
            } else if (role === 'AGENT') {
                this.router.navigate(['/agent/dashboard']);
            } else {
                this.router.navigate(['/user/dashboard']);
            }
        }
    }
}
