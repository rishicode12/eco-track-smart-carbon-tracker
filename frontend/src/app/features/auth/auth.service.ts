import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http'; // Naya Import
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

interface GoogleLoginResponse {
  token: string;
  message: string;
  email: string;
  fullName: string;
  provider: string;
  providerId: string;
  profilePicture: string | null;
  emailVerified: boolean;
}

interface GoogleLoginApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface GoogleCredentialResponse {
  credential?: string;
  select_by?: string;
}

type GoogleCredentialCallback = (response: GoogleCredentialResponse) => void;

interface GoogleTokenClient {
  requestAccessToken?: () => void;
  requestIdToken?: (options?: { nonce?: string }) => void;
  callback?: GoogleCredentialCallback;
}

interface GoogleAccountsId {
  initialize: (config: {
    client_id: string;
    callback: GoogleCredentialCallback;
    auto_select?: boolean;
    cancel_on_tap_outside?: boolean;
    use_fedcm_for_prompt?: boolean;
  }) => GoogleTokenClient;
  prompt: (listener?: (notification: { isNotDisplayed?: () => boolean; isSkippedMoment?: () => boolean; isDismissedMoment?: () => boolean; getNotDisplayedReason?: () => string; getSkippedReason?: () => string; getDismissedReason?: () => string }) => void) => void;
}

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: GoogleAccountsId;
      };
    };
  }
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
  private http = inject(HttpClient); // Injecting HttpClient for profile fetching

  private readonly tokenKey = 'ecotrack_token';
  private readonly userKey = 'ecotrack_user';
  private readonly googleClientId = environment.googleClientId;
  private googleScriptPromise: Promise<void> | null = null;

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
  // GET USER PROFILE (Sidebar, Navbar, Profile ke liye)
  // ==========================================
  public async getUserProfile(): Promise<any> {
    const token = this.getToken();
    
    if (!token) {
      throw new Error("No token found");
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    try {
      const response: any = await firstValueFrom(
        this.http.get('http://localhost:8080/api/users/profile', { headers })
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
  // GOOGLE LOGIN (Added callback parameter)
  // ==========================================
  public async loginWithGoogle(callback?: () => void): Promise<GoogleLoginResponse> {
    const idToken = await this.requestGoogleIdToken();

    const response = await firstValueFrom(
      this.apiService.post<GoogleLoginApiResponse<GoogleLoginResponse>>('/api/auth/google', {
        idToken,
      })
    );

    if (!response.success || !response.data?.token) {
      throw new Error(response.message || 'Google authentication failed.');
    }

    this.setSession(response.data.token, response.data.email);
    
    // Execute the callback if it was provided by the component
    if (callback) {
      callback();
    }

    return response.data;
  }

  public getToken(): string | null {
    if (typeof window === 'undefined') {
      return null;
    }

    return localStorage.getItem(this.tokenKey);
  }

  public logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  private setSession(token: string, email: string): void {
    const session: UserSession = { email };

    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(session));
    this.isAuthenticated.set(true);
    this.currentUser.set(session);
  }

  private async requestGoogleIdToken(): Promise<string> {
    await this.loadGoogleIdentityScript();

    const googleAccounts = window.google?.accounts?.id;
    if (!googleAccounts) {
      throw new Error('Google sign-in is unavailable right now.');
    }

    if (!this.googleClientId) {
      throw new Error('Google client ID is not configured.');
    }

    return await new Promise<string>((resolve, reject) => {
      const client = googleAccounts.initialize({
        client_id: this.googleClientId,
        callback: (response) => {
          if (!response.credential) {
            reject(new Error('Google sign-in was cancelled.'));
            return;
          }

          resolve(response.credential);
        },
        auto_select: false,
        cancel_on_tap_outside: true,
        use_fedcm_for_prompt: true,
      });

      client.callback = (response) => {
        if (!response.credential) {
          reject(new Error('Google sign-in was cancelled.'));
          return;
        }

        resolve(response.credential);
      };

      googleAccounts.prompt((notification) => {
        if (notification.isNotDisplayed?.() || notification.isSkippedMoment?.() || notification.isDismissedMoment?.()) {
          const reason = notification.getNotDisplayedReason?.() || notification.getSkippedReason?.() || notification.getDismissedReason?.() || 'cancelled';
          reject(new Error(`Google sign-in was not completed (${reason}).`));
        }
      });
    });
  }

  private async loadGoogleIdentityScript(): Promise<void> {
    if (typeof window === 'undefined') {
      throw new Error('Google sign-in is only available in the browser.');
    }

    if (window.google?.accounts?.id) {
      return;
    }

    if (!this.googleScriptPromise) {
      this.googleScriptPromise = new Promise<void>((resolve, reject) => {
        const existingScript = document.querySelector('script[data-ecotrack-google-gis="true"]');
        if (existingScript) {
          existingScript.addEventListener('load', () => resolve(), { once: true });
          existingScript.addEventListener('error', () => reject(new Error('Failed to load Google Identity Services.')), { once: true });
          return;
        }

        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.setAttribute('data-ecotrack-google-gis', 'true');
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Failed to load Google Identity Services.'));
        document.head.appendChild(script);
      });
    }

    await this.googleScriptPromise;
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