import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
    selector: 'app-welcome',
    imports: [RouterLink],
    templateUrl: './welcome.component.html'
})
export class WelcomeComponent {
    private authService = inject(AuthService);
    firstName = this.authService.getFullName()?.split(' ')[0] || 'User';
}
