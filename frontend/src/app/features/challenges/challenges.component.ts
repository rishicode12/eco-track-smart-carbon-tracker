import { Component, AfterViewInit, ChangeDetectorRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { GamificationService, EcoLeaderboardResponse, EcoProfileResponse } from '../../core/services/gamification.service';

gsap.registerPlugin(ScrollTrigger);

interface Challenge {
  id: number;
  title: string;
  desc: string;
  xp: number;
  tags: string[];
  joinedCount: string;
  joined: boolean;
  image: string;
}

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
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  public ecoProfile: EcoProfileResponse | null = null;
  public leaderboard: EcoLeaderboardResponse[] = [];
  public searchQuery: string = '';

  public allBadges: BadgePreset[] = [
    { code: 'GREEN_HERO', name: 'Green Hero', icon: 'bi-gem', condition: 'Complete your first challenge' },
    { code: 'ENERGY_SAVER', name: 'Energy Saver', icon: 'bi-lightning-charge', condition: 'Save 50 kWh of electricity' },
    { code: 'CARBON_CRUSHER', name: 'Carbon Crusher', icon: 'bi-arrow-down-circle', condition: 'Cut your carbon footprint by 20%' },
    { code: 'ECO_WARRIOR', name: 'Eco Warrior', icon: 'bi-bicycle', condition: 'Log 5+ zero-emission transport activities' },
    { code: 'TREE_MASTER', name: 'Tree Master', icon: 'bi-tree', condition: 'Plant 5 trees' },
    { code: 'ZERO_WASTE', name: 'Zero Waste', icon: 'bi-recycle', condition: 'Go a full week without waste' }
  ];

  public recommendedChallenges: Challenge[] = [
    {
      id: 1,
      title: 'Energy Saving Challenge',
      desc: 'Reduce your home electricity consumption by 20% over 30 days.',
      xp: 500,
      tags: ['ENERGY', 'GLOBAL'],
      joinedCount: '1.2k joined',
      joined: false,
      image: 'https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?q=80&w=250&auto=format&fit=crop'
    },
    {
      id: 2,
      title: 'Cycle to Work',
      desc: 'Swap your car for a bike for at least 3 days a week. Track your miles.',
      xp: 350,
      tags: ['TRANSPORT', 'LOCAL'],
      joinedCount: '840 joined',
      joined: false,
      image: 'https://images.unsplash.com/photo-1541614101331-1a5a3a194e92?q=80&w=250&auto=format&fit=crop'
    },
    {
      id: 3,
      title: 'Tree Plantation Drive',
      desc: 'Collaborative goal: Plant 5,000 trees this month. Every tree counts.',
      xp: 1200,
      tags: ['NATURE', 'TEAM'],
      joinedCount: '3.5k joined',
      joined: true,
      image: 'https://images.unsplash.com/photo-1530595467537-0b5996c41f2d?q=80&w=250&auto=format&fit=crop'
    }
  ];

  public ngOnInit(): void {
    this.loadData();
  }

  public ngAfterViewInit(): void {
    this.animateEntrance();
    this.animateProgressBar();
    this.animateBadgesOnScroll();
  }

  public async loadData(): Promise<void> {
    try {
      const [profile, leaderboard] = await Promise.all([
        this.gamificationService.getProfile(),
        this.gamificationService.getLeaderboard()
      ]);
      this.ecoProfile = profile;
      this.leaderboard = leaderboard;
    } catch (error) {
      console.error('Failed to load gamification data', error);
    } finally {
      this.cdr.detectChanges();
    }
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
    const fill = this.progressFillRef.nativeElement;
    const target = Number(fill.dataset['progress'] ?? 0);
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

  public get filteredChallenges(): Challenge[] {
    const query = this.searchQuery.trim().toLowerCase();
    if (!query) {
      return this.recommendedChallenges;
    }
    return this.recommendedChallenges.filter(
      (ch) =>
        ch.title.toLowerCase().includes(query) ||
        ch.tags.some((tag) => tag.toLowerCase().includes(query))
    );
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

  public navigateToCalculator(): void {
    this.router.navigate(['/carbon']);
  }

  public onFabClick(): void {
    this.router.navigate(['/carbon']);
  }

  public onJoinChallenge(id: number) {
    const ch = this.recommendedChallenges.find(c => c.id === id);
    if (ch) {
      ch.joined = !ch.joined;
      if (ch.joined) {
        ch.joinedCount = (parseFloat(ch.joinedCount) + 0.1).toFixed(1) + 'k joined';
      } else {
        ch.joinedCount = (parseFloat(ch.joinedCount) - 0.1).toFixed(1) + 'k joined';
      }
    }
  }
}