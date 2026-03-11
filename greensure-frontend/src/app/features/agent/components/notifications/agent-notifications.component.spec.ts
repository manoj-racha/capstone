import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AgentNotificationsComponent } from './agent-notifications.component';

describe('AgentNotificationsComponent', () => {
  let component: AgentNotificationsComponent;
  let fixture: ComponentFixture<AgentNotificationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentNotificationsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AgentNotificationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
