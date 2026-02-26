import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, NgForOf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ShiftService } from '../services/shift.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-shift',
  imports: [FormsModule, NgForOf],
  templateUrl: './shift.html',
  styleUrl: './shift.css',
})
export class ShiftComponent {
  roles = ['Nappalos', 'Éjszakás', 'Ügyelet'];
  employeeName = '';
  role = '';
  startAtDate = '';
  startAtTime = '';
  endAtDate = '';
  endAtTime = '';

  constructor(
    private readonly shiftService: ShiftService,
    private readonly router: Router,
    @Inject(PLATFORM_ID) private readonly platformId: Object
  ) {
    if (isPlatformBrowser(this.platformId)) {
      this.employeeName = localStorage.getItem('username') ?? '';
    }
  }

  save() {
    this.shiftService.create({
      employeeName: this.employeeName,
      role: this.role,
      startAtDate: this.startAtDate,
      startAtTime: this.startAtTime,
      endAtDate: this.endAtDate,
      endAtTime: this.endAtTime,
    }).subscribe(() => this.router.navigate(['/main-page']));
  }
}
