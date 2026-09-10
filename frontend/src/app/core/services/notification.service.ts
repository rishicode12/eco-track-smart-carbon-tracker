import { Injectable, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../features/auth/auth.service';

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: string; // LEVEL_UP, BADGE_UNLOCK, CHALLENGE_COMPLETED
  isRead: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly basePath = `${environment.apiUrl}/api/notifications`;
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();
  private authService = inject(AuthService);

  getNotifications(): Promise<{ notifications: NotificationItem[], unreadCount: number }> {
    const token = this.authService.getToken();
    if (!this.authService.isAuthenticated() || !token) {
      return Promise.resolve({ notifications: [], unreadCount: 0 });
    }

    return fetch(`${this.basePath}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      credentials: 'include',
    })
      .then((response) => {
        if (!response.ok) throw new Error('Failed to fetch notifications');
        return response.json();
      })
      .then((data) => {
        this.unreadCountSubject.next(data.unreadCount || 0);
        return data;
      })
      .catch((error) => {
        console.error('Error fetching notifications:', error);
        return { notifications: [], unreadCount: 0 };
      });
  }

  markAsRead(id: number): Promise<void> {
    const token = this.authService.getToken();
    if (!this.authService.isAuthenticated() || !token) {
      return Promise.resolve();
    }

    return fetch(`${this.basePath}/${id}/read`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      credentials: 'include',
    })
      .then((response) => {
        if (!response.ok) throw new Error('Failed to mark as read');
        return response.json();
      })
      .then(() => {
        this.unreadCountSubject.next(Math.max(this.unreadCountSubject.value - 1, 0));
      })
      .catch((error) => {
        console.error('Error marking notification as read:', error);
      });
  }

  markAllAsRead(): Promise<void> {
    const token = this.authService.getToken();
    if (!this.authService.isAuthenticated() || !token) {
      return Promise.resolve();
    }

    return fetch(`${this.basePath}/read-all`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      credentials: 'include',
    })
      .then((response) => {
        if (!response.ok) throw new Error('Failed to mark all as read');
        return response.json();
      })
      .then(() => {
        this.unreadCountSubject.next(0);
      })
      .catch((error) => {
        console.error('Error marking all as read:', error);
      });
  }
}