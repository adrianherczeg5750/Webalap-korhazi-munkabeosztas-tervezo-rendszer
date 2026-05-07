import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LeaveRequestComponent } from './leave-request';
import { LeaveRequestService } from '../services/leaveRequest.service';

describe('LeaveRequestComponent', () => {
  let component: LeaveRequestComponent;
  let fixture: ComponentFixture<LeaveRequestComponent>;
  let leaveService: jasmine.SpyObj<LeaveRequestService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    leaveService = jasmine.createSpyObj('LeaveRequestService', ['create']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LeaveRequestComponent, HttpClientTestingModule],
      providers: [
        { provide: LeaveRequestService, useValue: leaveService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeaveRequestComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show error when userId is missing', () => {
    component.startDate = '2026-05-01';
    component.endDate = '2026-05-05';

    component.submit();

    expect(component.errorMsg).toContain('Nem található');
    expect(leaveService.create).not.toHaveBeenCalled();
  });

  it('should show error when dates are empty', () => {
    localStorage.setItem('userId', '1');

    component.submit();

    expect(component.errorMsg).toBe('A kezdő és záró dátum megadása kötelező.');
  });

  it('should show error when endDate is before startDate', () => {
    localStorage.setItem('userId', '1');
    component.startDate = '2026-05-10';
    component.endDate = '2026-05-05';

    component.submit();

    expect(component.errorMsg).toBe('A befejezés dátuma nem lehet korábbi, mint a kezdés dátuma.');
  });

  it('should call create with correct data on valid submit', () => {
    localStorage.setItem('userId', '42');
    leaveService.create.and.returnValue(of({}));

    component.startDate = '2026-05-01';
    component.endDate = '2026-05-05';
    component.type = 'PAID';

    component.submit();

    expect(leaveService.create).toHaveBeenCalledWith({
      employeeId: 42,
      startDate: '2026-05-01',
      endDate: '2026-05-05',
      type: 'PAID',
    });
    expect(component.successMsg).toBe('Szabadságkérelem sikeresen benyújtva.');
  });

  it('should show error message on API failure', () => {
    localStorage.setItem('userId', '1');
    leaveService.create.and.returnValue(throwError(() => ({ error: 'Conflict' })));

    component.startDate = '2026-05-01';
    component.endDate = '2026-05-05';

    component.submit();

    expect(component.errorMsg).toBeTruthy();
    expect(component.saving).toBeFalse();
  });

  it('should navigate to main-page on cancel', () => {
    component.cancel();

    expect(router.navigate).toHaveBeenCalledWith(['/main-page']);
  });
});