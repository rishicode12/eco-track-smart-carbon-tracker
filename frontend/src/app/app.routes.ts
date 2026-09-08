import { Routes } from '@angular/router';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout.component';
import { AdminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // Public Routes (No Layout)
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent),
    pathMatch: 'full'
  },
  {
    path: 'about',
    loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent)
  },
  
  // Auth Routes
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  
  // Dashboard and core tracking features (wrapped in layout)
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'carbon',
        loadComponent: () => import('./features/carbon/carbon.component').then(m => m.CarbonComponent)
      },
      {
        path: 'goals',
        loadComponent: () => import('./features/goals/goals.component').then(m => m.GoalsComponent)
      },
      {
        path: 'ai',
        loadComponent: () => import('./features/ai/ai.component').then(m => m.AiComponent)
      },
      {
        path: 'challenges',
        loadComponent: () => import('./features/challenges/challenges.component').then(m => m.ChallengesComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./features/reports/reports.component').then(m => m.ReportsComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'admin',
        canActivate: [AdminGuard],
        loadComponent: () => import('./features/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'support',
        loadComponent: () => import('./features/support/support.component').then(m => m.SupportComponent)
      },
      {
        path: 'privacy',
        loadComponent: () => import('./features/privacy-policy/privacy-policy.component').then(m => m.PrivacyPolicyComponent)
      },
      {
        path: 'terms',
        loadComponent: () => import('./features/terms-of-service/terms-of-service.component').then(m => m.TermsOfServiceComponent)
      },
      {
        path: 'docs',
        loadComponent: () => import('./features/docs/docs.component').then(m => m.DocsComponent)
      }
    ]
  },
  
  // Fallback Route
  {
    path: '**',
    redirectTo: ''
  }
];

