import {Component, OnInit} from '@angular/core';
import {DatePipe, NgForOf, NgIf} from '@angular/common';
import {ShiftService} from '../services/shift.service';
import {ShiftDto} from '../shift/shift.model';
import { CommonModule } from '@angular/common';
import {Router, RouterModule} from '@angular/router';

@Component({
  selector: 'app-main-page',
  standalone: true,
  templateUrl: './main-page.html',
  imports: [
    DatePipe,
    NgForOf,
    NgIf
  ],
  styleUrl: './main-page.css'
})
export class MainPageComponent implements OnInit {
  shifts: ShiftDto[] = [];

  constructor(private readonly shiftService: ShiftService, private readonly router: Router) {}

  ngOnInit(): void {
    this.shiftService.list().subscribe({
      next: (data) => this.shifts = data,
      error: (err) => console.error('Shift list error', err),
    });
  }
  navigateToShiftAdd(): void {
    this.router.navigate(['/shift-add']);
  }
}
