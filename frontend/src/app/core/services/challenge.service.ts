import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';

export interface ChallengeResponse {
  id: number;
  title: string;
  description: string;
  category: string;
  rewardPoints: number;
  xpReward: number;
}

export interface ActiveChallenge {
  id: number;
  title: string;
  targetGoal: number;
  currentProgress: number;
  isJoined: boolean;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChallengeService {

  private readonly api = inject(ApiService);
  private readonly basePath = '/challenges';

  async getActiveChallenges(): Promise<ActiveChallenge[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<ActiveChallenge[]>>(this.basePath)
    );
    return response.data ?? [];
  }

  async joinChallenge(challengeId: number): Promise<ActiveChallenge> {
    const response = await firstValueFrom(
      this.api.post<ApiResponse<ActiveChallenge>>(`${this.basePath}/${challengeId}/join`, {})
    );
    return response.data;
  }

  async searchChallenges(query: string): Promise<ChallengeResponse[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<ChallengeResponse[]>>(`${this.basePath}/search?q=${query}`)
    );
    return response.data ?? [];
  }
}