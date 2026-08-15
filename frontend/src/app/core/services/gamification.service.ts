import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';

export interface EcoProfileResponse {
  totalXp: number;
  currentLevel: number;
  unlockedBadges: string[];
}

export interface EcoLeaderboardResponse {
  username: string;
  totalXp: number;
  currentLevel: number;
  rank: number;
}

interface EcoLeaderboardRawResponse {
  rank: number;
  userId: number;
  fullName: string;
  totalXp: number;
  currentLevel: number;
}

@Injectable({
  providedIn: 'root',
})
export class GamificationService {

  private readonly api = inject(ApiService);
  private readonly basePath = '/api/gamification';

  async getProfile(): Promise<EcoProfileResponse> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<EcoProfileResponse>>(
        `${this.basePath}/profile`
      )
    );
    return response.data;
  }

  async getLeaderboard(): Promise<EcoLeaderboardResponse[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<EcoLeaderboardRawResponse[]>>(
        `${this.basePath}/leaderboard`
      )
    );
    return (response.data ?? []).map((entry) => ({
      rank: entry.rank,
      username: entry.fullName,
      totalXp: entry.totalXp,
      currentLevel: entry.currentLevel,
    }));
  }
}