import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse } from '../models/api-response.model';

export interface GoalRequest {
  title: string;
  targetCarbonReduction: number;
  deadline: string;
}

export interface GoalResponse {
  id: number;
  title: string;
  targetCarbonReduction: number;
  currentProgress: number;
  deadline: string;
  isCompleted: boolean;
  createdAt: string;
  progressPercent: number;
}

@Injectable({
  providedIn: 'root',
})
export class GoalService {

  private readonly api = inject(ApiService);

  private readonly basePath = '/api/goals';

  private readonly goalsSubject =
    new BehaviorSubject<GoalResponse[]>([]);

  readonly goals$ =
    this.goalsSubject.asObservable();

  get snapshot(): GoalResponse[] {
    return this.goalsSubject.value;
  }

  get activeGoals(): GoalResponse[] {
    return this.snapshot.filter(
      (goal) => !goal.isCompleted
    );
  }

  get aggregateProgressPercent(): number {

    const active = this.activeGoals;

    if (active.length === 0) {
      return 0;
    }

    const total = active.reduce(
      (sum, goal) =>
        sum + (goal.progressPercent ?? 0),
      0
    );

    return Math.round(
      (total / active.length) * 10
    ) / 10;
  }

  async refreshGoals(): Promise<GoalResponse[]> {

    const response =
      await firstValueFrom(
        this.api.get<ApiResponse<GoalResponse[]>>(
          this.basePath
        )
      );

    const goals =
      response.data ?? [];

    this.goalsSubject.next(goals);

    return goals;
  }

  async createGoal(
    request: GoalRequest
  ): Promise<GoalResponse> {

    const response =
      await firstValueFrom(
        this.api.post<ApiResponse<GoalResponse>>(
          this.basePath,
          request
        )
      );

    if (
      !response.success ||
      !response.data
    ) {
      throw new Error(
        response.message ||
        'Failed to create goal'
      );
    }

    await this.refreshGoals();

    this.dispatchGoalUpdatedEvent();

    return response.data;
  }

  async updateGoal(
    id: number,
    request: GoalRequest
  ): Promise<GoalResponse> {

    const response =
      await firstValueFrom(
        this.api.put<ApiResponse<GoalResponse>>(
          `${this.basePath}/${id}`,
          request
        )
      );

    if (
      !response.success ||
      !response.data
    ) {
      throw new Error(
        response.message ||
        'Failed to update goal'
      );
    }

    await this.refreshGoals();

    this.dispatchGoalUpdatedEvent();

    return response.data;
  }

  async completeGoal(
    id: number
  ): Promise<GoalResponse> {

    const response =
      await firstValueFrom(
        this.api.patch<ApiResponse<GoalResponse>>(
          `${this.basePath}/${id}/complete`,
          {}
        )
      );

    if (
      !response.success ||
      !response.data
    ) {
      throw new Error(
        response.message ||
        'Failed to complete goal'
      );
    }

    await this.refreshGoals();

    this.dispatchGoalUpdatedEvent();

    return response.data;
  }

  async deleteGoal(
    id: number
  ): Promise<void> {

    const response =
      await firstValueFrom(
        this.api.delete<ApiResponse<void>>(
          `${this.basePath}/${id}`
        )
      );

    if (!response.success) {
      throw new Error(
        response.message ||
        'Failed to delete goal'
      );
    }

    await this.refreshGoals();

    this.dispatchGoalUpdatedEvent();
  }

  private dispatchGoalUpdatedEvent(): void {

    if (typeof window !== 'undefined') {
      window.dispatchEvent(
        new CustomEvent('goal-updated')
      );
    }
  }
}