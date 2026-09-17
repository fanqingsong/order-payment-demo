import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '@core/auth/auth.service';

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PAYMENT_FAILED';

export interface Order {
  id: string;
  userId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  productName: string;
  quantity: number;
  unitPrice: number;
}

@Injectable({ providedIn: 'root' })
export class OrderApiService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  getOrders(): Observable<Order[]> {
    const endpoint = this.auth.user()?.role === 'ADMIN' ? '/api/admin/orders' : '/api/orders';
    return this.http.get<Order[]>(endpoint);
  }

  getOrder(id: string): Observable<Order> {
    return this.http.get<Order>(`/api/orders/${id}`);
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>('/api/orders', request);
  }
}
