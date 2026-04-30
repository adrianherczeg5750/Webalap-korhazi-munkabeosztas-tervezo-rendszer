import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { environment } from '../../../environments/environment';

interface UserDto {
  id: number;
  username: string;
  role: string;
  assigment: string;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class AdminComponent implements OnInit {
  private baseUrl = environment.apiUrl;

  users: (UserDto & { selectedRole: string; selectedAssigment: string })[] = [];

  deleteMonth: string = '';
  availableMonths: string[] = [];
  deleteSuccess: string | null = null;
  deleteError: string | null = null;

  roleSuccess: string | null = null;
  roleError: string | null = null;
  deleteUserError: string | null = null;
  assigmentSuccess: string | null = null;
  assigmentError: string | null = null;

  modalVisible = false;
  modalTargetUser: (UserDto & { selectedRole: string; selectedAssigment: string }) | null = null;

  readonly roles = ['ADMIN', 'MANAGER', 'EMPLOYEE'];
  readonly assigments: { value: string; label: string }[] = [
    { value: 'NOT_ASSIGNED', label: 'Nincs beosztva' },
    { value: 'EMERGENCY', label: 'Sürgősségi' },
    { value: 'INPATIENT', label: 'Fekvőbeteg' },
    { value: 'OUTPATIENT', label: 'Járóbeteg' },
    { value: 'DAY_CARE', label: 'Nappali ellátás' },
    { value: 'NURSING', label: 'Ápolás' },
  ];

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.http.get<UserDto[]>(`${this.baseUrl}/api/admin/users`).subscribe({
      next: (data) => {
        this.users = (data || []).map(u => ({
          ...u,
          selectedRole: u.role,
          selectedAssigment: u.assigment,
        }));
      },
      error: (err) => console.error('User list error', err),
    });

    this.http.get<string[]>(`${this.baseUrl}/api/admin/shifts/months`).subscribe({
      next: (months) => { this.availableMonths = months || []; },
      error: (err) => console.error('Months load error', err),
    });
  }

  get employees() {
    return this.users.filter(u => u.role === 'EMPLOYEE');
  }

  get managers() {
    return this.users.filter(u => u.role === 'MANAGER');
  }

  get admins() {
    return this.users.filter(u => u.role === 'ADMIN');
  }

  getAssigmentLabel(value: string): string {
    return this.assigments.find(a => a.value === value)?.label ?? value;
  }

  changeRole(user: UserDto & { selectedRole: string; selectedAssigment: string }): void {
    this.roleSuccess = null;
    this.roleError = null;

    this.http.put<UserDto>(`${this.baseUrl}/api/admin/users/${user.id}/role`, { role: user.selectedRole })
      .subscribe({
        next: (updated) => {
          user.role = updated.role;
          user.selectedRole = updated.role;
          user.assigment = updated.assigment;
          user.selectedAssigment = updated.assigment;
          this.roleSuccess = `${user.username} szerepköre sikeresen módosítva: ${updated.role}`;
          setTimeout(() => this.roleSuccess = null, 10000);
        },
        error: (err) => {
          const msg = err?.error?.message || err?.error || err?.message;
          this.roleError = msg ? String(msg) : 'Hiba történt a szerepkör módosítása közben.';
          setTimeout(() => this.roleError = null, 10000);
        },
      });
  }

  changeAssigment(user: UserDto & { selectedRole: string; selectedAssigment: string }): void {
    this.assigmentSuccess = null;
    this.assigmentError = null;

    this.http.put<UserDto>(`${this.baseUrl}/api/admin/users/${user.id}/assigment`, { assigment: user.selectedAssigment })
      .subscribe({
        next: (updated) => {
          user.assigment = updated.assigment;
          user.selectedAssigment = updated.assigment;
          const label = this.assigments.find(a => a.value === updated.assigment)?.label ?? updated.assigment;
          this.assigmentSuccess = `${user.username} beosztása sikeresen módosítva: ${label}`;
          setTimeout(() => this.assigmentSuccess = null, 10000);
        },
        error: (err) => {
          const msg = err?.error?.message || err?.error || err?.message;
          this.assigmentError = msg ? String(msg) : 'Hiba történt a beosztás módosítása közben.';
          setTimeout(() => this.assigmentError = null, 10000);
        },
      });
  }

  deleteUser(user: UserDto & { selectedRole: string; selectedAssigment: string }): void {
    this.deleteUserError = null;
    this.roleSuccess = null;
    this.modalTargetUser = user;
    this.modalVisible = true;
  }

  confirmDelete(): void {
    const user = this.modalTargetUser;
    if (!user) return;

    this.modalVisible = false;
    this.modalTargetUser = null;

    this.http.delete(`${this.baseUrl}/api/admin/users/${user.id}`).subscribe({
      next: () => {
        this.users = this.users.filter(u => u.id !== user.id);
        this.roleSuccess = `${user.username} sikeresen törölve.`;
        setTimeout(() => this.roleSuccess = null, 10000);
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || err?.message;
        this.deleteUserError = msg ? String(msg) : 'Hiba történt a törlés közben.';
        setTimeout(() => this.deleteUserError = null, 10000);
      },
    });
  }

  cancelDelete(): void {
    this.modalVisible = false;
    this.modalTargetUser = null;
  }

  deleteShiftsForMonth(): void {
    this.deleteSuccess = null;
    this.deleteError = null;

    if (!this.deleteMonth) {
      this.deleteError = 'Kérlek válassz hónapot.';
      return;
    }

    this.http.delete(`${this.baseUrl}/api/admin/shifts/month/${this.deleteMonth}`).subscribe({
      next: () => {
        this.deleteSuccess = `A(z) ${this.deleteMonth} hónap beosztásai sikeresen törölve.`;
        setTimeout(() => this.deleteSuccess = null, 10000);
        this.availableMonths = this.availableMonths.filter(m => m !== this.deleteMonth);
        this.deleteMonth = '';
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || err?.message;
        this.deleteError = msg ? String(msg) : 'Hiba történt a törlés közben.';
        setTimeout(() => this.deleteError = null, 10000);
      },
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
    localStorage.clear();
  }
}
