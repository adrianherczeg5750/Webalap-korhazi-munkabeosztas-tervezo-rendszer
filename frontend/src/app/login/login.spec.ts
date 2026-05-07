import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../services/auth';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['login', 'setSession']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should navigate to /admin on ADMIN login', () => {
    authService.login.and.returnValue(
      of({ token: 't', expiresInSeconds: 3600, role: 'ADMIN', id: 1, username: 'admin' })
    );

    component.username = 'admin';
    component.password = 'pass';
    component.onLogin();

    expect(authService.setSession).toHaveBeenCalledWith('t', 3600);
    expect(router.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('should navigate to /manager on MANAGER login', () => {
    authService.login.and.returnValue(
      of({ token: 't', expiresInSeconds: 3600, role: 'MANAGER', id: 2, username: 'mgr' })
    );

    component.username = 'mgr';
    component.password = 'pass';
    component.onLogin();

    expect(router.navigate).toHaveBeenCalledWith(['/manager']);
  });

  it('should navigate to /main-page on EMPLOYEE login', () => {
    authService.login.and.returnValue(
      of({ token: 't', expiresInSeconds: 3600, role: 'EMPLOYEE', id: 3, username: 'emp' })
    );

    component.username = 'emp';
    component.password = 'pass';
    component.onLogin();

    expect(router.navigate).toHaveBeenCalledWith(['/main-page']);
  });

  it('should store userId and role in localStorage', () => {
    authService.login.and.returnValue(
      of({ token: 't', expiresInSeconds: 3600, role: 'EMPLOYEE', id: 5, username: 'user5' })
    );

    component.username = 'user5';
    component.password = 'pass';
    component.onLogin();

    expect(localStorage.getItem('userId')).toBe('5');
    expect(localStorage.getItem('role')).toBe('EMPLOYEE');
    expect(localStorage.getItem('username')).toBe('user5');
  });

  it('should set error message on login failure', () => {
    authService.login.and.returnValue(throwError(() => new Error('401')));

    component.onLogin();

    expect(component.errorMessage).toBe('Hibás felhasználónév vagy jelszó!');
  });

  it('should navigate to /register on onClickRegister', () => {
    component.onClickRegister();

    expect(router.navigate).toHaveBeenCalledWith(['/register']);
  });
});