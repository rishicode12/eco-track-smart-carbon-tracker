import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Component, ElementRef, HostListener, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';
import { COUNTRIES } from './countries';

const passwordsMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css'],
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private hostElement = inject(ElementRef<HTMLElement>);

  public isLoading = false;
  public isSuccess = false;
  public errorMessage = '';
  public fieldErrors: Record<string, string> = {};
  public showPassword = false;
  public countryDropdownOpen = false;
  public countrySearchTerm = '';
  public filteredCountries = [...COUNTRIES];

  public signupForm = this.fb.group(
  {
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.pattern('^[a-zA-Z0-9._%+-]+@(gmail\\.com|outlook\\.com)$') // Cleaned up regex string below:
      ]
    ],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
    company: [''],
    country: ['', [Validators.required]],
    acceptTerms: [false, [Validators.requiredTrue]],
  },
  { validators: passwordsMatchValidator }
);

  get fullName() { return this.signupForm.get('fullName'); }
  get email() { return this.signupForm.get('email'); }
  get password() { return this.signupForm.get('password'); }
  get confirmPassword() { return this.signupForm.get('confirmPassword'); }
  get company() { return this.signupForm.get('company'); }
  get country() { return this.signupForm.get('country'); }
  get acceptTerms() { return this.signupForm.get('acceptTerms'); }

  @HostListener('document:click', ['$event'])
  public onDocumentClick(event: MouseEvent): void {
    if (!this.hostElement.nativeElement.contains(event.target as Node)) {
      this.closeCountryDropdown();
    }
  }

  public onSubmit(): void {
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.fieldErrors = {};

    const fullName = this.signupForm.value.fullName as string;
    const email = this.signupForm.value.email as string;
    const password = this.signupForm.value.password as string;
    const country = this.signupForm.value.country as string;

    this.authService.register(fullName, email, password, country)
      .then(() => {
        this.isLoading = false;
        this.isSuccess = true;

        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 1500);
      })
      .catch((error: unknown) => {
        this.isLoading = false;
        this.applyAuthError(error, 'Registration failed. Please review your details and try again.');
      });
  }

  public async onGoogleSignup(): Promise<void> {
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
      this.applyAuthError(error, 'Google sign-up failed. Please try again.');
    } finally {
      this.isLoading = false;
    }
  }

  public openCountryDropdown(): void {
    this.countryDropdownOpen = true;
    this.filterCountries(this.countrySearchTerm || (this.country?.value as string) || '');
  }

  public onCountryInput(event: Event): void {
    const searchTerm = (event.target as HTMLInputElement).value;
    this.countrySearchTerm = searchTerm;
    this.filterCountries(searchTerm);
    this.countryDropdownOpen = true;

    const exactMatch = COUNTRIES.find((country) => country.toLowerCase() === searchTerm.trim().toLowerCase()) ?? '';
    this.country?.setValue(exactMatch, { emitEvent: false });
    this.country?.markAsDirty();
  }

  public onCountryKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeCountryDropdown();
      return;
    }

    if (event.key === 'Enter' && this.filteredCountries.length > 0) {
      event.preventDefault();
      this.selectCountry(this.filteredCountries[0]);
    }
  }

  public onCountryBlur(): void {
    this.country?.markAsTouched();

    window.setTimeout(() => {
      this.closeCountryDropdown();
    }, 150);
  }

  public selectCountry(country: string): void {
    this.countrySearchTerm = country;
    this.country?.setValue(country);
    this.country?.markAsTouched();
    this.country?.markAsDirty();
    this.filterCountries(country);
    this.closeCountryDropdown();
  }

  public clearCountry(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.countrySearchTerm = '';
    this.country?.setValue('');
    this.filterCountries('');
    this.countryDropdownOpen = true;
  }

  public trackByCountry(_: number, country: string): string {
    return country;
  }

  public getFieldError(fieldName: string): string | null {
    return this.fieldErrors[fieldName] ?? null;
  }

  public get passwordMismatch(): boolean {
    return this.signupForm.hasError('passwordMismatch') && this.signupForm.touched;
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

  private filterCountries(searchTerm: string): void {
    const normalizedSearchTerm = searchTerm.trim().toLowerCase();

    this.filteredCountries = COUNTRIES.filter((country) =>
      country.toLowerCase().includes(normalizedSearchTerm)
    );
  }

  private closeCountryDropdown(): void {
    this.countryDropdownOpen = false;
  }

}