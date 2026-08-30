import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';

export interface AdminUser {
  id: number;
  fullName: string;
  email: string;
  role: string;
  createdAt: string | null;
}

export interface AdminChallenge {
  id: number;
  title: string;
  description: string;
  challengeType: string;
  rewardPoints: number;
  badgeName: string;
  category: string | null;
  targetGoal: number | null;
  metric: string | null;
  status: string | null;
  active: boolean | null;
}

export interface AdminChallengeCreate {
  title: string;
  description: string;
  challengeType: 'DAILY' | 'WEEKLY';
  rewardPoints: number;
  badgeName: string;
  category?: string;
  targetGoal?: number;
  metric?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminService {

  private readonly api = inject(ApiService);
  private readonly basePath = '/api/admin';

  async getUsers(): Promise<AdminUser[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<AdminUser[]>>(`${this.basePath}/users`)
    );
    return response.data ?? [];
  }

  async getChallenges(): Promise<AdminChallenge[]> {
    const response = await firstValueFrom(
      this.api.get<ApiResponse<AdminChallenge[]>>(`${this.basePath}/challenges`)
    );
    return response.data ?? [];
  }

  async createChallenge(payload: AdminChallengeCreate): Promise<ApiResponse<unknown>> {
    return firstValueFrom(
      this.api.post<ApiResponse<unknown>>(`${this.basePath}/challenges`, payload)
    );
  }

  async deleteChallenge(id: number): Promise<ApiResponse<unknown>> {
    return firstValueFrom(
      this.api.delete<ApiResponse<unknown>>(`${this.basePath}/challenges/${id}`)
    );
  }
}