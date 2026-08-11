import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';
import { HttpErrorResponse } from '@angular/common/http';

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

  public togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  public async onSSOLogin(): Promise<void> {
    this.errorMessage = '';

    try {
      await this.authService.loginWithGoogle(() => {
        this.isLoading = true;
      });
      this.isSuccess = true;

      setTimeout(() => {
        this.router.navigate(['/dashboard']);
      }, 1500);
    } catch (error: unknown) {
      this.applyAuthError(error, 'Google sign-in failed. Please try again.');
    } finally {
      this.isLoading = false;
    }
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
        this.applyAuthError(error, 'Authentication failed. Please check your credentials.');
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

