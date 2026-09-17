import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { throwError } from 'rxjs';

import { AuthService } from '@core/auth/auth.service';
import { LoginPage } from '@features/auth/login/login.page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null } } },
        },
        {
          provide: AuthService,
          useValue: {
            login: vi.fn().mockReturnValue(
              throwError(() => new HttpErrorResponse({ status: 401 })),
            ),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
  });

  it('shows a friendly message when credentials are rejected', () => {
    const element = fixture.nativeElement as HTMLElement;
    const email = element.querySelector<HTMLInputElement>('input[type="email"]')!;
    const password = element.querySelector<HTMLInputElement>('input[type="password"]')!;

    email.value = 'user@example.com';
    email.dispatchEvent(new Event('input'));
    password.value = 'wrong-password';
    password.dispatchEvent(new Event('input'));
    element.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Incorrect email or password. Please try again.',
    );
  });
});
