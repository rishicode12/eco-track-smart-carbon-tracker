import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { environment } from '../../../environments/environment';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface LoginResponse {
  token: string;
  message: string;
  email: string;
}

interface UserSession {
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private router = inject(Router);
  private apiService = inject(ApiService);
  private http = inject(HttpClient);

  private readonly tokenKey = 'ecotrack_token';
  private readonly userKey = 'ecotrack_user';

  public isAuthenticated = signal(false);
  public currentUser = signal<UserSession | null>(null);
  public userProfile = signal<any>(null);

  constructor() {
    this.checkSession();
  }

  private checkSession() {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem(this.tokenKey);
      const user = localStorage.getItem(this.userKey);

      if (token && user) {
        this.isAuthenticated.set(true);
        this.currentUser.set(JSON.parse(user));
      }
    }
  }

  // ==========================================
  // GET USER PROFILE
  // ==========================================
  public async getUserProfile(): Promise<any> {
    const token = this.getToken();

    if (!token) {
      throw new Error("No token found");
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    try {
      const response: any = await firstValueFrom(
        this.http.get(`${environment.apiUrl}/api/users/profile`, { headers })
      );

      const data = response.data || response;
      this.userProfile.set(data);
      return data;
    } catch (error) {
      console.error("Error fetching profile from backend", error);
      throw error;
    }
  }

  public async login(email: string, password: string): Promise<LoginResponse> {
    const response = await firstValueFrom(
      this.apiService.post<ApiResponse<LoginResponse>>('/api/users/login', {
        email,
        password,
      })
    );

    return this.processAuthResponse(response, 'Login failed.');
  }

  public async register(fullName: string, email: string, password: string, country?: string): Promise<LoginResponse> {
    const response = await firstValueFrom(
      this.apiService.post<ApiResponse<LoginResponse>>('/api/users/register', {
        fullName,
        email,
        password,
        country,
      })
    );

    return this.processAuthResponse(response, 'Registration failed.');
  }

  // ==========================================
  // TOKEN
  // ==========================================
  public getApiUrl(): string {
    return environment.apiUrl;
  }

  public getToken(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }

    return localStorage.getItem(this.tokenKey);
  }

  /**
   * True when the current user holds an administrator role.
   * Accepts both "ADMIN" and the Spring convention "ROLE_ADMIN".
   */
  public isAdmin(): boolean {
    const role = this.userProfile()?.role;
    return role === 'ADMIN' || role === 'ROLE_ADMIN';
  }

  // ==========================================
  // FORGOT PASSWORD
  // ==========================================
  public async forgotPassword(email: string): Promise<void> {
    await firstValueFrom(
      this.apiService.post('/api/auth/forgot-password', { email })
    );
  }

  // ==========================================
  // OAUTH2 TOKEN CAPTURE (from backend redirect)
  // ==========================================
  public handleOAuthCallback(token: string): void {
    const payload = this.decodeJwtPayload(token);
    if (payload?.email) {
      this.setSession(token, payload.email);
      this.router.navigate(['/dashboard']);
    }
  }

  private decodeJwtPayload(token: string): { email: string } | null {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(base64));
      return { email: decoded.sub };
    } catch {
      return null;
    }
  }

  public logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  public async deleteMyAccount(): Promise<void> {
    const token = this.getToken();
    if (!token) {
      throw new Error('No token found');
    }
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    await firstValueFrom(
      this.http.delete<ApiResponse<void>>(`${environment.apiUrl}/api/users/me`, { headers })
    );
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
    this.userProfile.set(null);
    this.router.navigate(['/auth/login']);
  }

  private setSession(token: string, email: string): void {
    const session: UserSession = { email };

    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(session));
    this.isAuthenticated.set(true);
    this.currentUser.set(session);
  }

  private processAuthResponse(response: ApiResponse<LoginResponse>, fallbackMessage: string): LoginResponse {
    const authData = response.data;

    if (!response.success || !authData?.token) {
      throw new Error(response.message || fallbackMessage);
    }

    this.setSession(authData.token, authData.email);
    return authData;
  }
}
