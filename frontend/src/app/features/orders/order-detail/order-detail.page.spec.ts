import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { Observable, throwError } from 'rxjs';

import { Order, OrderApiService } from '@features/orders/order-api.service';
import { OrderDetailPage } from '@features/orders/order-detail/order-detail.page';

describe('OrderDetailPage', () => {
  const validOrderId = '8be2dc82-31e4-4c37-a3aa-79a0e9d53b36';

  function createPage(
    id: string,
    getOrder: (orderId: string) => Observable<Order>,
  ): ComponentFixture<OrderDetailPage> {
    TestBed.configureTestingModule({
      imports: [OrderDetailPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => id } } },
        },
        { provide: OrderApiService, useValue: { getOrder } },
      ],
    });

    const fixture = TestBed.createComponent(OrderDetailPage);
    fixture.detectChanges();
    return fixture;
  }

  it('shows an invalid ID message without requesting the API', () => {
    const getOrder = vi.fn();

    const fixture = createPage('not-a-uuid', getOrder);

    expect(getOrder).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('The order ID in the address is invalid');
  });

  it('does not reveal whether a missing or inaccessible order exists', () => {
    const getOrder = vi.fn().mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            error: { message: `Order not found: ${validOrderId}` },
          }),
      ),
    );

    const fixture = createPage(validOrderId, getOrder);

    expect(getOrder).toHaveBeenCalledWith(validOrderId);
    expect(fixture.nativeElement.textContent).toContain(
      'This order does not exist, or you do not have permission to view it.',
    );
  });
});
