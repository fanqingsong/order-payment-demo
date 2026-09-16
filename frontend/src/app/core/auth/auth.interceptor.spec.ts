import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@core/auth/auth.service';
import { authInterceptor } from '@core/auth/auth.interceptor';

describe('authInterceptor', () => {
  it('adds the bearer token to outgoing API requests', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => 'test-token' } },
      ],
    });
    const controller = TestBed.inject(HttpTestingController);
    const client = TestBed.inject(HttpClient);

    client.get('/api/orders').subscribe();

    const request = controller.expectOne('/api/orders');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    request.flush([]);
    controller.verify();
  });
});
