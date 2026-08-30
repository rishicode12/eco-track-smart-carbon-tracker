import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';

export interface ActiveChallenge {
  id: number;
  title: string;
  targetGoal: number;
  currentProgress: number;
  isJoined: boolean;
  status: string;
}

@Injectable({
  providedIn: 'root',
})
export class ChallengeService {

  private readonly api = inject(ApiService);
  private readonly basePath = '/api/challenges';

  async getActiveChallenges(): Promise<ActiveChallenge[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<ActiveChallenge[]>>(this.basePath)
    );
    return response.data ?? [];
  }
}