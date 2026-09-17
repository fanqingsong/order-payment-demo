import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '@core/api-error';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Register</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>
        Email
        <input type="email" formControlName="email" autocomplete="email" />
      </label>
      <label>
        Password
        <input type="password" formControlName="password" autocomplete="new-password" />
        <small>Use 8–72 characters.</small>
      </label>
      @if (error()) {
        <div class="register-error" role="alert">
          <strong>Registration failed</strong>
          <span>{{ error() }}</span>
        </div>
      }
      <button type="submit" [disabled]="form.invalid || submitting()">
        {{ submitting() ? 'Creating account…' : 'Register' }}
      </button>
    </form>
    <p>Already registered? <a routerLink="/login">Log in</a>.</p>
  `,
  styles: `
    .register-error {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 0.375rem;
      color: #991b1b;
      display: grid;
      gap: 0.2rem;
      padding: 0.75rem 0.9rem;
    }
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    this.auth
      .register(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/login']),
        error: (error: unknown) => this.error.set(this.registrationErrorMessage(error)),
      });
  }

  private registrationErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'An account already exists for this email. Try logging in instead.';
    }

    return apiErrorMessage(error);
  }
}
