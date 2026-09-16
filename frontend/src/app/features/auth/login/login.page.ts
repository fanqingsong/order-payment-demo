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
        <p class="error" role="alert">{{ error() }}</p>
      }
      <button type="submit" [disabled]="form.invalid || submitting()">
        {{ submitting() ? 'Logging in…' : 'Log in' }}
      </button>
    </form>
    <p>Need an account? <a routerLink="/register">Register</a>.</p>
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
        error: (error: unknown) => this.error.set(apiErrorMessage(error)),
      });
  }
}
