import { Component, OnInit } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
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
    DecimalPipe,
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
  staffPerShift = 2;
  generateModalVisible = false;
  generationMethod = '';
  generationMethods: { value: string; label: string }[] = [];
  generateError: string = '';
  generateSuccess: string = '';
  partialModalVisible = false;
  partialFrom = 1;
  partialTo = 1;

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

  get isCurrentMonthGenerated(): boolean{
    return this.monthShifts.length > 0;
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
  openGenerateModal(): void {
    this.loadGenerationMethods();
    this.generateModalVisible = true;
  }

  cancelGenerate(): void {
    this.generateModalVisible = false;
  }

  confirmGenerate(): void {
    this.generateModalVisible = false;

    const month = `${this.selectedMonthAndYear.getFullYear()}-${String(this.selectedMonthAndYear.getMonth() + 1).padStart(2, '0')}`;

    const generatorName = this.generationMethod.replace(/\.[^.]+$/, '');
    this.shiftService.generateForMonth(month, this.staffPerShift, generatorName).subscribe({
      next: () => {
        this.generateSuccess = 'Beosztás sikeresen legenerálva!';
        setTimeout(() => this.generateSuccess = '', 10000);
        this.shiftService.list().subscribe({
          next: (data) => {
            this.shifts = data || [];
            this.rebuildMonthView();
          },
          error: (err) => console.error('Shift list reload error', err),
        });
      },
      error: (err) => {
        console.error('Schedule generation error', err);
        this.generateError = 'Nem sikerült a beosztás generálása!';
        setTimeout(() => this.generateError = '', 10000);
      }
    });
  }

  get ganttDays(): number[] {
    const year = this.selectedMonthAndYear.getFullYear();
    const month = this.selectedMonthAndYear.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    return Array.from({ length: daysInMonth }, (_, i) => i + 1);
  }

  get ganttEmployees(): string[] {
    const names = new Set<string>();
    for (const s of this.monthShifts) {
      const name = s.user?.username || s.employeeUsername || s.employeeName || s.username || s.userName;
      if (name) names.add(name);
    }
    return Array.from(names).sort();
  }

  ganttShiftType(employee: string, day: number): string | null {
    const year = this.selectedMonthAndYear.getFullYear();
    const month = this.selectedMonthAndYear.getMonth();
    const dayStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

    for (const s of this.monthShifts) {
      const name = s.user?.username || s.employeeUsername || s.employeeName || s.username || s.userName;
      const dateStr = (s.date || s.startAtDate || '').substring(0, 10);
      if (name === employee && dateStr === dayStr) {
        return s.shiftType;
      }
    }
    return null;
  }

  ganttCellClass(employee: string, day: number): string {
    const type = this.ganttShiftType(employee, day);
    if (!type) return 'gantt-cell';
    return 'gantt-cell gantt-' + type.toLowerCase();
  }

  ganttCellLabel(employee: string, day: number): string {
    const type = this.ganttShiftType(employee, day);
    switch (type) {
      case 'MORNING': return 'D';
      case 'AFTERNOON': return 'DU';
      case 'NIGHT': return 'É';
      default: return '';
    }
  }

  ganttTotalHours(employee: string): number {
    let hours = 0;
    for (const s of this.monthShifts) {
      const name = s.user?.username || s.employeeUsername || s.employeeName || s.username || s.userName;
      if (name === employee && s.shiftType) {
        hours += 8;
      }
    }
    return hours;
  }

  get daysInMonth(): number {
    const year = this.selectedMonthAndYear.getFullYear();
    const month = this.selectedMonthAndYear.getMonth();
    return new Date(year, month + 1, 0).getDate();
  }

  get detectedStaffPerShift(): number {
    const counts: number[] = [];
    for (const day of this.ganttDays) {
      const year = this.selectedMonthAndYear.getFullYear();
      const month = this.selectedMonthAndYear.getMonth();
      const dayStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      for (const type of ['MORNING', 'AFTERNOON', 'NIGHT']) {
        let count = 0;
        for (const s of this.monthShifts) {
          const dateStr = (s.date || s.startAtDate || '').substring(0, 10);
          if (dateStr === dayStr && s.shiftType === type) count++;
        }
        if (count > 0) counts.push(count);
      }
    }
    if (counts.length === 0) return this.staffPerShift;
    return Math.round(counts.reduce((a, b) => a + b, 0) / counts.length);
  }

  get averageHoursPerEmployee(): number {
    const employees = this.ganttEmployees.length;
    if (employees === 0) return 0;
    const totalShifts = this.daysInMonth * 3 * this.detectedStaffPerShift;
    return Math.round((totalShifts * 8) / employees);
  }

  get hoursLowerBound(): number {
    return Math.round((this.averageHoursPerEmployee * 0.75) / 8) * 8;
  }

  get hoursUpperBound(): number {
    return Math.round((this.averageHoursPerEmployee * 1.25) / 8) * 8;
  }

  hoursStatus(employee: string): 'ok' | 'warning' | 'border' {
    const hours = this.ganttTotalHours(employee);
    const lower = Math.round(this.hoursLowerBound);
    const upper = Math.round(this.hoursUpperBound);
    if (hours === lower || hours === upper) return 'border';
    if (hours > lower && hours < upper) return 'ok';
    return 'warning';
  }

  get maxEmployeeHours(): number {
    let max = 0;
    for (const emp of this.ganttEmployees) {
      const h = this.ganttTotalHours(emp);
      if (h > max) max = h;
    }
    const upper = this.hoursUpperBound;
    if (upper > max) max = upper;
    return max || 1;
  }

  get hoursChartTicks(): number[] {
    const max = this.maxEmployeeHours;
    const step = max <= 80 ? 8 : 16;
    const ticks: number[] = [];
    for (let i = 0; i <= max; i += step) {
      ticks.push(i);
    }
    if (ticks[ticks.length - 1] < max) {
      ticks.push(max);
    }
    return ticks;
  }

  hoursBarWidth(employee: string): number {
    return (this.ganttTotalHours(employee) / this.maxEmployeeHours) * 100;
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

  openPartialModal(): void {
    this.loadGenerationMethods();
    const daysInMonth = new Date(this.selectedMonthAndYear.getFullYear(), this.selectedMonthAndYear.getMonth() + 1, 0).getDate();
    this.partialFrom = 1;
    this.partialTo = daysInMonth;
    this.partialModalVisible = true;
  }

  cancelPartial(): void {
    this.partialModalVisible = false;
  }

  confirmPartial(): void {
    this.partialModalVisible = false;

    const month = `${this.selectedMonthAndYear.getFullYear()}-${String(this.selectedMonthAndYear.getMonth() + 1).padStart(2, '0')}`;
    const generatorName = this.generationMethod.replace(/\.[^.]+$/, '');

    this.shiftService.regeneratePartial(month, this.partialFrom, this.partialTo, this.staffPerShift, generatorName).subscribe({
      next: () => {
        this.generateSuccess = `Beosztás sikeresen újragenerálva (${this.partialFrom}. - ${this.partialTo}. nap)!`;
        setTimeout(() => this.generateSuccess = '', 10000);
        this.shiftService.list().subscribe({
          next: (data) => {
            this.shifts = data || [];
            this.rebuildMonthView();
          },
          error: (err) => console.error('Shift list reload error', err),
        });
      },
      error: (err) => {
        console.error('Partial regeneration error', err);
        this.generateError = 'Nem sikerült a részleges újragenerálás!';
        setTimeout(() => this.generateError = '', 10000);
      }
    });
  }

  loadGenerationMethods(): void {
    this.shiftService.listGenerators().subscribe({
      next: (files) => {
        this.generationMethods = (files || []).map(f => ({
          value: f,
          label: f.replace(/\.[^.]+$/, ''),
        }));
        if (this.generationMethods.length > 0) {
          this.generationMethod = this.generationMethods[0].value;
        }
      },
      error: (err) => console.error('Generator list error', err),
    });
  }
}
