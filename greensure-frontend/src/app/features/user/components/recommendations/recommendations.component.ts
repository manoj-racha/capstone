import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../../core/models/api-response';
import { RecommendationResponse } from '../../../../core/models/score';

@Component({
    selector: 'app-recommendations',
    imports: [CommonModule],
    templateUrl: './recommendations.component.html'
})
export class RecommendationsComponent implements OnInit {
    private http = inject(HttpClient);

    recommendations = signal<RecommendationResponse[]>([]);

    error = signal('');

    ngOnInit(): void {
        const headers = new HttpHeaders({
            'Authorization': `Bearer ${localStorage.getItem('token')}`
        });

        this.http.get<ApiResponse<RecommendationResponse[]>>(
            `${environment.apiUrl}/recommendations/my`,
            { headers }
        ).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.recommendations.set(res.data);
                } else {
                    this.error.set(res.error || 'Failed to load recommendations');
                }
            },
            error: (err) => {
                this.error.set(err.error?.error || 'Failed to load recommendations');
            }
        });
    }
}
