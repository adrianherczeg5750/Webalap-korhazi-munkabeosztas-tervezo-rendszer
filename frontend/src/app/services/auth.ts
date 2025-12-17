import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {environment} from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  private apiBaseUrl = environment.apiUrl;

  login(username: string, password: string) {
    return this.http.post(`${this.apiBaseUrl}/auth/login`, { username, password });
  }

  register(username: string, password: string) {
    return this.http.post(`${this.apiBaseUrl}/auth/register`, { username, password });
  }
}
