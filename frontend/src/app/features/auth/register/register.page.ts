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
        <p class="error" role="alert">{{ error() }}</p>
      }
      <button type="submit" [disabled]="form.invalid || submitting()">
        {{ submitting() ? 'Creating account…' : 'Register' }}
      </button>
    </form>
    <p>Already registered? <a routerLink="/login">Log in</a>.</p>
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
        error: (error: unknown) => this.error.set(apiErrorMessage(error)),
      });
  }
}
