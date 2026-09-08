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
  badgeName?: string;
  targetGoal?: number;
  metric?: string;
  isJoined?: boolean;
  currentProgress?: number;
  imageUrl?: string;
  image?: string;
  tags?: string[];
  joinedCount?: string;
  joined?: boolean;
}

export interface ActiveChallenge {
  id: number;
  title: string;
  targetGoal: number;
  currentProgress: number;
  isJoined: boolean;
  status: string;
  rewardPoints?: number;
  imageUrl?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChallengeService {

  private readonly api = inject(ApiService);
  private readonly basePath = '/challenges';

  async getAllChallenges(): Promise<ChallengeResponse[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<ChallengeResponse[]>>(this.basePath)
    );
    return response.data ?? [];
  }

  async getActiveChallenges(): Promise<ActiveChallenge[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<ActiveChallenge[]>>(`${this.basePath}/active`)
    );
    return response.data ?? [];
  }

  async joinChallenge(challengeId: number): Promise<ActiveChallenge> {
    const response = await firstValueFrom(
      this.api.post<ApiResponse<ActiveChallenge>>(`${this.basePath}/${challengeId}/join`, {})
    );
    return response.data;
  }

  async leaveChallenge(challengeId: number): Promise<void> {
    await firstValueFrom(
      this.api.delete<ApiResponse<void>>(`${this.basePath}/${challengeId}/leave`)
    );
  }

  async searchChallenges(query: string): Promise<ChallengeResponse[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<ChallengeResponse[]>>(`${this.basePath}/search?q=${query}`)
    );
    return response.data ?? [];
  }
}