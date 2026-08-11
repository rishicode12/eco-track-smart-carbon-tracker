import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';


import { ApiService }from './api.service';


export interface CarbonLogRequest {
  activityCategory: string;
  co2Impact: number;
  description?: string;
  loggedAt?: string;
}

export interface CarbonLogResponse {
  id: number;
  activityCategory: string;
  co2Impact: number;
  description: string;
  loggedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CarbonService {
  // Yahan humne HTTPClient ki jagah ApiService inject kiya hai
  private api = inject(ApiService);
  private basePath = 'api/carbon'; 

  async getLogs(): Promise<CarbonLogResponse[]> {
    // Ab manual headers or token lagane ki zaroorat nahi hai, interceptor khud lagayega
    const response: any = await firstValueFrom(this.api.get(this.basePath));
    return response.data;
  }

  async addLog(log: CarbonLogRequest): Promise<CarbonLogResponse> {
    const response: any = await firstValueFrom(this.api.post(this.basePath, log));
    return response.data;
  }

  async updateLog(id: number, log: CarbonLogRequest): Promise<CarbonLogResponse> {
    const response: any = await firstValueFrom(this.api.put(`${this.basePath}/${id}`, log));
    return response.data;
  }

  async deleteLog(id: number): Promise<void> {
    await firstValueFrom(this.api.delete(`${this.basePath}/${id}`));
  }
}