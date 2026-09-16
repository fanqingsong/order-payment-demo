import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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

  getOrders(): Observable<Order[]> {
    return this.http.get<Order[]>('/api/orders');
  }

  getOrder(id: string): Observable<Order> {
    return this.http.get<Order>(`/api/orders/${id}`);
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>('/api/orders', request);
  }
}
