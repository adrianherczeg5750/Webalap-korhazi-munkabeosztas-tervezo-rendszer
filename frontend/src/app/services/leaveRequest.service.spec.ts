import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { LeaveRequestService, CreateLeaveRequestDto } from './leaveRequest.service';
import { environment } from '../../../environments/environment';

describe('LeaveRequestService', () => {
  let service: LeaveRequestService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(LeaveRequestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send POST to create leave request', () => {
    const dto: CreateLeaveRequestDto = {
      employeeId: 1,
      startDate: '2026-05-01',
      endDate: '2026-05-05',
      type: 'PAID',
    };

    service.create(dto).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/leave-requests/create-request`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush({});
  });

  it('should GET leave requests by employee', () => {
    service.listByEmployee(42).subscribe((data) => {
      expect(data.length).toBe(1);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/leave-requests/by-employee/42`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1 }]);
  });

  it('should GET all leave requests', () => {
    service.listAll().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/leave-requests/all`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should POST to approve leave request', () => {
    service.approve(5, 10).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/leave-requests/5/approve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ managerId: 10 });
    req.flush({});
  });

  it('should POST to reject leave request', () => {
    service.reject(5, 10).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/leave-requests/5/reject`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ managerId: 10 });
    req.flush({});
  });
});