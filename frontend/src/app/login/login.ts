import { Component } from '@angular/core';
import {AuthService} from '../services/auth';
import {FormsModule} from '@angular/forms';
import {NgIf} from '@angular/common';
import {Router} from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  imports: [
    FormsModule,
    NgIf
  ],
  styleUrls: ['./login.css']
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  errorMessage: string = '';

  constructor(private readonly authService: AuthService, private readonly routes: Router) {}

  onLogin() {
    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        console.log("Sikeres belépés!", response);
        this.routes.navigate(['/main-page']);
        localStorage.setItem("Name", this.username);
      },
      error: (err) => {
        this.errorMessage = "Hibás felhasználónév vagy jelszó!";
      }
    });
  }
  onClickRegister(){
    this.routes.navigate(['/register']);
  }
  navigateToIndex(){
    this.routes.navigate(['/main-page']);
  }
}
