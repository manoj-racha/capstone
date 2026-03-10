import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../../core/services/notification.service';
import { NotificationResponse } from '../../../../core/models/notification';

@Component({
    selector: 'app-notifications',
    imports: [CommonModule],
    templateUrl: './notifications.component.html'
})
export class NotificationsComponent implements OnInit {
    private notificationService = inject(NotificationService);

    notifications = signal<NotificationResponse[]>([]);

    error = signal('');

    ngOnInit(): void {
        this.loadNotifications();
    }

    private loadNotifications(): void {
        this.notificationService.getMyNotifications().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.notifications.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load notifications');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load notifications');
            }
        });
    }

    markAsRead(id: number, currentStatus: string): void {
        if (currentStatus === 'READ') return;

        this.notificationService.markAsRead(id).subscribe({
            next: (res) => {
                if (res.success) {
                    // Update local state without full reload
                    this.notifications.update(list =>
                        list.map(n => n.notificationId === id ? { ...n, status: 'READ' } : n)
                    );
                }
            }
        });
    }
}
