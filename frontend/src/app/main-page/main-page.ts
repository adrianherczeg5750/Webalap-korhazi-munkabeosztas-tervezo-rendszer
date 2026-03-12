import { Component, OnInit } from '@angular/core';
import { DatePipe, NgForOf, NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ShiftService } from '../services/shift.service';
import { ShiftDto } from '../shift/shift.model';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import {environment} from '../../../environments/environment';
import {LeaveRequestService} from '../services/leaveRequest.service';

@Component({
  selector: 'app-main-page',
  standalone: true,
  templateUrl: './main-page.html',
  imports: [DatePipe, NgForOf, NgIf],
  styleUrl: './main-page.css',
})
export class MainPageComponent implements OnInit {
  baseUrl = environment.apiUrl;
  shifts: ShiftDto[] = [];
  monthShifts: any[] = [];

  selectedMonth: Date = new Date();
  monthLabel = '';
  totalWorkHours = '0 óra';

  leaveRequests: any[] = [];
  loggedInUsername: string | null = null;

  constructor(
    private readonly shiftService: ShiftService,
    private readonly leaverequestService: LeaveRequestService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.shiftService.list().subscribe({
      next: (data) => {
        this.shifts = data || [];
        this.rebuildMonthView();
      },
      error: (err) => console.error('Shift list error', err),
    });

    this.loggedInUsername = this.resolveLoggedInUsername();
    console.log('loggedInUsername:', this.loggedInUsername);

    const employeeId = this.getEmployeeId();
    if (employeeId) {
      this.leaverequestService.listByEmployee(employeeId).subscribe({
        next: (data) => (this.leaveRequests = data || []),
        error: (err) => console.error('Leave request list error', err),
      });
    } else {
      this.leaveRequests = [];
    }
  }

  private resolveLoggedInUsername(): string | null {
    const fromStorage = localStorage.getItem('username');
    if (fromStorage) {
      return fromStorage;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      return null;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload?.username || payload?.upn || null;
    } catch (e) {
      console.error('Token parse error', e);
      return null;
    }
  }

  isCurrentUsersShift(s: any): boolean {
    const shiftUsername = s?.user?.username || s?.employeeUsername || s?.username || s?.userName || null;
    return !!this.loggedInUsername && shiftUsername === this.loggedInUsername;
  }

  navigateToShiftAdd(): void {
    this.router.navigate(['/shift-add']);
  }

  navigateToLeaveRequest(): void {
    this.router.navigate(['/leave-request']);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
    localStorage.clear();
  }

  prevMonth(): void {
    const d = new Date(this.selectedMonth);
    d.setMonth(d.getMonth() - 1);
    this.selectedMonth = d;
    this.rebuildMonthView();
  }

  nextMonth(): void {
    const d = new Date(this.selectedMonth);
    d.setMonth(d.getMonth() + 1);
    this.selectedMonth = d;
    this.rebuildMonthView();
  }

  private rebuildMonthView(): void {
    const year = this.selectedMonth.getFullYear();
    const month = this.selectedMonth.getMonth();

    const monthStart = new Date(year, month, 1);
    const monthEnd = new Date(year, month + 1, 1);
    const employleeId = this.getEmployeeId();

    this.monthLabel = new Intl.DateTimeFormat('hu-HU', {
      year: 'numeric',
      month: 'long',
    }).format(monthStart);

    this.monthShifts = (this.shifts || [])
      .map((s: any) => ({
        ...s,
        dateObj: this.parseShiftDate(s.date || s.startAtDate),
      }))
      .filter((s: any) => s.dateObj && s.dateObj >= monthStart && s.dateObj < monthEnd && (s.user.id === employleeId ))
      .sort((a: any, b: any) => (a.dateObj?.getTime?.() || 0) - (b.dateObj?.getTime?.() || 0));

    const hours = this.monthShifts.length * 8;
    this.totalWorkHours = `${hours} óra`;
  }

  toHungarianShiftLabel(shiftType: string | null): string {
    switch (shiftType) {
      case 'MORNING':
      case 'DE':
        return 'Délelőtt';
      case 'AFTERNOON':
      case 'DU':
        return 'Délután';
      case 'NIGHT':
      case 'EJSZAKA':
        return 'Éjszakás';
      default:
        return '—';
    }
  }

  formatLeaveDates(dates: any): string {
    if (!dates) return '—';
    const arr = Array.isArray(dates) ? dates : Array.from(dates);
    if (!arr || arr.length === 0) return '—';

    return arr
      .map((d: any) => {
        const dt = new Date(d);
        return isNaN(dt.getTime()) ? String(d) : dt.toLocaleDateString('hu-HU');
      })
      .join(', ');
  }

  private parseShiftDate(value: any): Date | null {
    if (!value) return null;
    const d = new Date(value);
    return isNaN(d.getTime()) ? null : d;
  }

  private getEmployeeId(): number | null {
    const raw = localStorage.getItem('userId');
    if (!raw) return null;
    const n = Number(raw);
    return Number.isFinite(n) ? n : null;
  }
}
