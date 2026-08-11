import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { AuthService } from '../../../features/auth/auth.service';
import { LayoutService } from '../../services/layout.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  

  public layoutService = inject(LayoutService);
  public authService = inject(AuthService);

  public navItems = [
    { route: '/dashboard', label: 'Dashboard', icon: 'bi-grid' },
    { route: '/carbon', label: 'Carbon Tracker', icon: 'bi-calculator' },
    { route: '/goals', label: 'Goals', icon: 'bi-bullseye' },
    { route: '/ai', label: 'AI Assistant', icon: 'bi-robot' },
    { route: '/challenges', label: 'Challenges', icon: 'bi-trophy' },
    { route: '/reports', label: 'Reports', icon: 'bi-bar-chart' },
    { route: '/profile', label: 'Profile', icon: 'bi-person' }
  ];

  async ngOnInit() {
    try {
      await this.authService.getUserProfile();
    } catch (error) {
      console.error('Sidebar error:', error);
    }
  }

}