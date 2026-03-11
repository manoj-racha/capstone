import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';
import {
    LucideAngularModule,
    Leaf
} from 'lucide-angular';

@Component({
    selector: 'app-navbar',
    imports: [RouterLink, RouterLinkActive, NotificationBellComponent, LucideAngularModule],
    templateUrl: './navbar.component.html'
})
export class NavbarComponent implements OnInit {

    private authService = inject(AuthService);
    private router = inject(Router);

    isLoggedIn = signal<boolean>(false);
    role = signal<string | null>(null);
    currentFragment = signal<string>('');

    // ── Lucide icons ────────────────────────────────────────
    readonly Leaf = Leaf;

    ngOnInit(): void {
        this.updateNavState();

        // Listen for route changes to update the navbar
        this.router.events.pipe(
            filter(event => event instanceof NavigationEnd)
        ).subscribe((event: any) => {
            // Track fragment for active state highlighting
            if (event.urlAfterRedirects) {
                const url = event.urlAfterRedirects;
                const fragmentIndex = url.indexOf('#');
                if (fragmentIndex !== -1) {
                    this.currentFragment.set(url.substring(fragmentIndex + 1));
                } else {
                    this.currentFragment.set('');
                }
            } else if (event.url) {
                const url = event.url;
                const fragmentIndex = url.indexOf('#');
                if (fragmentIndex !== -1) {
                    this.currentFragment.set(url.substring(fragmentIndex + 1));
                } else {
                    this.currentFragment.set('');
                }
            }
            this.updateNavState();
        });
    }

    private updateNavState(): void {
        this.isLoggedIn.set(this.authService.isLoggedIn());
        this.role.set(this.authService.getRole());
    }

    onLogout(): void {
        if (this.isLoggedIn()) {
            this.authService.logout().subscribe({
                next: () => {
                    this.authService.clearSession();
                    this.router.navigate(['/login']);
                    this.updateNavState();
                },
                error: () => {
                    // Fallback if backend throws error due to expired token
                    this.authService.clearSession();
                    this.router.navigate(['/login']);
                    this.updateNavState();
                }
            });
        }
    }
}
