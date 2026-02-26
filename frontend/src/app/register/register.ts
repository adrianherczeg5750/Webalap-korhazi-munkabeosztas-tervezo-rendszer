import { Component } from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {NgIf} from '@angular/common';
import {AuthService} from '../services/auth';
import {Router} from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    FormsModule,
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent {
  username: string = '';
  password: string = '';
  role: string = '';
  errorMessage: string = '';

  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  onRegister(){
    this.authService.register(this.username, this.password, this.role).subscribe({
      next: () => {
        console.log("Sikeres regisztráció!");
        this.router.navigate(['/login']);
      },
      error: () => {
        this.errorMessage = "Hibás felhasználónév vagy jelszó!";
      }
    });
  }

  navigateToLogin(){
    this.router.navigate(['/login']);
  }

}
