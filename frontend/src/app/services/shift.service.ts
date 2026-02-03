import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {ShiftDto} from '../shift/shift.model';

@Injectable({ providedIn: 'root' })
export class ShiftService {
  private readonly baseUrl = 'http://localhost:8080/api/shifts';

  constructor(private http: HttpClient) {}

  list(): Observable<ShiftDto[]> {
    return this.http.get<ShiftDto[]>(this.baseUrl);
  }

  create(payload: Omit<ShiftDto, 'id' | 'workDuration'>): Observable<ShiftDto> {
    return this.http.post<ShiftDto>(this.baseUrl, payload);
  }
}
