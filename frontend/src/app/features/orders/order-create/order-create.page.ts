import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '@core/api-error';
import { OrderApiService } from '@features/orders/order-api.service';

@Component({
  selector: 'app-order-create-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h1>Create order</h1>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <label>
        Product name
        <input type="text" formControlName="productName" />
      </label>
      <label>
        Quantity
        <input type="number" min="1" step="1" formControlName="quantity" />
      </label>
      <label>
        Unit price
        <input type="number" min="0.01" step="0.01" formControlName="unitPrice" />
      </label>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <div class="actions">
        <button type="submit" [disabled]="form.invalid || submitting()">
          {{ submitting() ? 'Creating…' : 'Create order' }}
        </button>
        <a class="secondary-link" routerLink="/orders">Cancel</a>
      </div>
    </form>
  `,
})
export class OrderCreatePage {
  private readonly orderApi = inject(OrderApiService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly error = signal('');
  protected readonly form = new FormGroup({
    productName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(255)],
    }),
    quantity: new FormControl(1, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1)],
    }),
    unitPrice: new FormControl(0.01, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0.01)],
    }),
  });

  protected submit(): void {
    if (this.form.invalid) return;
    this.submitting.set(true);
    this.error.set('');
    this.orderApi
      .createOrder(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (order) => void this.router.navigate(['/orders', order.id]),
        error: (error: unknown) => this.error.set(apiErrorMessage(error)),
      });
  }
}
