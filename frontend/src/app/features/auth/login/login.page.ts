import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '@core/api-error';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Log in</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>
        Email
        <input type="email" formControlName="email" autocomplete="email" />
      </label>
      <label>
        Password
        <input type="password" formControlName="password" autocomplete="current-password" />
      </label>
      @if (error()) {
        <div class="login-error" role="alert">
          <strong>Login failed</strong>
          <span>{{ error() }}</span>
        </div>
      }
      <button type="submit" [disabled]="form.invalid || submitting()">
        {{ submitting() ? 'Logging in…' : 'Log in' }}
      </button>
    </form>
    <p>Need an account? <a routerLink="/register">Register</a>.</p>
  `,
  styles: `
    .login-error {
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
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/orders';
          void this.router.navigateByUrl(returnUrl);
        },
        error: (error: unknown) => this.error.set(this.loginErrorMessage(error)),
      });
  }

  private loginErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Incorrect email or password. Please try again.';
    }

    return apiErrorMessage(error);
  }
}
