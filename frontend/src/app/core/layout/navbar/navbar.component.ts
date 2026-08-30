import { Component, OnInit, inject, HostListener, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../features/auth/auth.service';
import { LayoutService } from '../../services/layout.service';
import { GamificationService, EcoProfileResponse } from '../../services/gamification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {

  // Level thresholds: index = level - 1, value = XP needed to reach that level.
  // L1: base 0    -> target 1000  (span 1000)
  // L2: base 1000 -> target 2500  (span 1500)
  // L3: base 2500 -> target 5000  (span 2500)
  // L4: base 5000 -> target 10000 (span 5000)
  // L5+: max rank, progress capped at 100%.
  private static readonly LEVEL_XP = [0, 1000, 2500, 5000, 10000];

  public layoutService = inject(LayoutService);
  public authService = inject(AuthService);
  public gamificationService = inject(GamificationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  public unreadNotificationsCount = 1; // Dummy notification count

  public isDropdownOpen = false;
  public ecoProfile: EcoProfileResponse | null = null;
  public currentUser: any = null;

  public level = 1;
  public xpProgressPercent = 0;
  public xpToNextRank = 1000;

  async ngOnInit() {
    try {
      this.currentUser = await this.authService.getUserProfile();
    } catch (error) {
      console.error('Navbar error:', error);
    }

    this.gamificationService.profile$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((profile) => {
        this.ecoProfile = profile;
        this.applyLevelMath(profile);
        this.cdr.detectChanges();
      });

    this.gamificationService.refreshProfile();
  }

  @HostListener('document:click')
  public onDocumentClick(): void {
    this.isDropdownOpen = false;
  }

  public toggleDropdown(event: Event): void {
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  public logout(): void {
    this.isDropdownOpen = false;
    this.authService.logout();
  }

  private applyLevelMath(profile: EcoProfileResponse | null): void {
    const totalXp = profile?.totalXp ?? 0;
    const level = profile?.currentLevel ?? 1;

    this.level = level;

    if (totalXp <= 0) {
      this.xpProgressPercent = 0;
      this.xpToNextRank = NavbarComponent.LEVEL_XP[1];
      return;
    }

    // Max rank reached: bar full, nothing left to unlock.
    if (level >= NavbarComponent.LEVEL_XP.length) {
      this.xpProgressPercent = 100;
      this.xpToNextRank = 0;
      return;
    }

    const currentLevelBaseXp = NavbarComponent.LEVEL_XP[level - 1];
    const nextLevelTargetXp = NavbarComponent.LEVEL_XP[level];
    const levelSpanXp = nextLevelTargetXp - currentLevelBaseXp;

    const rawPercent = ((totalXp - currentLevelBaseXp) / levelSpanXp) * 100;
    this.xpProgressPercent = Math.round(Math.min(100, Math.max(0, rawPercent)));
    this.xpToNextRank = Math.max(0, nextLevelTargetXp - totalXp);
  }
}