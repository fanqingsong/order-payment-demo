import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { apiErrorMessage } from '@core/api-error';
import { Order, OrderApiService } from '@features/orders/order-api.service';

@Component({
  selector: 'app-order-detail-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <p><a routerLink="/orders">← Back to orders</a></p>
    <h1>Order details</h1>
    @if (loading()) {
      <p>Loading order…</p>
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
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
})
export class OrderDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly orderApi = inject(OrderApiService);

  protected readonly order = signal<Order | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Order ID is missing.');
      this.loading.set(false);
      return;
    }

    this.orderApi.getOrder(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
