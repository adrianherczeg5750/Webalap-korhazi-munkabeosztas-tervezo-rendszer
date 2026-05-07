import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { WorkRequest } from './work-request';
import { WorkRequestService } from '../services/workRequest.service';

describe('WorkRequestComponent', () => {
  let component: WorkRequest;
  let fixture: ComponentFixture<WorkRequest>;
  let workService: jasmine.SpyObj<WorkRequestService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    workService = jasmine.createSpyObj('WorkRequestService', ['create']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [WorkRequest, HttpClientTestingModule],
      providers: [
        { provide: WorkRequestService, useValue: workService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkRequest);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show error when userId is missing', () => {
    component.startDate = '2026-05-10';

    component.submit();

    expect(component.errorMsg).toContain('Nem található');
    expect(workService.create).not.toHaveBeenCalled();
  });

  it('should show error when startDate is empty', () => {
    localStorage.setItem('userId', '1');

    component.submit();

    expect(component.errorMsg).toBe('A dátum megadása kötelező.');
  });

  it('should show error for MULTIPLE type when endDate is empty', () => {
    localStorage.setItem('userId', '1');
    component.type = 'MULTIPLE';
    component.startDate = '2026-05-10';

    component.submit();

    expect(component.errorMsg).toBe('A kezdő és záró dátum megadása kötelező.');
  });

  it('should show error for MULTIPLE type when endDate is before startDate', () => {
    localStorage.setItem('userId', '1');
    component.type = 'MULTIPLE';
    component.startDate = '2026-05-10';
    component.endDate = '2026-05-05';

    component.submit();

    expect(component.errorMsg).toBe('A befejezés dátuma nem lehet korábbi, mint a kezdés dátuma.');
  });

  it('should set endDate to startDate for SINGLE type', () => {
    localStorage.setItem('userId', '1');
    workService.create.and.returnValue(of({}));

    component.type = 'SINGLE';
    component.startDate = '2026-05-10';
    component.role = 'MORNING';

    component.submit();

    expect(workService.create).toHaveBeenCalledWith({
      employeeId: 1,
      startDate: '2026-05-10',
      endDate: '2026-05-10',
      type: 'SINGLE',
      role: 'MORNING',
    });
  });

  it('should call create with correct data for MULTIPLE type', () => {
    localStorage.setItem('userId', '5');
    workService.create.and.returnValue(of({}));

    component.type = 'MULTIPLE';
    component.startDate = '2026-05-10';
    component.endDate = '2026-05-15';
    component.role = 'NIGHT';

    component.submit();

    expect(workService.create).toHaveBeenCalledWith({
      employeeId: 5,
      startDate: '2026-05-10',
      endDate: '2026-05-15',
      type: 'MULTIPLE',
      role: 'NIGHT',
    });
    expect(component.successMsg).toBe('Munkavégzési kérelem sikeresen benyújtva.');
  });

  it('should show error message on API failure', () => {
    localStorage.setItem('userId', '1');
    workService.create.and.returnValue(throwError(() => ({ error: 'Error' })));

    component.type = 'SINGLE';
    component.startDate = '2026-05-10';

    component.submit();

    expect(component.errorMsg).toBeTruthy();
    expect(component.saving).toBeFalse();
  });

  it('should navigate to main-page on cancel', () => {
    component.cancel();

    expect(router.navigate).toHaveBeenCalledWith(['/main-page']);
  });
});
