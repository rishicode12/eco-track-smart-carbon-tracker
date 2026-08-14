import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export interface AIInsightResponse {
  strengths: string[];
  weaknesses: string[];
  recommendations: string[];
  priorityActions: string[];
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  nextGoalSuggestion: string;
  predictedMonthlyCarbon: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private readonly apiService = inject(ApiService);

  getInsights(): Observable<ApiResponse<AIInsightResponse>> {
    return this.apiService.get<ApiResponse<AIInsightResponse>>('/api/ai/insights');
  }
}