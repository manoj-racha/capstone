import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { NotificationResponse } from '../models/notification';

@Injectable({
    providedIn: 'root'
})
export class NotificationService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    // GET /notifications/my — fetch all notifications for the logged-in user/agent
    getMyNotifications(): Observable<ApiResponse<NotificationResponse[]>> {
        return this.httpClient.get<ApiResponse<NotificationResponse[]>>(
            `${this.apiUrl}/notifications/my`
        );
    }

    // PUT /notifications/{id}/read — mark a single notification as read
    markAsRead(id: number): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/notifications/${id}/read`,
            {}
        );
    }

    // PUT /notifications/mark-all-read — mark all notifications as read
    markAllAsRead(): Observable<ApiResponse<void>> {
        return this.httpClient.put<ApiResponse<void>>(
            `${this.apiUrl}/notifications/mark-all-read`,
            {}
        );
    }

    // GET /notifications/unread-count — fetch count for the notification bell badge
    getUnreadCount(): Observable<ApiResponse<number>> {
        return this.httpClient.get<ApiResponse<number>>(
            `${this.apiUrl}/notifications/unread-count`
        );
    }
}
