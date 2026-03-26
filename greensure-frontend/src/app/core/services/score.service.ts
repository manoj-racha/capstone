import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../models/api-response';
import { CarbonScoreDetail } from '../models/score';

@Injectable({
    providedIn: 'root'
})
export class ScoreService {

    private httpClient: HttpClient = inject(HttpClient);
    private apiUrl: string = environment.apiUrl;

    // GET /score/my-score
    // Fetches the latest carbon score for the logged-in user.
    // Returns: scoreId, totalCo2, perCapitaCo2, zone,
    //   category breakdowns (energy/transport/lifestyle or operations %),
    //   comparisons (cityAverage, nationalAverage, previousYearCo2),
    //   and attached recommendations.
    getMyScore(): Observable<ApiResponse<CarbonScoreDetail>> {
        return this.httpClient.get<ApiResponse<CarbonScoreDetail>>(
            `${this.apiUrl}/score/my-score`
        );
    }

    // GET /score/my-history
    // Fetches all past carbon scores for the logged-in user.
    // Returns an array of CarbonScoreDetail — one per year.
    // Used by score-history component to show year-over-year trends.
    getMyHistory(): Observable<ApiResponse<CarbonScoreDetail[]>> {
        return this.httpClient.get<ApiResponse<CarbonScoreDetail[]>>(
            `${this.apiUrl}/score/my-history`
        );
    }
}
