import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from '../../../environments/environment';

export type WorkType = 'SINGLE' | 'MULTIPLE';

export interface CreateWorkRequestDto {
  employeeId: number;
  startDate?: string | null;
  endDate?: string | null;
  type: WorkType;
  role?: string;
}

@Injectable({
  providedIn: 'root',
})
export class WorkRequestService {
  private baseUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  create(dto: CreateWorkRequestDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/work-requests/create-request`, dto);
  }

  listByEmployee(employeeId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/work-requests/by-employee/${employeeId}`);
  }

  listAll(): Observable<any>{
    return this.http.get<any[]>(`${this.baseUrl}/api/work-requests/all`);
  }

  approve(id: number, managerId: number) {
    return this.http.post(`${this.baseUrl}/api/work-requests/${id}/approve`, { managerId });
  }

  reject(id: number, managerId: number) {
    return this.http.post(`${this.baseUrl}/api/work-requests/${id}/reject`, { managerId });
  }
}
