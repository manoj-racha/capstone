import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';

export const routes: Routes = [

  // ── DEFAULT REDIRECT ──────────────────────────────────────
  // When someone visits http://localhost:4200 → go to landing
  {
    path: '',
    redirectTo: 'landing',
    pathMatch: 'full'
  },

  // ── PUBLIC PAGES ──────────────────────────────────────────
  {
    path: 'landing',
    loadComponent: () =>
      import('./features/landing/components/landing/landing.component')
        .then(m => m.LandingComponent)
  },

  // ── AUTH PAGES (no-auth guard: logged-in users get redirected) ──
  {
    path: 'login',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/auth/components/login/login.component')
        .then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/auth/components/register/register.component')
        .then(m => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/auth/components/forgot-password/forgot-password.component')
        .then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/components/reset-password/reset-password.component')
        .then(m => m.ResetPasswordComponent)
  },

  // ── USER PAGES (must be logged in + USER role) ─────────────
  {
    path: 'user/welcome',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/welcome/welcome.component')
        .then(m => m.WelcomeComponent)
  },
  {
    path: 'user/dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/dashboard/dashboard.component')
        .then(m => m.DashboardComponent)
  },
  {
    path: 'user/profile',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/profile/profile.component')
        .then(m => m.ProfileComponent)
  },
  {
    path: 'user/score-history',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/score-history/score-history.component')
        .then(m => m.ScoreHistoryComponent)
  },
  {
    path: 'user/recommendations',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/recommendations/recommendations.component')
        .then(m => m.RecommendationsComponent)
  },
  {
    path: 'user/notifications',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/notifications/notifications.component')
        .then(m => m.NotificationsComponent)
  },
  {
    path: 'user/policies',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/policies/policies.component')
        .then(m => m.PoliciesComponent)
  },
  {
    path: 'user/my-policies',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/user/components/my-policies/my-policies.component')
        .then(m => m.MyPoliciesComponent)
  },

  // ── DECLARATION PAGES (must be logged in + USER role) ──────
  {
    path: 'declaration/start',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-start/declaration-start.component')
        .then(m => m.DeclarationStartComponent)
  },
  {
    path: 'declaration/fill/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-fill/declaration-fill.component')
        .then(m => m.DeclarationFillComponent)
  },
  {
    path: 'declaration/vehicles/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-vehicles/declaration-vehicles.component')
        .then(m => m.DeclarationVehiclesComponent)
  },
  {
    path: 'declaration/review/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-review/declaration-review.component')
        .then(m => m.DeclarationReviewComponent)
  },
  {
    path: 'declaration/history',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-history/declaration-history.component')
        .then(m => m.DeclarationHistoryComponent)
  },

  // ── AGENT PAGES (must be logged in + AGENT role) ───────────
  {
    path: 'agent/dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/dashboard/agent-dashboard.component')
        .then(m => m.AgentDashboardComponent)
  },
  {
    path: 'agent/task/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/task-detail/task-detail.component')
        .then(m => m.TaskDetailComponent)
  },
  {
    path: 'agent/verify/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/verify/verify.component')
        .then(m => m.VerifyComponent)
  },
  {
    path: 'agent/performance',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/performance/performance.component')
        .then(m => m.PerformanceComponent)
  },
  {
    path: 'agent/notifications',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/notifications/agent-notifications.component')
        .then(m => m.AgentNotificationsComponent)
  },

  // ── ADMIN PAGES (must be logged in + ADMIN role) ───────────
  {
    path: 'admin/dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/dashboard/admin-dashboard.component')
        .then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/users/users.component')
        .then(m => m.UsersComponent)
  },
  {
    path: 'admin/users/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/user-detail/user-detail.component')
        .then(m => m.UserDetailComponent)
  },
  {
    path: 'admin/agents',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/agents/agents.component')
        .then(m => m.AgentsComponent)
  },
  {
    path: 'admin/agents/create',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/create-agent/create-agent.component')
        .then(m => m.CreateAgentComponent)
  },
  {
    path: 'admin/agents/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/agent-detail/agent-detail.component')
        .then(m => m.AgentDetailComponent)
  },
  {
    path: 'admin/declarations',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/declarations/declarations.component')
        .then(m => m.DeclarationsComponent)
  },
  {
    path: 'admin/declarations/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/declaration-detail/declaration-detail.component')
        .then(m => m.DeclarationDetailComponent)
  },
  {
    path: 'admin/assignments',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/assignments/assignments.component')
        .then(m => m.AssignmentsComponent)
  },
  {
    path: 'admin/reports',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/reports/reports.component')
        .then(m => m.ReportsComponent)
  },

  // ── FALLBACK ──────────────────────────────────────────────
  // Any unknown URL → go to landing
  {
    path: '**',
    redirectTo: 'landing'
  }
];