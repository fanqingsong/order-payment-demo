import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { throwError } from 'rxjs';

import { AuthService } from '@core/auth/auth.service';
import { RegisterPage } from '@features/auth/register/register.page';

describe('RegisterPage', () => {
  let fixture: ComponentFixture<RegisterPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            register: vi.fn().mockReturnValue(
              throwError(() => new HttpErrorResponse({ status: 409 })),
            ),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(RegisterPage);
    fixture.detectChanges();
  });

  it('shows a friendly message when the email is already registered', () => {
    const element = fixture.nativeElement as HTMLElement;
    const email = element.querySelector<HTMLInputElement>('input[type="email"]')!;
    const password = element.querySelector<HTMLInputElement>('input[type="password"]')!;

    email.value = 'existing@example.com';
    email.dispatchEvent(new Event('input'));
    password.value = 'password123';
    password.dispatchEvent(new Event('input'));
    element.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'An account already exists for this email. Try logging in instead.',
    );
  });
});
