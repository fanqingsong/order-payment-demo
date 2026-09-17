import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { apiErrorMessage } from '@core/api-error';
import { Order, OrderApiService } from '@features/orders/order-api.service';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

@Component({
  selector: 'app-order-detail-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <p><a routerLink="/orders">← Back to orders</a></p>
    <h1>Order details</h1>
    @if (loading()) {
      <p>Loading order…</p>
    } @else if (error()) {
      <section class="error-state" role="alert" aria-labelledby="order-error-title">
        <h2 id="order-error-title">Unable to show this order</h2>
        <p>{{ error() }}</p>
        <a class="primary-link" routerLink="/orders">Return to orders</a>
      </section>
    } @else if (order(); as currentOrder) {
      <dl class="details">
        <dt>ID</dt><dd>{{ currentOrder.id }}</dd>
        <dt>Product</dt><dd>{{ currentOrder.productName }}</dd>
        <dt>Quantity</dt><dd>{{ currentOrder.quantity }}</dd>
        <dt>Unit price</dt><dd>{{ currentOrder.unitPrice | currency }}</dd>
        <dt>Total</dt><dd>{{ currentOrder.totalAmount | currency }}</dd>
        <dt>Status</dt><dd>{{ currentOrder.status }}</dd>
        <dt>Created</dt><dd>{{ currentOrder.createdAt | date: 'medium' }}</dd>
        <dt>Updated</dt><dd>{{ currentOrder.updatedAt | date: 'medium' }}</dd>
      </dl>
    }
  `,
  styles: `
    .error-state {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: 0.5rem;
      max-width: 38rem;
      padding: 1.25rem;
    }

    .error-state h2 {
      color: #991b1b;
      font-size: 1.15rem;
      margin: 0 0 0.5rem;
    }

    .error-state p {
      margin: 0 0 1rem;
    }

    .primary-link {
      border-radius: 0.25rem;
      display: inline-block;
      padding: 0.55rem 0.9rem;
      text-decoration: none;
    }
  `,
})
export class OrderDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApiService);

  protected readonly order = signal<Order | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id || !UUID_PATTERN.test(id)) {
      this.error.set('The order ID in the address is invalid. Check the link and try again.');
      this.loading.set(false);
      return;
    }

    this.orderApi.getOrder(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.orderErrorMessage(error));
        this.loading.set(false);
      },
    });
  }

  private orderErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 404) {
        return 'This order does not exist, or you do not have permission to view it.';
      }
      if (error.status === 403) {
        return 'You do not have permission to view this order.';
      }
    }

    return apiErrorMessage(error);
  }
}
