import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { PLATFORM_ID } from '@angular/core';
import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from './auth';

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: PLATFORM_ID, useValue: 'browser' },
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should add Authorization header when token exists', () => {
    authService.setSession('my-token', 3600);

    http.get('/api/shifts').subscribe();

    const req = httpMock.expectOne('/api/shifts');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
    req.flush({});
  });

  it('should not add Authorization header when no token', () => {
    http.get('/api/shifts').subscribe();

    const req = httpMock.expectOne('/api/shifts');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should skip auth header for login requests', () => {
    authService.setSession('my-token', 3600);

    http.post('/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should skip auth header for register requests', () => {
    authService.setSession('my-token', 3600);

    http.post('/auth/register', {}).subscribe();

    const req = httpMock.expectOne('/auth/register');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});