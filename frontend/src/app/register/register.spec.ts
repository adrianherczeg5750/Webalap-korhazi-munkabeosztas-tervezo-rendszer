import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register';
import { AuthService } from '../services/auth';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['register']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show error when passwords do not match', () => {
    component.username = 'user1';
    component.password = 'pass1';
    component.confirmPassword = 'pass2';

    component.onRegister();

    expect(component.errorMessage).toBe('A két jelszó nem egyezik meg.');
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('should call register and navigate to login on success', () => {
    authService.register.and.returnValue(of({}));

    component.username = 'user1';
    component.password = 'pass1';
    component.confirmPassword = 'pass1';

    component.onRegister();

    expect(authService.register).toHaveBeenCalledWith('user1', 'pass1');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should show error on register failure', () => {
    authService.register.and.returnValue(throwError(() => new Error('conflict')));

    component.username = 'user1';
    component.password = 'pass1';
    component.confirmPassword = 'pass1';

    component.onRegister();

    expect(component.errorMessage).toBe('Hibás felhasználónév vagy jelszó!');
  });

  it('should navigate to login on navigateToLogin', () => {
    component.navigateToLogin();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});