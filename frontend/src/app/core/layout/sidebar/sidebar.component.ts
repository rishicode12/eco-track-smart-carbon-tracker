import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { AuthService } from '../../../features/auth/auth.service';
import { LayoutService } from '../../services/layout.service';
import { GamificationService, EcoProfileResponse } from '../../services/gamification.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  
  private static readonly LEVEL_XP = [0, 1000, 2500, 5000, 10000];

  public layoutService = inject(LayoutService);
  public authService = inject(AuthService);
  public gamificationService = inject(GamificationService);

  public ecoProfile: EcoProfileResponse | null = null;

  public navItems = [
    { route: '/dashboard', label: 'Dashboard', icon: 'bi-grid' },
    { route: '/carbon', label: 'Carbon Tracker', icon: 'bi-calculator' },
    { route: '/goals', label: 'Goals', icon: 'bi-bullseye' },
    { route: '/ai', label: 'AI Assistant', icon: 'bi-robot' },
    { route: '/challenges', label: 'Challenges', icon: 'bi-trophy' },
    { route: '/reports', label: 'Reports', icon: 'bi-bar-chart' },
    { route: '/profile', label: 'Profile', icon: 'bi-person' }
  ];

  async ngOnInit() {
    try {
      await this.authService.getUserProfile();
      this.ecoProfile = await this.gamificationService.getProfile();
    } catch (error) {
      console.error('Sidebar error:', error);
    }
  }

  public xpProgress(): number {
    const xp = this.ecoProfile?.totalXp ?? 0;
    const level = this.ecoProfile?.currentLevel ?? 1;
    const thresholds = SidebarComponent.LEVEL_XP;
    const floor = thresholds[Math.min(level, thresholds.length - 1)];
    const ceiling = thresholds[Math.min(level + 1, thresholds.length - 1)];
    if (ceiling <= floor) {
      return 100;
    }
    return Math.min(100, Math.round(((xp - floor) / (ceiling - floor)) * 100));
  }

  public xpToNextRank(): number {
    const xp = this.ecoProfile?.totalXp ?? 0;
    const level = this.ecoProfile?.currentLevel ?? 1;
    const thresholds = SidebarComponent.LEVEL_XP;
    const ceiling = thresholds[Math.min(level, thresholds.length - 1)];
    return Math.max(0, ceiling - xp);
  }

}