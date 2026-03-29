import { Component, OnInit } from '@angular/core';
import { DatePipe, NgForOf, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShiftService } from '../services/shift.service';
import { LeaveRequestService } from '../services/leaveRequest.service';
import { WorkRequestService } from '../services/workRequest.service';
import { AuthService } from '../services/auth';
import { Router } from '@angular/router';
import {ShiftDto} from '../shift/shift.model';

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
  workRequests: any[] = [];
  selectedMonthAndYear: Date = new Date();
  monthLabel = '';
  shifts: ShiftDto[] = [];
  monthShifts: any[] = [];

  constructor(
    private readonly router: Router,
    private readonly auth: AuthService,
    private readonly leaverequestService: LeaveRequestService,
    private readonly workRequestService: WorkRequestService,
    private readonly shiftService: ShiftService,
  ) {}

  ngOnInit(): void {
    this.shiftService.list().subscribe({
      next: (data) => {
        this.shifts = data || [];
        this.rebuildMonthView();
      },
      error: (err) => console.error('Shift list error', err),
    });

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

    this.workRequestService.listAll().subscribe({
      next: (data) => (this.workRequests = data || []),
      error: (err) => console.error('Work request list error', err),
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

    const year = this.selectedMonthAndYear.getFullYear();
    const month = this.selectedMonthAndYear.getMonth();

    const monthStart = new Date(year, month, 1);
    const monthEnd = new Date(year, month + 1, 1);
    this.monthShifts = (this.shifts || [])
      .map((s: any) => ({
        ...s,
        dateObj: this.parseShiftDate(s.date || s.startAtDate),
      }))
      .filter((s: any) => s.dateObj && s.dateObj >= monthStart && s.dateObj < monthEnd)
      .sort((a: any, b: any) => (a.dateObj?.getTime?.() || 0) - (b.dateObj?.getTime?.() || 0));
  }


  toDisplayEmployee(r: any): string {
    return r?.employeeUsername || '—';
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

  get filteredWorkRequests(): any[] {
    return (this.workRequests || []).filter((r: any) => {
      const startDate = this.parseDate(r?.startDate);
      if (!startDate) return false;
      return (
        startDate.getFullYear() === this.selectedMonthAndYear.getFullYear() &&
        startDate.getMonth() === this.selectedMonthAndYear.getMonth()
      );
    });
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

  approveWorkRequest(r: any): void {
    const managerId = localStorage.getItem('userId');

    this.workRequestService.approve(r.id, Number(managerId)).subscribe({
      next: (updated: any) => {
        r.status = updated.status;
      },
      error: (err) => console.error('Work request approve error', err),
    });
  }

  rejectWorkRequest(r: any): void {
    const managerId = localStorage.getItem('userId');

    this.workRequestService.reject(r.id, Number(managerId)).subscribe({
      next: (updated: any) => {
        r.status = updated.status;
      },
      error: (err) => console.error('Work request reject error', err),
    });
  }
  generateScheduleForMonth(): void {
    const month = `${this.selectedMonthAndYear.getFullYear()}-${String(this.selectedMonthAndYear.getMonth() + 1).padStart(2, '0')}`;

    this.shiftService.generateForMonth(month).subscribe({
      next: () => {
        this.shiftService.list().subscribe({
          next: (data) => {
            this.shifts = data || [];
            this.rebuildMonthView();
          },
          error: (err) => console.error('Shift list reload error', err),
        });
      },
      error: (err) => console.error('Schedule generation error', err),
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
    localStorage.clear();
  }

  private parseShiftDate(value: any): Date | null {
    if (!value) return null;
    const d = new Date(value);
    return isNaN(d.getTime()) ? null : d;
  }
}
