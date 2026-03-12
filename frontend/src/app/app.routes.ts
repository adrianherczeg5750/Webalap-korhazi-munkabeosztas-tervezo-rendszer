import { CanActivateFn, Router, Routes } from '@angular/router';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { MainPageComponent } from './main-page/main-page';
import { ShiftComponent } from './shift/shift';
import { inject } from '@angular/core';
import { AuthService } from './services/auth';
import {AdminComponent} from './admin/admin';
import {ManagerComponent} from './manager/manager';
import {LeaveRequestComponent} from './leave-request/leave-request';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;

  return router.navigate(['/login']);
};

export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    const role = localStorage.getItem('role');

    if (role && allowedRoles.includes(role)) {
      return true;
    }
    return router.navigate(['/main-page']);
  };
};


export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'main-page', component: MainPageComponent , canActivate: [authGuard, roleGuard(['ADMIN','EMPLOYEE'])]},
  { path: 'shift-add', component: ShiftComponent, canActivate: [authGuard, roleGuard(['ADMIN','EMPLOYEE'])] },
  { path: 'leave-request', component: LeaveRequestComponent, canActivate: [authGuard, roleGuard(['ADMIN','EMPLOYEE'])] },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard, roleGuard(['ADMIN'])] },
  { path: 'manager', component: ManagerComponent, canActivate: [authGuard, roleGuard(['ADMIN','MANAGER'])] },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
