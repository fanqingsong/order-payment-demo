import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService, AuthUser } from '@core/auth/auth.service';
import { OrderApiService } from '@features/orders/order-api.service';

describe('OrderApiService', () => {
  let httpTesting: HttpTestingController;

  afterEach(() => {
    httpTesting.verify();
  });

  function createService(role: AuthUser['role']): OrderApiService {
    const user: AuthUser = {
      userId: crypto.randomUUID(),
      email: `${role.toLowerCase()}@example.com`,
      role,
      exp: Math.floor(Date.now() / 1000) + 3600,
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user: () => user } },
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);
    return TestBed.inject(OrderApiService);
  }

  it('loads all orders from the admin endpoint for an admin', () => {
    const service = createService('ADMIN');

    service.getOrders().subscribe((orders) => expect(orders).toEqual([]));

    httpTesting.expectOne('/api/admin/orders').flush([]);
  });

  it('loads only owned orders from the regular endpoint for a user', () => {
    const service = createService('USER');

    service.getOrders().subscribe((orders) => expect(orders).toEqual([]));

    httpTesting.expectOne('/api/orders').flush([]);
  });
});
