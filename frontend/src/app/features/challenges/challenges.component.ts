import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

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

interface Leader {
  rank: number;
  name: string;
  points: string;
}

@Component({
  selector: 'app-challenges',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './challenges.component.html',
  styleUrls: ['./challenges.component.css']
})
export class ChallengesComponent {
  public leaderboard: Leader[] = [
    { rank: 1, name: 'Sarah J.', points: '2.4k' },
    { rank: 2, name: 'Alex R.', points: '2.1k' },
    { rank: 3, name: 'Mike T.', points: '1.9k' }
  ];

  public badges = [
    { name: 'Green Hero', icon: 'bi-gem', desc: 'Completed first challenge' },
    { name: 'Energy Saver', icon: 'bi-lightning-charge', desc: 'Saved 50kWh electricity' },
    { name: 'H2O Master', icon: 'bi-droplet', desc: 'Saved 500L water' },
    { name: 'Planter', icon: 'bi-tree', desc: 'Planted 5 trees' }
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
