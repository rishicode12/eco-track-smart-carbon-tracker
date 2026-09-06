import { Component, OnInit, inject, HostListener, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { AuthService } from '../../../features/auth/auth.service';
import { LayoutService } from '../../services/layout.service';
import { GamificationService, EcoProfileResponse } from '../../services/gamification.service';
import { ChallengeService } from '../../services/challenge.service';
import { NotificationService } from '../../services/notification.service';
import { NotificationItem } from '../../services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {

  public layoutService = inject(LayoutService);
  public authService = inject(AuthService);
  public gamificationService = inject(GamificationService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  public challengeService = inject(ChallengeService);
  public notificationService = inject(NotificationService);

  public unreadNotificationsCount = 1;

  public isDropdownOpen = false;
  public ecoProfile: EcoProfileResponse | null = null;
  public currentUser: any = null;

  public level = 1;
  public xpProgressPercent = 0;
  public xpToNextRank = 1000;

  public searchControl = new FormControl('');
  public searchResults: any[] = [];
  public isSearchDropdownOpen = false;

  public notifications: NotificationItem[] = [];
  public unreadCount = 0;
  public isNotificationsOpen = false;

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

    this.gamificationService.profileUpdated$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.gamificationService.refreshProfile());

    this.gamificationService.refreshProfile();

    this.loadNotifications();

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe((query: string | null) => {
      if (!query || query.trim().length < 2) {
        this.searchResults = [];
        this.isSearchDropdownOpen = false;
        this.cdr.detectChanges();
        return;
      }
      this.searchChallenges(query.trim());
    });
  }

  private async loadNotifications(): Promise<void> {
    const result = await this.notificationService.getNotifications();
    this.notifications = result.notifications;
    this.unreadCount = result.unreadCount;
    this.cdr.detectChanges();
  }

  public toggleNotifications(): void {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    this.cdr.detectChanges();
  }

  public markAsRead(item: NotificationItem): void {
    this.notificationService.markAsRead(item.id).then(() => {
      this.notifications = this.notifications.filter(n => n.id !== item.id);
      this.unreadCount = Math.max(this.unreadCount - 1, 0);
      this.cdr.detectChanges();
    });
  }

  public markAllAsRead(): void {
    this.notificationService.markAllAsRead().then(() => {
      this.notifications = [];
      this.unreadCount = 0;
      this.isNotificationsOpen = false;
      this.cdr.detectChanges();
    });
  }

  @HostListener('document:click', ['$event'])
  public onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-wrapper') && !target.closest('.search-box-wrapper')) {
      this.isDropdownOpen = false;
      this.isNotificationsOpen = false;
      this.cdr.detectChanges();
    }
  }

  public toggleDropdown(event: Event): void {
    event.stopPropagation();
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  public logout(): void {
    this.isDropdownOpen = false;
    this.isNotificationsOpen = false;
    this.authService.logout();
  }

  public async searchChallenges(query: string): Promise<void> {
    try {
      const results = await this.challengeService.searchChallenges(query);
      this.searchResults = results;
      this.isSearchDropdownOpen = true;
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Error searching challenges:', error);
      this.searchResults = [];
      this.isSearchDropdownOpen = false;
    }
  }

  public getCategoryClass(category: string): string {
    const classMap: Record<string, string> = {
      'WASTE_REDUCTION': 'badge-success',
      'ENERGY_CONSERVATION': 'badge-warning',
      'SUSTAINABLE_TRANSPORT': 'badge-info',
      'WATER_CONSERVATION': 'badge-primary',
      'GENERAL': 'badge-secondary'
    };
    return classMap[category] || 'badge-secondary';
  }

  public getNotificationIcon(type: string): string {
    const iconMap: Record<string, string> = {
      'LEVEL_UP': 'bi bi-trophy',
      'BADGE_UNLOCK': 'bi bi-award',
      'CHALLENGE_COMPLETED': 'bi bi-check-circle',
      'ACTIVITY_LOGGED': 'bi bi-check2-square'
    };
    return iconMap[type] || 'bi bi-bell';
  }

  private applyLevelMath(profile: EcoProfileResponse | null): void {
    this.level = profile?.currentLevel ?? 1;
    this.xpProgressPercent = profile?.progressPercentage ?? 0;
    this.xpToNextRank = profile?.xpToNextLevel ?? 0;
  }
}
