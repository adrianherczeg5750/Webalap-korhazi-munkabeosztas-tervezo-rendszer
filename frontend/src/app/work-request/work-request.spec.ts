import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkRequest } from './work-request';

describe('WorkRequest', () => {
  let component: WorkRequest;
  let fixture: ComponentFixture<WorkRequest>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkRequest]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WorkRequest);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
