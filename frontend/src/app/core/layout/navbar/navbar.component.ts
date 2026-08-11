import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // <-- 1. NAYA IMPORT (ngIf aur ngClass ke liye)

// 👇 Agar in dono lines par red error aaye, toh inko delete karke Ctrl + Space se Auto-Import karein

import { AuthService } from '../../../features/auth/auth.service'; 
import { LayoutService } from '../../services/layout.service';

@Component({
  selector: 'app-navbar',
  standalone: true, // <-- 2. Standalone declare kiya
  imports: [CommonModule], // <-- 3. CommonModule add kiya taaki HTML errors hat jayein
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'] 
})
export class NavbarComponent implements OnInit {
  
  // Services inject ki hain
  public layoutService = inject(LayoutService);
  public authService = inject(AuthService);

  public unreadNotificationsCount = 1; // Dummy notification count

  // API Call - Jaise hi Navbar load ho, real data aa jaye
  async ngOnInit() {
    try {
      await this.authService.getUserProfile(); 
    } catch (error) {
      console.error('Navbar error:', error);
    }
  }
}