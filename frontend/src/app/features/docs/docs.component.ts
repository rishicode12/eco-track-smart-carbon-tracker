import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

interface DocSection {
  id: string;
  title: string;
  icon: string;
}

@Component({
  selector: 'app-docs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './docs.component.html',
  styleUrls: ['./docs.component.scss']
})
export class DocsComponent {
  public activeTab: 'all' | 'logging' | 'gamification' | 'challenges' | 'api' = 'all';

  public docNavSections: DocSection[] = [
    { id: 'getting-started', title: 'Quick Start Overview', icon: 'bi-rocket-takeoff-fill' },
    { id: 'logging-activities', title: 'Logging Carbon Activities', icon: 'bi-plus-circle-dotted' },
    { id: 'gamification-system', title: 'Gamification & Badges', icon: 'bi-trophy-fill' },
    { id: 'community-challenges', title: 'Community Challenges', icon: 'bi-people-fill' },
    { id: 'emission-factors', title: 'Calculation Formulas & Factors', icon: 'bi-calculator-fill' }
  ];

  public badgeTiers = [
    {
      tier: 'Iron',
      xpRequirement: '0 - 499 XP',
      color: '#94a3b8',
      icon: 'bi-shield',
      description: 'Starting rank for beginners logging their baseline carbon footprints.'
    },
    {
      tier: 'Bronze',
      xpRequirement: '500 - 1,499 XP',
      color: '#d97706',
      icon: 'bi-shield-check',
      description: 'Awarded to consistent users logging daily commute and utility habits.'
    },
    {
      tier: 'Silver',
      xpRequirement: '1,500 - 3,499 XP',
      color: '#cbd5e1',
      icon: 'bi-award',
      description: 'Earned by reducing monthly carbon footprint by over 15% against baseline.'
    },
    {
      tier: 'Gold',
      xpRequirement: '3,500 - 6,999 XP',
      color: '#eab308',
      icon: 'bi-award-fill',
      description: 'Recognizes sustained sustainability habits, challenge wins, and goal milestones.'
    },
    {
      tier: 'Platinum',
      xpRequirement: '7,000 - 11,999 XP',
      color: '#2dd4bf',
      icon: 'bi-patch-check-fill',
      description: 'Elite eco-champions achieving high carbon offsets and community leadership.'
    },
    {
      tier: 'Diamond',
      xpRequirement: '12,000+ XP',
      color: '#38bdf8',
      icon: 'bi-gem',
      description: 'Legendary sustainability masters leading top global leaderboard positions.'
    }
  ];

  public scrollToSection(sectionId: string): void {
    const el = document.getElementById(sectionId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
