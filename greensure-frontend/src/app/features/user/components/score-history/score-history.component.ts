import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ScoreService } from '../../../../core/services/score.service';
import { CarbonScoreDetail } from '../../../../core/models/score';

@Component({
    selector: 'app-score-history',
    imports: [CommonModule, RouterLink],
    templateUrl: './score-history.component.html'
})
export class ScoreHistoryComponent implements OnInit {
    private scoreService = inject(ScoreService);

    history = signal<CarbonScoreDetail[]>([]);

    error = signal('');

    ngOnInit(): void {
        this.scoreService.getMyHistory().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.history.set(res.data);
                } else {
                    this.error.set(res.message || 'Failed to load score history');
                }
            },
            error: (err) => {
                this.error.set(err.error?.message || 'Failed to load score history');
            }
        });
    }
}
