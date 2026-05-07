import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PLATFORM_ID } from '@angular/core';
import { AuthService } from './auth';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should store token and expiration on setSession', () => {
    service.setSession('test-token', 3600);

    expect(localStorage.getItem('auth_token')).toBe('test-token');
    expect(localStorage.getItem('auth_expires_at')).toBeTruthy();
  });

  it('should return token from getToken', () => {
    localStorage.setItem('auth_token', 'my-token');

    expect(service.getToken()).toBe('my-token');
  });

  it('should return null from getToken when no token exists', () => {
    expect(service.getToken()).toBeNull();
  });

  it('should return true from isLoggedIn when token is valid', () => {
    service.setSession('valid-token', 3600);

    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should return false from isLoggedIn when no token exists', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should return false and clear storage when token is expired', () => {
    localStorage.setItem('auth_token', 'expired-token');
    localStorage.setItem('auth_expires_at', String(Date.now() - 1000));

    expect(service.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('should clear localStorage on logout', () => {
    service.setSession('token', 3600);
    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(localStorage.getItem('auth_expires_at')).toBeNull();
  });

  it('should send POST to login endpoint', () => {
    service.login('user1', 'pass1').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'user1', password: 'pass1' });
    req.flush({});
  });

  it('should send POST to register endpoint', () => {
    service.register('newuser', 'newpass').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'newuser', password: 'newpass' });
    req.flush({});
  });
});