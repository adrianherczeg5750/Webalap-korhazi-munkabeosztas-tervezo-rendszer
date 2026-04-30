import { Component } from '@angular/core';
import {AuthService} from '../services/auth';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  imports: [
    FormsModule
  ],
  styleUrls: ['./login.css']
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  role: string = '';
  errorMessage: string = '';

  constructor(private readonly authService: AuthService, private readonly routes: Router) {}

  onLogin() {
    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        console.log("Sikeres belépés!", response);
        const token = (response as any).token as string;
        const expiresInSeconds = (response as any).expiresInSeconds as number;
        const role = (response as any).role as string;
        const userId = (response as any).id as number;
        const username = (response as any).username as string;

        this.authService.setSession(token, expiresInSeconds);
        localStorage.setItem('token', token);
        localStorage.setItem('userId', String(userId));
        localStorage.setItem('username', username || this.username);
        localStorage.setItem('role', role);

        switch (role) {
          case 'ADMIN':
            this.routes.navigate(['/admin']);
            break;
          case 'MANAGER':
            this.routes.navigate(['/manager']);
            break;
          case 'EMPLOYEE':
          default:
            this.routes.navigate(['/main-page']);
            break;
        }
      },
      error: (err) => {
        this.errorMessage = "Hibás felhasználónév vagy jelszó!";
      }
    });
  }
  onClickRegister(){
    this.routes.navigate(['/register']);
  }
}
