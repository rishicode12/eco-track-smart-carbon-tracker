import { Injectable, inject } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../../features/auth/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  async canActivate(): Promise<boolean> {
    try {
      // Ensure the profile (which carries the role) is loaded before checking.
      if (!this.authService.userProfile()) {
        await this.authService.getUserProfile();
      }
    } catch (error) {
      console.error('AdminGuard: failed to load user profile', error);
    }

    if (this.authService.isAdmin()) {
      return true;
    }

    this.router.navigate(['/dashboard']);
    return false;
  }
}