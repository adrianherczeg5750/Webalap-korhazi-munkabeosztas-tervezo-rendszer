import { Component, OnInit } from '@angular/core';
import { DatePipe, NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShiftService } from '../services/shift.service';
import { LeaveRequestService } from '../services/leaveRequest.service';
import { AuthService } from '../services/auth';
import { Router } from '@angular/router';

@Component({
  selector: 'app-manager',
  imports: [
    DatePipe,
    NgForOf,
    NgIf,
    FormsModule,
  ],
  templateUrl: './manager.html',
  styleUrl: './manager.css',
})
export class ManagerComponent implements OnInit {
  leaveRequests: any[] = [];
  selectedMonthAndYear: Date = new Date();
  monthLabel = '';

  constructor(
    private readonly router: Router,
    private readonly auth: AuthService,
    private readonly leaverequestService: LeaveRequestService,
    private readonly shiftService: ShiftService,
  ) {}

  ngOnInit(): void {
    this.rebuildMonthView();
    this.leaverequestService.listAll().subscribe({
      next: (data) => {
        console.log('leaveRequests:', data);
        if (Array.isArray(data) && data.length > 0) {
          console.log('first leaveRequest:', data[0]);
          console.log('first leaveRequest employee:', data[0]?.employee);
        }
        this.leaveRequests = data || [];
      },
      error: (err) => console.error('Leave request list error', err),
    });
  }
  prevMonth(): void {
    const d = new Date(this.selectedMonthAndYear);
    d.setMonth(d.getMonth() - 1);
    this.selectedMonthAndYear = d;
    this.rebuildMonthView();
  }

  nextMonth(): void {
    const d = new Date(this.selectedMonthAndYear);
    d.setMonth(d.getMonth() + 1);
    this.selectedMonthAndYear = d;
    this.rebuildMonthView();
  }

  private rebuildMonthView(): void {
    this.monthLabel = new Intl.DateTimeFormat('hu-HU', {
      year: 'numeric',
      month: 'long',
    }).format(this.selectedMonthAndYear);
  }

  formatLeaveDates(dates: any): string {
    if (!dates) return '—';
    if (Array.isArray(dates)) {
      return dates.length ? dates.join(', ') : '—';
    }
    if (typeof dates === 'string') {
      return dates.trim().length ? dates : '—';
    }
    return '—';
  }

  toDisplayEmployee(r: any): string {
    return r?.employeeUsername || '—';
  }

  get filteredLeaveRequests(): any[] {
    return (this.leaveRequests || []).filter((r: any) => {
      const startDate = this.parseDate(r?.startDate);
      if (!startDate) return false;

      return (
        startDate.getFullYear() === this.selectedMonthAndYear.getFullYear() &&
        startDate.getMonth() === this.selectedMonthAndYear.getMonth()
      );
    });
  }

  private parseDate(value: any): Date | null {
    if (!value) return null;

    const parsed = new Date(value);
    if (!isNaN(parsed.getTime())) {
      return parsed;
    }

    const text = String(value).trim();
    const huMatch = text.match(/^(\d{4})\.(\d{2})\.(\d{2})/);
    if (huMatch) {
      const year = Number(huMatch[1]);
      const month = Number(huMatch[2]) - 1;
      const day = Number(huMatch[3]);
      const date = new Date(year, month, day);
      return isNaN(date.getTime()) ? null : date;
    }

    return null;
  }

  approve(r: any): void {
    const managerId = localStorage.getItem('userId');

    this.leaverequestService.approve(r.id, Number(managerId)).subscribe({
      next: (updated: any) => {
        r.status = updated.status;
      },
      error: (err) => console.error('Approve error', err),
    });
  }

  reject(r: any): void {
    const managerId = localStorage.getItem('userId');

    this.leaverequestService.reject(r.id, Number(managerId)).subscribe({
      next: (updated: any) => {
        r.status = updated.status;
      },
      error: (err) => console.error('Reject error', err),
    });
  }
  generateScheduleForMonth(): void {
    const month = `${this.selectedMonthAndYear.getFullYear()}-${String(this.selectedMonthAndYear.getMonth() + 1).padStart(2, '0')}`;

    this.shiftService.generateForMonth(month).subscribe({
      next: () => {
        console.log('Schedule generated for month:', month);
      },
      error: (err) => console.error('Schedule generation error', err),
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
    localStorage.clear();
  }
}
