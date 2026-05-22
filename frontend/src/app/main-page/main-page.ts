import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ShiftService } from '../services/shift.service';
import { ShiftDto } from '../shift/shift.model';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import {environment} from '../../../environments/environment';
import {LeaveRequestService} from '../services/leaveRequest.service';
import {WorkRequestService} from '../services/workRequest.service';

@Component({
  selector: 'app-main-page',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './main-page.html',
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
  workRequests: any[] = [];
  loggedInUsername: string | null = null;

  constructor(
    private readonly shiftService: ShiftService,
    private readonly leaverequestService: LeaveRequestService,
    private readonly workRequestService: WorkRequestService,
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
      this.workRequestService.listByEmployee(employeeId).subscribe({
        next: (data) => (this.workRequests = data || []),
        error: (err) => console.error('Work request list error', err),
      });
    } else {
      this.leaveRequests = [];
      this.workRequests = [];
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

  navigateToLeaveRequest(): void {
    this.router.navigate(['/leave-request']);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
    localStorage.clear();
  }

  get filteredWorkRequests(): any[] {
    return (this.workRequests || []).filter((r: any) => {
      const startDate = this.parseDate(r?.startDate);
      if (!startDate) return false;
      return (
        startDate.getFullYear() === this.selectedMonth.getFullYear() &&
        startDate.getMonth() === this.selectedMonth.getMonth()
      );
    });
  }

  get filteredLeaveRequests(): any[] {
    return (this.leaveRequests || []).filter((r: any) => {
      const startDate = this.parseDate(r?.startDate);
      if (!startDate) return false;

      return (
        startDate.getFullYear() === this.selectedMonth.getFullYear() &&
        startDate.getMonth() === this.selectedMonth.getMonth()
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
        return 'Délelőtt';
      case 'AFTERNOON':
        return 'Délután';
      case 'NIGHT':
        return 'Éjszakás';
      default:
        return '—';
    }
  }

  shiftHours(shiftType: string): string {
    switch (shiftType) {
      case 'MORNING':
        return '00:00 - 08:00';
      case 'AFTERNOON':
        return '08:00 - 16:00';
      case 'NIGHT':
        return '16:00 - 23:59';
      default:
        return '—';
    }
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

  navigateToWorkRequest() {
    this.router.navigate(['/work-request']);
  }

  get ganttDays(): number[] {
    const year = this.selectedMonth.getFullYear();
    const month = this.selectedMonth.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    return Array.from({ length: daysInMonth }, (_, i) => i + 1);
  }

  ganttShiftType(day: number): string | null {
    const year = this.selectedMonth.getFullYear();
    const month = this.selectedMonth.getMonth();
    const dayStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

    for (const s of this.monthShifts) {
      const dateStr = (s.date || s.startAtDate || '').substring(0, 10);
      if (dateStr === dayStr) {
        return s.shiftType;
      }
    }
    return null;
  }

  ganttCellClass(day: number): string {
    const type = this.ganttShiftType(day);
    if (!type) return 'gantt-cell';
    return 'gantt-cell gantt-' + type.toLowerCase();
  }

  ganttCellLabel(day: number): string {
    const type = this.ganttShiftType(day);
    switch (type) {
      case 'MORNING': return 'D';
      case 'AFTERNOON': return 'DU';
      case 'NIGHT': return 'É';
      default: return '';
    }
  }
}
