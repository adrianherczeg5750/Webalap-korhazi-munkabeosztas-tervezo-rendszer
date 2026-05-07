import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WorkRequestService, CreateWorkRequestDto } from './workRequest.service';
import { environment } from '../../../environments/environment';

describe('WorkRequestService', () => {
  let service: WorkRequestService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(WorkRequestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send POST to create work request', () => {
    const dto: CreateWorkRequestDto = {
      employeeId: 1,
      startDate: '2026-05-10',
      endDate: '2026-05-10',
      type: 'SINGLE',
      role: 'MORNING',
    };

    service.create(dto).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/work-requests/create-request`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush({});
  });

  it('should GET work requests by employee', () => {
    service.listByEmployee(7).subscribe((data) => {
      expect(data.length).toBe(2);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/work-requests/by-employee/7`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1 }, { id: 2 }]);
  });

  it('should GET all work requests', () => {
    service.listAll().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/work-requests/all`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should POST to approve work request', () => {
    service.approve(3, 8).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/work-requests/3/approve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ managerId: 8 });
    req.flush({});
  });

  it('should POST to reject work request', () => {
    service.reject(3, 8).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/api/work-requests/3/reject`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ managerId: 8 });
    req.flush({});
  });
});