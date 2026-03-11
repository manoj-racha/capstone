import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import {
    LucideAngularModule,
    Shield,
    Leaf,
    Car,
    Home,
    Building2,
    CheckCircle,
    AlertTriangle,
    Trophy,
    TrendingUp,
    Users,
    FileText,
    ArrowRight,
    Star,
    Lock,
    Phone,
    Mail,
    ChevronRight,
    BadgeCheck,
    Sprout,
    ClipboardList,
    UserCheck,
    Coins
} from 'lucide-angular';

@Component({
    selector: 'app-landing',
    imports: [RouterLink, LucideAngularModule],
    templateUrl: './landing.component.html'
})
export class LandingComponent implements OnInit {

    private authService = inject(AuthService);
    private router = inject(Router);

    // ── Icon Registry ────────────────────────────────────────
    readonly Shield = Shield;
    readonly Leaf = Leaf;
    readonly Car = Car;
    readonly Home = Home;
    readonly Building2 = Building2;
    readonly CheckCircle = CheckCircle;
    readonly AlertTriangle = AlertTriangle;
    readonly Trophy = Trophy;
    readonly TrendingUp = TrendingUp;
    readonly Users = Users;
    readonly FileText = FileText;
    readonly ArrowRight = ArrowRight;
    readonly Star = Star;
    readonly Lock = Lock;
    readonly Phone = Phone;
    readonly Mail = Mail;
    readonly ChevronRight = ChevronRight;
    readonly BadgeCheck = BadgeCheck;
    readonly Sprout = Sprout;
    readonly ClipboardList = ClipboardList;
    readonly UserCheck = UserCheck;
    readonly Coins = Coins;

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
