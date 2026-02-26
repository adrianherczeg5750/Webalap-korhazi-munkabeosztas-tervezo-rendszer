import { Injectable, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private apiBaseUrl = environment.apiUrl;

  private readonly TOKEN_KEY = 'auth_token';
  private readonly EXPIRES_AT_KEY = 'auth_expires_at';

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';
  }

  setSession(token: string, expiresInSeconds: number): void {
    if (!this.isBrowser()) return;
    const expiresAt = Date.now() + expiresInSeconds * 1000;
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.EXPIRES_AT_KEY, String(expiresAt));
  }

  getToken(): string | null {
    if (!this.isBrowser()) return null;
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    if (!this.isBrowser()) return false;

    const token = this.getToken();
    const expiresAtRaw = localStorage.getItem(this.EXPIRES_AT_KEY);
    if (!token || !expiresAtRaw) return false;

    const expiresAt = Number(expiresAtRaw);
    if (!Number.isFinite(expiresAt) || Date.now() >= expiresAt) {
      this.logout();
      return false;
    }
    return true;
  }

  logout(): void {
    if (!this.isBrowser()) return;
    localStorage.clear()
  }

  login(username: string, password: string) {
    return this.http.post(`${this.apiBaseUrl}/auth/login`, { username, password });
  }

  register(username: string, password: string, role: string) {
    return this.http.post(`${this.apiBaseUrl}/auth/register`, { username, password, role });
  }

}
