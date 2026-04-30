import { Component } from '@angular/core';
import {FormsModule} from '@angular/forms';
import {WorkRequestService} from '../services/workRequest.service';
import {Router} from '@angular/router';
import {WorkType} from '../services/workRequest.service';

@Component({
  selector: 'app-work-request',
  imports: [
    FormsModule
  ],
  templateUrl: './work-request.html',
  styleUrl: './work-request.css',
})
export class WorkRequest {
  startDate: string = '';
  endDate: string = '';
  type: WorkType = 'SINGLE';
  role: string = '';

  saving = false;
  errorMsg: string | null = null;
  successMsg: string | null = null;

  workTypes: Array<{ value: WorkType; label: string }> = [
    { value: 'MULTIPLE', label: 'Több napot felölelő' },
    { value: 'SINGLE', label: 'Egy napos' },

  ];

  constructor(
    private readonly workRequestService: WorkRequestService,
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

    if (!this.startDate) {
      this.errorMsg = 'A dátum megadása kötelező.';
      return;
    }

    if (this.type === 'MULTIPLE') {
      if (!this.endDate) {
        this.errorMsg = 'A kezdő és záró dátum megadása kötelező.';
        return;
      }

      if (this.endDate < this.startDate) {
        this.errorMsg = 'A befejezés dátuma nem lehet korábbi, mint a kezdés dátuma.';
        return;
      }
    } else {
      this.endDate = this.startDate;
    }

    this.saving = true;

    this.workRequestService
      .create({
        employeeId,
        startDate: this.startDate,
        endDate: this.endDate,
        type: this.type,
        role: this.role
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.successMsg = 'Munkavégzési kérelem sikeresen benyújtva.';

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
