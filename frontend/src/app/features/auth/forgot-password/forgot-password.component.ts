import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  public forgotPasswordForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  public isLoading = false;
  public isSuccess = false;
  public errorMessage = '';

  get email() { return this.forgotPasswordForm.get('email'); }

  public async onSubmit(): Promise<void> {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      const email = this.forgotPasswordForm.value.email as string;
      await this.authService.forgotPassword(email);
      this.isSuccess = true;
    } catch (error: any) {
      this.errorMessage = error?.error?.message || 'Failed to send reset email. Please try again.';
    } finally {
      this.isLoading = false;
    }
  }
}
