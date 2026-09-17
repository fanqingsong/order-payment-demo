import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/auth.guard';
import { guestGuard } from '@core/auth/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'orders' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('@features/auth/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('@features/auth/register/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/orders/order-list/order-list.page').then((m) => m.OrderListPage),
  },
  {
    path: 'orders/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/orders/order-create/order-create.page').then((m) => m.OrderCreatePage),
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/orders/order-detail/order-detail.page').then((m) => m.OrderDetailPage),
  },
  { path: '**', redirectTo: 'orders' },
];
