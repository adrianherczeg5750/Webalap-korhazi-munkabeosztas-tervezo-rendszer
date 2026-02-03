import { Routes } from '@angular/router';
import { LoginComponent } from './login/login';
import { RegisterComponent } from './register/register';
import { MainPageComponent } from './main-page/main-page';
import {ShiftComponent} from './shift/shift';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'main-page', component: MainPageComponent },
  { path: 'shift-add', component: ShiftComponent },

  { path: '', redirectTo: 'register', pathMatch: 'full' },
  { path: '**', redirectTo: 'register' }
];
