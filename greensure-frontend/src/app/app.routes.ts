import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';

export const routes: Routes = [

  // ── DEFAULT REDIRECT ──────────────────────────────────────
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

  // ── AUTH PAGES (no-auth guard) ────────────────────────────
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
    path: 'verify-otp',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/auth/components/verify-otp/verify-otp.component')
        .then(m => m.VerifyOtpComponent)
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

  // ── USER PAGES (authGuard + roleGuard USER) ───────────────
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

  // ── DECLARATION PAGES (authGuard + roleGuard USER) ────────
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
    path: 'declaration/:id/household',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-household/declaration-household.component')
        .then(m => m.DeclarationHouseholdComponent)
  },
  {
    path: 'declaration/:id/vehicle',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-vehicle/declaration-vehicle.component')
        .then(m => m.DeclarationVehicleComponent)
  },
  {
    path: 'declaration/:id/electricity',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-electricity/declaration-electricity.component')
        .then(m => m.DeclarationElectricityComponent)
  },
  {
    path: 'declaration/:id/solar',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-solar/declaration-solar.component')
        .then(m => m.DeclarationSolarComponent)
  },
  {
    path: 'declaration/:id/cooking',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-cooking/declaration-cooking.component')
        .then(m => m.DeclarationCookingComponent)
  },
  {
    path: 'declaration/:id/lifestyle',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-lifestyle/declaration-lifestyle.component')
        .then(m => m.DeclarationLifestyleComponent)
  },
  {
    path: 'declaration/:id/review',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['USER'] },
    loadComponent: () =>
      import('./features/declaration/components/declaration-review/declaration-review.component')
        .then(m => m.DeclarationReviewComponent)
  },


  // ── AGENT PAGES (authGuard + roleGuard AGENT) ─────────────
  {
    path: 'agent/dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/dashboard/agent-dashboard.component')
        .then(m => m.AgentDashboardComponent)
  },
  {
    path: 'agent/task/:assignmentId',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/task-detail/task-detail.component')
        .then(m => m.TaskDetailComponent)
  },
  {
    path: 'agent/workspace/:assignmentId',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['AGENT'] },
    loadComponent: () =>
      import('./features/agent/components/workspace/agent-workspace.component')
        .then(m => m.AgentWorkspaceComponent)
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

  // ── ADMIN PAGES (authGuard + roleGuard ADMIN) ─────────────
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
    path: 'admin/analytics',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/components/analytics/admin-analytics.component')
        .then(m => m.AdminAnalyticsComponent)
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
  {
    path: '**',
    redirectTo: 'landing'
  }
];