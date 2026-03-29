import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from '../../../environments/environment';

export type LeaveType = 'PAID' | 'UNPAID';

export interface CreateLeaveRequestDto {
  employeeId: number;
  startDate?: string | null;
  endDate?: string | null;
  type: LeaveType;
}

@Injectable({
  providedIn: 'root',
})
export class LeaveRequestService {
  private baseUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  create(dto: CreateLeaveRequestDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/leave-requests/create-request`, dto);
  }

  listByEmployee(employeeId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/leave-requests/by-employee/${employeeId}`);
  }

  listAll(): Observable<any>{
    return this.http.get<any[]>(`${this.baseUrl}/api/leave-requests/all`);
  }

  approve(id: number, managerId: number) {
    return this.http.post(`${this.baseUrl}/api/leave-requests/${id}/approve`, { managerId });
  }

  reject(id: number, managerId: number) {
    return this.http.post(`${this.baseUrl}/api/leave-requests/${id}/reject`, { managerId });
  }
}
