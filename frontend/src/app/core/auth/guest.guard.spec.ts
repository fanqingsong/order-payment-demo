import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { guestGuard } from '@core/auth/guest.guard';
import { AuthService } from '@core/auth/auth.service';

describe('guestGuard', () => {
  function runGuard(isAuthenticated: boolean): boolean | UrlTree {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => isAuthenticated } },
        {
          provide: Router,
          useValue: { createUrlTree: vi.fn().mockReturnValue({ redirectTo: '/orders' }) },
        },
      ],
    });

    return TestBed.runInInjectionContext(
      () =>
        guestGuard(
          {} as ActivatedRouteSnapshot,
          {} as RouterStateSnapshot,
        ) as boolean | UrlTree,
    );
  }

  it('allows unauthenticated users to open an authentication page', () => {
    expect(runGuard(false)).toBe(true);
  });

  it('redirects authenticated users away from authentication pages', () => {
    expect(runGuard(true)).toEqual({ redirectTo: '/orders' });
  });
});
