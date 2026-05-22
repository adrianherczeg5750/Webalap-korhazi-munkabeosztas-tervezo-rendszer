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

  create(payload: Omit<ShiftDto, 'id'>): Observable<ShiftDto> {
    return this.http.post<ShiftDto>(this.baseUrl, payload);
  }

  generateForMonth(month: string, staffPerShift: number, generatorName: string): Observable<void>{
    return this.http.post<void>(`${this.baseUrl}/generate`, { month, staffPerShift, generatorName });
  }

  listGenerators(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/generators`);
  }

  regeneratePartial(month: string, from: number, to: number, staffPerShift: number, generatorName: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/regenerate-partial`, { month, from, to, staffPerShift, generatorName });
  }
}
