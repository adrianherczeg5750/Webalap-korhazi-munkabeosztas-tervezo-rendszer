import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrlLogin = 'http://localhost:8080/auth/login';
  private apiUrlRegister = 'http://localhost:8080/auth/register';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<any> {
    const body = {
      username: username,
      password: password
    };
    return this.http.post<any>(this.apiUrlLogin, body);
  }

  register(username: string, password: string): Observable<any>{
    const body = {
      username: username,
      password: password
    };
    return this.http.post<any>(this.apiUrlRegister, body);
  }
}
