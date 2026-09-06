import { Component, DestroyRef, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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

  public layoutService = inject(LayoutService);
  public authService = inject(AuthService);
  public gamificationService = inject(GamificationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  public ecoProfile: EcoProfileResponse | null = null;

  public rankTitle = 'LEVEL 1 EXPLORER';
  public xpProgressPercent = 0;
  public xpToNextRank = 1000;

  public navItems = [
    { route: '/dashboard', label: 'Dashboard', icon: 'bi-grid' },
    { route: '/carbon', label: 'Carbon Tracker', icon: 'bi-calculator' },
    { route: '/goals', label: 'Goals', icon: 'bi-bullseye' },
    { route: '/ai', label: 'AI Assistant', icon: 'bi-robot' },
    { route: '/challenges', label: 'Challenges', icon: 'bi-trophy' },
    { route: '/reports', label: 'Reports', icon: 'bi-bar-chart' },
    { route: '/profile', label: 'Profile', icon: 'bi-person' }
  ];

  public ngOnInit() {
    this.authService.getUserProfile().catch((error) => {
      console.error('Sidebar user profile error:', error);
    });

    this.gamificationService.profile$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((profile) => {
        this.ecoProfile = profile;
        this.applyLevelMath(profile);
        this.cdr.detectChanges();
      });

    this.gamificationService.profileUpdated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.gamificationService.refreshProfile());

    this.gamificationService.refreshProfile();
  }

  private applyLevelMath(profile: EcoProfileResponse | null): void {
    const level = profile?.currentLevel ?? 1;
    const levelName = profile?.levelName ?? 'Explorer';

    this.rankTitle = `LEVEL ${level} ${levelName.toUpperCase()}`;
    this.xpProgressPercent = profile?.progressPercentage ?? 0;
    this.xpToNextRank = profile?.xpToNextLevel ?? 0;
  }

}