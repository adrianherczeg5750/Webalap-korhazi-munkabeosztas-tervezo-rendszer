import {AuthService} from './auth';
import {Injectable} from '@angular/core';
import {Router} from '@angular/router';

@Injectable({ providedIn: 'root' })
export class InactivityService {

  private readonly TIMEOUT = 30 * 60 * 1000;
  private logoutTimer: any;

  constructor(private auth: AuthService, private router: Router) {
    this.initListeners();
  }

  private initListeners(): void {
    const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];

    events.forEach(event =>
      window.addEventListener(event, () => this.resetTimer(), { passive: true })
    );

    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) this.resetTimer();
    });
  }

  startTimer(): void {
    if (!this.auth.isLoggedIn()) return;

    this.clearTimer();

    this.logoutTimer = setTimeout(() => {
      this.auth.logout();
      this.clearTimer();
      this.router.navigate(['/login']);
    }, this.TIMEOUT);
  }

  resetTimer(): void {
    if (!this.auth.isLoggedIn()) return;
    this.startTimer();
  }

  clearTimer(): void {
    if (this.logoutTimer) {
      clearTimeout(this.logoutTimer);
      this.logoutTimer = null;
    }
  }

}
