import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '@core/api-error';
import { Order, OrderApiService } from '@features/orders/order-api.service';

@Component({
  selector: 'app-order-list-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
    <div class="actions">
      <h1>Orders</h1>
      <a class="primary-link" routerLink="/orders/new">Create order</a>
    </div>
    @if (loading()) {
      <p>Loading orders…</p>
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    } @else if (orders().length === 0) {
      <p>No orders yet.</p>
    } @else {
      <table>
        <thead>
          <tr><th>Product</th><th>Total</th><th>Status</th><th>Created</th></tr>
        </thead>
        <tbody>
          @for (order of orders(); track order.id) {
            <tr>
              <td><a [routerLink]="['/orders', order.id]">{{ order.productName }}</a></td>
              <td>{{ order.totalAmount | currency }}</td>
              <td>{{ order.status }}</td>
              <td>{{ order.createdAt | date: 'medium' }}</td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: `
    .actions { align-items: center; justify-content: space-between; }
    .actions h1 { margin-bottom: 0; }
    .primary-link { border-radius: 0.25rem; padding: 0.55rem 0.9rem; text-decoration: none; }
  `,
})
export class OrderListPage {
  private readonly orderApi = inject(OrderApiService);

  protected readonly orders = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  constructor() {
    this.orderApi.getOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
