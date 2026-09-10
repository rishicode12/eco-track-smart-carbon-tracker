import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserNotFoundError } from './auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);

  // Simple state variables using standard JS structures
  public loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false]
  });

  // Getters for form controls for clean HTML template validations
  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }

  public showPassword = false;
  public isLoading = false;
  public isSuccess = false;
  public errorMessage = '';
  public fieldErrors: Record<string, string> = {};
  public showUserNotFoundModal = false;

  public togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  public onSSOLogin(): void {
    window.location.href = `${environment.apiUrl.replace('/api', '')}/oauth2/authorization/google`;
  }

  public onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.fieldErrors = {};

    const email = this.loginForm.value.email as string;
    const password = this.loginForm.value.password as string;

    this.authService.login(email, password)
      .then(() => {
        this.isLoading = false;
        this.isSuccess = true;
        
        // Hold success checkmark briefly before redirecting
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 1500);
      })
      .catch((error: unknown) => {
        this.isLoading = false;
        const status = (error as any)?.status ?? (error as any)?.statusCode;
        const message = (error as any)?.error?.message || (error as any)?.message;

        if (
          error instanceof UserNotFoundError ||
          (error as any)?.code === 'USER_NOT_FOUND' ||
          status === 404 ||
          (error as any)?.error?.status === 404 ||
          message === 'USER_NOT_FOUND' ||
          (typeof message === 'string' &&
            (message.toLowerCase().includes('user_not_found') ||
             message.toLowerCase().includes('user not found')))
        ) {
          this.showUserNotFoundModal = true;
          this.errorMessage = '';
          return;
        }
        this.applyAuthError(error, 'Authentication failed. Please check your credentials.');
      });
  }

  public closeUserNotFoundModal(): void {
    this.showUserNotFoundModal = false;
  }

  public navigateToSignup(): void {
    this.showUserNotFoundModal = false;
    const email = this.loginForm.value.email;
    this.router.navigate(['/auth/signup'], {
      queryParams: email ? { email } : {}
    });
  }

  public getFieldError(fieldName: string): string | null {
    return this.fieldErrors[fieldName] ?? null;
  }

  private applyAuthError(error: unknown, fallbackMessage: string): void {
    if (error instanceof HttpErrorResponse) {
      const backendErrors = error.error?.data;

      if (backendErrors && typeof backendErrors === 'object' && !Array.isArray(backendErrors)) {
        this.fieldErrors = backendErrors as Record<string, string>;
        return;
      }

      this.errorMessage = error.error?.message || fallbackMessage;
      return;
    }

    this.errorMessage = error instanceof Error ? error.message : fallbackMessage;
  }
}

