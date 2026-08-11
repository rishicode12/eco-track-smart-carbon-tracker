import { Injectable, signal, effect, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  // Responsive signals
  public sidebarCollapsed = signal<boolean>(false);
  public mobileSidebarOpen = signal<boolean>(false);
  public theme = signal<'light' | 'dark'>('light');

  constructor() {
    this.initializeTheme();

    // Effect to apply theme class directly to body element whenever the theme signal changes
    effect(() => {
      if (this.isBrowser) {
        const currentTheme = this.theme();
        const body = document.body;
        
        if (currentTheme === 'dark') {
          body.classList.add('dark-theme');
          body.classList.remove('light-theme');
        } else {
          body.classList.add('light-theme');
          body.classList.remove('dark-theme');
        }
        
        localStorage.setItem('ecotrack_theme', currentTheme);
      }
    });
  }

  private initializeTheme(): void {
    if (!this.isBrowser) return;

    const savedTheme = localStorage.getItem('ecotrack_theme') as 'light' | 'dark' | null;
    if (savedTheme === 'light' || savedTheme === 'dark') {
      this.theme.set(savedTheme);
      return;
    }

    // Default to system preference
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.theme.set(prefersDark ? 'dark' : 'light');

    // Watch for system preference changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (!localStorage.getItem('ecotrack_theme')) {
        this.theme.set(e.matches ? 'dark' : 'light');
      }
    });
  }

  public toggleTheme(): void {
    this.theme.update(current => current === 'light' ? 'dark' : 'light');
  }

  public toggleSidebar(): void {
    this.sidebarCollapsed.update(collapsed => !collapsed);
  }

  public toggleMobileSidebar(): void {
    this.mobileSidebarOpen.update(open => !open);
  }

  public closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }
}
