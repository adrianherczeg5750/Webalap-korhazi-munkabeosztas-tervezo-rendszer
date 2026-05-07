import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ShiftService } from './shift.service';

describe('ShiftService', () => {
  let service: ShiftService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/shifts';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ShiftService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET all shifts', () => {
    const mockShifts = [
      { id: 1, employeeName: 'Teszt', shiftType: 'MORNING' },
    ];

    service.list().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].employeeName).toBe('Teszt');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockShifts);
  });

  it('should POST to create shift', () => {
    const payload = {
      employeeName: 'Teszt',
      role: 'EMPLOYEE',
      startAtDate: '2026-05-10',
      startAtTime: '08:00',
      endAtDate: '2026-05-10',
      endAtTime: '16:00',
    };

    service.create(payload).subscribe();

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({});
  });

  it('should POST to generate shifts for month', () => {
    service.generateForMonth('2026-05', 3, 'DefaultGenerator').subscribe();

    const req = httpMock.expectOne(`${baseUrl}/generate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      month: '2026-05',
      staffPerShift: 3,
      generatorName: 'DefaultGenerator',
    });
    req.flush({});
  });

  it('should GET generator list', () => {
    service.listGenerators().subscribe((data) => {
      expect(data).toEqual(['DefaultGenerator.java', 'random_gen.py']);
    });

    const req = httpMock.expectOne(`${baseUrl}/generators`);
    expect(req.request.method).toBe('GET');
    req.flush(['DefaultGenerator.java', 'random_gen.py']);
  });
});