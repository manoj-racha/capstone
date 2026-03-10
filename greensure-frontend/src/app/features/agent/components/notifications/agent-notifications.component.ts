import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { NotificationService } from '../../../../core/services/notification.service';
import { NotificationResponse } from '../../../../core/models/notification';

@Component({
    selector: 'app-agent-notifications',
    standalone: true,
    imports: [CommonModule, DatePipe],
    templateUrl: './agent-notifications.component.html'
})
export class AgentNotificationsComponent implements OnInit {
    private notificationService = inject(NotificationService);

    notifications = signal<NotificationResponse[]>([]);

    error = signal<string>('');

    ngOnInit(): void {
        this.loadNotifications();
    }

    loadNotifications(): void {
        this.notificationService.getMyNotifications().subscribe({
            next: (res: any) => {
                if (res.success && res.data) {
                    this.notifications.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load notifications.');
                }
            },
            error: (err: any) => {
                this.error.set(err.error?.error || 'Failed to load notifications.');
            }
        });
    }

    markAsRead(id: number): void {
        const notif = this.notifications().find(n => n.notificationId === id);
        if (!notif || notif.status === 'READ') return; // Already read

        this.notificationService.markAsRead(id).subscribe({
            next: (res) => {
                if (res.success) {
                    // Update local state
                    this.notifications.update(current =>
                        current.map(n => n.notificationId === id ? { ...n, status: 'READ' } : n)
                    );
                }
            }
        });
    }
}
