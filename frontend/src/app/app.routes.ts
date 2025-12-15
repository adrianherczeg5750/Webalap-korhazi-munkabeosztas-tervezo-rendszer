import { Routes } from '@angular/router';
import { LoginComponent } from './login/login';
import {RegisterComponent} from './register/register';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'register', pathMatch: 'full' },
  { path: 'register', component: RegisterComponent }
];
