import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgForOf, NgIf } from '@angular/common';
import { Router } from '@angular/router';

import { LeaveRequestService, LeaveType } from '../services/leaveRequest.service';

@Component({
  selector: 'app-leave-request',
  standalone: true,
  imports: [FormsModule, NgForOf, NgIf],
  templateUrl: './leave-request.html',
  styleUrl: './leave-request.css',
})
export class LeaveRequestComponent {
  startDate: string = '';
  endDate: string = '';
  type: LeaveType = 'VACATION';

  saving = false;
  errorMsg: string | null = null;
  successMsg: string | null = null;

  leaveTypes: Array<{ value: LeaveType; label: string }> = [
    { value: 'VACATION', label: 'Szabadság' },
    { value: 'SICK', label: 'Táppénz / Betegszabadság' },
    { value: 'UNPAID', label: 'Fizetés nélküli' },
  ];

  constructor(
    private readonly leaveRequestService: LeaveRequestService,
    private readonly router: Router,
  ) {}

  submit(): void {
    this.errorMsg = null;
    this.successMsg = null;

    const employeeId = this.getEmployeeId();
    if (employeeId == null) {
      this.errorMsg = 'Nem található a bejelentkezett felhasználó azonosítója (userId). Jelentkezz be újra.';
      return;
    }

    if (!this.startDate || !this.endDate) {
      this.errorMsg = 'A kezdő és záró dátum megadása kötelező.';
      return;
    }

    if (this.endDate < this.startDate) {
      this.errorMsg = 'A befejezés dátuma nem lehet korábbi, mint a kezdés dátuma.';
      return;
    }

    this.saving = true;

    this.leaveRequestService
      .create({
        employeeId,
        startDate: this.startDate,
        endDate: this.endDate,
        type: this.type,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.successMsg = 'Szabadságkérelem sikeresen benyújtva.';

          setTimeout(() => this.router.navigate(['/main-page']), 400);
        },
        error: (err) => {
          this.saving = false;
          const msg = err?.error?.message || err?.error || err?.message;
          this.errorMsg = msg ? String(msg) : 'Hiba történt a kérelem mentése közben.';
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/main-page']);
  }

  private getEmployeeId(): number | null {
    const raw = localStorage.getItem('userId');
    if (!raw) return null;
    const n = Number(raw);
    return Number.isFinite(n) ? n : null;
  }
}
