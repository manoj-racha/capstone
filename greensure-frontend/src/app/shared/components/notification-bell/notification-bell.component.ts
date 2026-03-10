import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, NavigationEnd, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'app-notification-bell',
    imports: [RouterLink],
    templateUrl: './notification-bell.component.html'
})
export class NotificationBellComponent implements OnInit {

    private notificationService = inject(NotificationService);
    private authService = inject(AuthService);
    private router = inject(Router);

    unreadCount = signal<number>(0);
    notificationLink = signal<string>('/user/notifications');

    ngOnInit(): void {
        this.updateLink();
        this.fetchUnreadCount();

        // Refresh unread count on navigation
        this.router.events.pipe(
            filter(event => event instanceof NavigationEnd)
        ).subscribe(() => {
            this.fetchUnreadCount();
        });
    }

    private updateLink(): void {
        const role = this.authService.getRole();
        if (role === 'AGENT') {
            this.notificationLink.set('/agent/notifications');
        } else {
            this.notificationLink.set('/user/notifications');
        }
    }

    fetchUnreadCount(): void {
        if (!this.authService.isLoggedIn()) return;

        this.notificationService.getUnreadCount().subscribe({
            next: (res) => {
                if (res.success) {
                    this.unreadCount.set(res.data);
                }
            }
        });
    }
}
