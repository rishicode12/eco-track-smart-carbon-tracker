import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReportSummary {
  todayCarbon: number;
  weeklyCarbon: number;
  monthlyCarbon: number;
  yearlyCarbon: number;
  avgDailyEmission: number;
  momReductionPercent: number;
}

export interface CategoryBreakdown {
  category: string;
  co2Impact: number;
  percentage: number;
}

export interface MonthlyTrend {
  month: string;
  co2Impact: number;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('ecotrack_token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  private get<T>(path: string): Promise<T> {
    return firstValueFrom(
      this.http.get<ApiResponse<T>>(`${this.baseUrl}${path}`, {
        headers: this.getHeaders(),
      })
    ).then((res) => res.data);
  }

  async getSummary(): Promise<ReportSummary> {
    return this.get<ReportSummary>('/api/reports/summary');
  }

  async getCategoryBreakdown(): Promise<CategoryBreakdown[]> {
    return this.get<CategoryBreakdown[]>('/api/reports/categories');
  }

  async getMonthlyTrends(): Promise<MonthlyTrend[]> {
    return this.get<MonthlyTrend[]>('/api/reports/trends');
  }
}
