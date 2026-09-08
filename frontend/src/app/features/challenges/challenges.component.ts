import { Component, AfterViewInit, ChangeDetectorRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { GamificationService, EcoLeaderboardResponse, EcoProfileResponse } from '../../core/services/gamification.service';
import { ChallengeService, ActiveChallenge, ChallengeResponse } from '../../core/services/challenge.service';

gsap.registerPlugin(ScrollTrigger);

interface BadgePreset {
  code: string;
  name: string;
  icon: string;
  condition: string;
}

@Component({
  selector: 'app-challenges',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './challenges.component.html',
  styleUrls: ['./challenges.component.css']
})
export class ChallengesComponent implements OnInit, AfterViewInit {
  @ViewChild('progressFill', { static: false }) private progressFillRef!: ElementRef<HTMLElement>;

  private readonly gamificationService = inject(GamificationService);
  private readonly challengeService = inject(ChallengeService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  public ecoProfile: EcoProfileResponse | null = null;
  public leaderboard: EcoLeaderboardResponse[] = [];
  public searchQuery: string = '';
  public activeChallenges: ActiveChallenge[] = [];
  public recommendedChallenges: ChallengeResponse[] = [];
  public activeProgressPercent = 0;
  public isBadgeModalOpen = false;
  public errorMessage: string | null = null;

  public allBadges: BadgePreset[] = [
    { code: 'GREEN_HERO', name: 'Green Hero', icon: 'bi-gem', condition: 'Complete your first challenge' },
    { code: 'ENERGY_SAVER', name: 'Energy Saver', icon: 'bi-lightning-charge', condition: 'Save 50 kWh of electricity' },
    { code: 'CARBON_CRUSHER', name: 'Carbon Crusher', icon: 'bi-arrow-down-circle', condition: 'Cut your carbon footprint by 20%' },
    { code: 'ECO_WARRIOR', name: 'Eco Warrior', icon: 'bi-bicycle', condition: 'Log 5+ zero-emission transport activities' },
    { code: 'TREE_MASTER', name: 'Tree Master', icon: 'bi-tree', condition: 'Plant 5 trees' },
    { code: 'ZERO_WASTE', name: 'Zero Waste', icon: 'bi-recycle', condition: 'Go a full week without waste' }
  ];

  public badgeTiers = [
    { level: 1, name: 'Iron', color: '#64748B', glow: 'rgba(100, 116, 139, 0.4)' },
    { level: 2, name: 'Bronze', color: '#CD7F32', glow: 'rgba(205, 127, 50, 0.5)' },
    { level: 3, name: 'Silver', color: '#C0C0C0', glow: 'rgba(192, 192, 192, 0.5)' },
    { level: 4, name: 'Gold', color: '#FFD700', glow: 'rgba(255, 215, 0, 0.6)' },
    { level: 5, name: 'Platinum', color: '#00FFFF', glow: 'rgba(0, 255, 255, 0.7)' }
  ];

  public badgeUnlockCriteria = [
    { badge: 'ZERO_WASTE', name: 'Zero Waste', icon: 'bi-recycle', requirement: 'Log 10 Waste activities', xp: 300 },
    { badge: 'ENERGY_SAVER', name: 'Energy Saver', icon: 'bi-lightning-charge', requirement: 'Reduce electricity by 20% in goals', xp: 250 },
    { badge: 'CARBON_CRUSHER', name: 'Carbon Crusher', icon: 'bi-arrow-down-circle', requirement: 'Accumulate 1,000 total XP', xp: 500 },
    { badge: 'TREE_MASTER', name: 'Tree Master', icon: 'bi-tree', requirement: 'Join & complete a tree plantation challenge', xp: 400 },
    { badge: 'ECO_WARRIOR', name: 'Eco Warrior', icon: 'bi-bicycle', requirement: 'Log 5+ zero-emission transport activities', xp: 200 },
    { badge: 'GREEN_HERO', name: 'Green Hero', icon: 'bi-gem', requirement: 'Complete your first challenge', xp: 100 }
  ];

  public ngOnInit(): void {
    this.loadData();

    this.gamificationService.profile$.subscribe((profile) => {
      if (!profile) {
        return;
      }
      this.ecoProfile = profile;
      this.cdr.detectChanges();
    });
  }

  public ngAfterViewInit(): void {
    this.animateEntrance();
    this.animateProgressBar();
    this.animateBadgesOnScroll();
  }

  public async loadData(): Promise<void> {
    try {
      const [profile, leaderboard, active, allChallenges] = await Promise.all([
        this.gamificationService.getProfile(),
        this.gamificationService.getLeaderboard(),
        this.challengeService.getActiveChallenges(),
        this.challengeService.getAllChallenges()
      ]);
      this.ecoProfile = profile;
      this.leaderboard = leaderboard;
      this.activeChallenges = active || [];
      this.recommendedChallenges = allChallenges || [];
      this.computeActiveProgressPercent();
    } catch (error) {
      console.error('Failed to load challenges data', error);
    } finally {
      this.cdr.detectChanges();
      this.animateProgressBar();
    }
  }

  private computeActiveProgressPercent(): void {
    const joinedChallenge = this.activeChallenges.find((c) => c.isJoined);
    const fallback = this.activeChallenges.find((c) => (c.currentProgress ?? 0) > 0 || (c.targetGoal ?? 0) > 0);
    const active = joinedChallenge ?? fallback;

    if (!active) {
      this.activeProgressPercent = 0;
      return;
    }

    this.activeProgressPercent = Math.round(Math.min(100, Math.max(0, active.currentProgress ?? 0)));
  }

  private animateEntrance(): void {
    gsap.from('.stagger-item', {
      y: 30,
      opacity: 0,
      duration: 0.8,
      ease: 'power3.out',
      stagger: 0.15,
      delay: 0.1
    });
  }

  private animateProgressBar(): void {
    const fill = this.progressFillRef?.nativeElement;
    if (!fill) {
      return;
    }
    const target = Math.min(100, Math.max(0, this.activeProgressPercent));
    gsap.fromTo(
      fill,
      { width: '0%' },
      { width: `${target}%`, duration: 1.4, ease: 'back.out(1.7)', delay: 0.4 }
    );
  }

  private animateBadgesOnScroll(): void {
    const badgesContainer = document.querySelector('.badge-grid');
    if (!badgesContainer) {
      return;
    }
    gsap.from(badgesContainer, {
      scale: 0.6,
      opacity: 0,
      duration: 0.7,
      ease: 'back.out(2)',
      scrollTrigger: {
        trigger: badgesContainer,
        start: 'top 90%',
        toggleActions: 'play none none reverse'
      }
    });
  }

  public get filteredChallenges(): ChallengeResponse[] {
    const activeIds = new Set(this.activeChallenges.map((ac) => ac.id));
    // Filter: If a challenge is already inside activeChallenges, do NOT show it in recommendedChallenges list
    const unjoined = this.recommendedChallenges.filter((ch) => !activeIds.has(ch.id) && !ch.isJoined);
    const query = this.searchQuery.trim().toLowerCase();
    if (!query) {
      return unjoined;
    }
    return unjoined.filter(
      (ch) =>
        ch.title.toLowerCase().includes(query) ||
        (ch.category && ch.category.toLowerCase().includes(query)) ||
        (ch.description && ch.description.toLowerCase().includes(query))
    );
  }

  public get isMaxActiveLimitReached(): boolean {
    return this.activeChallenges.length >= 2;
  }

  public getRankIcon(rank: number): string {
    if (rank === 1) {
      return 'bi-1-square-fill';
    }
    if (rank === 2) {
      return 'bi-2-square-fill';
    }
    return 'bi-3-square-fill';
  }

  public isBadgeUnlocked(badgeCode: string): boolean {
    return this.ecoProfile?.unlockedBadges?.includes(badgeCode) ?? false;
  }

  public getBadgeTierClass(level: number): string {
    if (level <= 1) {
      return 'badge-iron';
    }
    if (level === 2) {
      return 'badge-bronze';
    }
    if (level === 3) {
      return 'badge-silver';
    }
    if (level === 4) {
      return 'badge-gold';
    }
    return 'badge-platinum';
  }

  public navigateToCalculator(): void {
    this.router.navigate(['/carbon']);
  }

  public onFabClick(): void {
    this.router.navigate(['/carbon']);
  }

  public async onJoinChallenge(id: number): Promise<void> {
    if (this.isMaxActiveLimitReached) {
      alert('A user can only have a maximum of 2 ACTIVE challenges at the same time.');
      return;
    }

    this.errorMessage = null;

    try {
      const activeChallenge = await this.challengeService.joinChallenge(id);
      const ch = this.recommendedChallenges.find(c => c.id === id);
      if (ch) {
        ch.isJoined = true;
      }

      const newActive: ActiveChallenge = activeChallenge ?? {
        id: id,
        title: ch ? ch.title : 'Challenge',
        targetGoal: ch?.targetGoal ?? 100,
        currentProgress: 0,
        isJoined: true,
        status: 'IN_PROGRESS',
        rewardPoints: ch?.rewardPoints,
        imageUrl: ch?.imageUrl
      };

      const existingIndex = this.activeChallenges.findIndex(ac => ac.id === id);
      if (existingIndex >= 0) {
        this.activeChallenges[existingIndex] = newActive;
      } else {
        this.activeChallenges.push(newActive);
      }
      this.computeActiveProgressPercent();
      this.cdr.detectChanges();
    } catch (err: any) {
      const msg = err?.error?.message || err?.message || 'A user can only have a maximum of 2 ACTIVE challenges at the same time.';
      this.errorMessage = msg;
      alert(msg);
    }
  }

  public async onLeaveChallenge(id: number): Promise<void> {
    try {
      await this.challengeService.leaveChallenge(id);
      this.activeChallenges = this.activeChallenges.filter(ac => ac.id !== id);
      const ch = this.recommendedChallenges.find(c => c.id === id);
      if (ch) {
        ch.isJoined = false;
      }
      this.computeActiveProgressPercent();
      this.cdr.detectChanges();
    } catch (err: any) {
      console.error('Failed to leave challenge', err);
      this.activeChallenges = this.activeChallenges.filter(ac => ac.id !== id);
      this.computeActiveProgressPercent();
      this.cdr.detectChanges();
    }
  }
}