import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../auth/auth.service'; 
import { COUNTRIES } from '../auth/countries'; 

interface Interest {
  id: string;
  name: string;
  selected: boolean;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  
  private authService = inject(AuthService);
  private http = inject(HttpClient);
  // YAHAN CHANGE DETECTION INJECT KIYA HAI JAISE DATA TURANT DIKHE
  private cdr = inject(ChangeDetectorRef); 

  public fullname = '';
  public email = '';
  public rewardPoints = 0;
  
  public profilePicture: string | null = null; 
  public isUploading = false;
  
  public role = '';
  public location = '';
  public commuteMode = '';
  public dietPreference = '';
  public country = '';
  public countries = COUNTRIES;

  // NAYA VARIABLE: Form ko lock/unlock karne ke liye
  public isEditing = false; 

  public interests: Interest[] = [
    { id: 'renewable', name: 'Renewable Energy', selected: true },
    { id: 'recycling', name: 'Recycling & Waste reduction', selected: true },
    { id: 'sustainable', name: 'Sustainable Living', selected: false },
    { id: 'transport', name: 'Green Transportation', selected: true },
    { id: 'water', name: 'Water Conservation', selected: false },
    { id: 'products', name: 'Eco-Friendly Products', selected: true },
    { id: 'organic', name: 'Organic Farming', selected: false }
  ];

  public badges = [
    { name: 'Eco Warrior', icon: 'bi-gem', unlocked: true, desc: 'Earned by completing 5 challenges.' },
    { name: 'Climate Hero', icon: 'bi-patch-check', unlocked: true, desc: 'Earned by saving over 500kg CO₂.' },
    { name: 'Planet Protector', icon: 'bi-shield-fill-check', unlocked: false, desc: 'Reach level 15 to unlock.' }
  ];

  public message = '';

  async ngOnInit() {
    try {
      const userProfile: any = await this.authService.getUserProfile();
      
      this.fullname = userProfile.fullName || '';
      this.email = userProfile.email || '';
      this.rewardPoints = userProfile.rewardPoints || 0;
      
      this.role = userProfile.role || '';
      this.location = userProfile.location || '';
      this.commuteMode = userProfile.commuteMode || '';
      this.dietPreference = userProfile.dietPreference || '';
      this.country = userProfile.country || '';
      
      this.profilePicture = userProfile.profilePicture; 

      // Parse interests from JSON string
      if (userProfile.interests) {
        try {
          const parsedInterests: string[] = JSON.parse(userProfile.interests);
          this.interests = this.interests.map(item => ({
            ...item,
            selected: parsedInterests.includes(item.id)
          }));
        } catch (e) {
          console.warn('Could not parse interests:', e);
        }
      }

      // ANGULAR KO FORCE UPDATE KARNE KE LIYE (Yeh problem 1 solve karega)
      this.cdr.detectChanges();

    } catch (error) {
      console.error('Failed to load profile data:', error);
    }
  }

  // Edit Button click hone par form unlock hoga
  public toggleEdit() {
    this.isEditing = true;
  }

  // Cancel Button click hone par form wapas lock hoga aur purana data aayega
  public cancelEdit() {
    this.isEditing = false;
    this.ngOnInit(); // Purana data wapas database se reload kar lega
  }

  public onSaveSettings() {
    const selectedInterests = this.interests.filter(i => i.selected).map(i => i.id);

    const updatedData = {
      fullName: this.fullname,
      role: this.role,
      location: this.location,
      commuteMode: this.commuteMode,
      dietPreference: this.dietPreference,
      country: this.country,
      interests: JSON.stringify(selectedInterests)
    };

    const token = localStorage.getItem('ecotrack_token'); 
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    this.http.put('http://localhost:8080/api/users/update-profile', updatedData, { headers })
      .subscribe({
        next: async (response: any) => {
          this.message = 'Profile permanently saved to database!';
          this.isEditing = false;
          // Refresh the global userProfile signal
          await this.authService.getUserProfile();
          this.cdr.detectChanges();
          setTimeout(() => { this.message = ''; this.cdr.detectChanges(); }, 3000);
        },
        error: (err) => {
          console.error("Failed to save profile", err);
          alert("Could not save settings. Please try again.");
        }
      });
  }

  public onFileSelected(event: any) {
    const file: File = event.target.files[0];
    
    if (file) {
      this.isUploading = true;
      const formData = new FormData();
      formData.append('file', file);

      const token = localStorage.getItem('ecotrack_token'); 
      const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

      this.http.post('http://localhost:8080/api/users/profile-picture', formData, { headers })
        .subscribe({
          next: (response: any) => {
            alert('Profile picture uploaded to Cloudinary successfully!');
            this.isUploading = false;
            window.location.reload(); 
          },
          error: (err) => {
            console.error('Upload Failed:', err);
            alert('Upload failed. Check console for details.');
            this.isUploading = false;
          }
        });
    }
  }

  public currentPassword = '';
  public newPassword = '';
  public confirmNewPassword = '';
  public changePasswordMessage = '';
  public changePasswordError = '';

  public onChangePassword() {
    this.changePasswordError = '';
    this.changePasswordMessage = '';

    if (!this.currentPassword || !this.newPassword || !this.confirmNewPassword) {
      this.changePasswordError = 'All password fields are required.';
      return;
    }

    if (this.newPassword !== this.confirmNewPassword) {
      this.changePasswordError = 'New password and confirm password do not match.';
      return;
    }

    if (this.newPassword.length < 6) {
      this.changePasswordError = 'New password must be at least 6 characters.';
      return;
    }

    const token = localStorage.getItem('ecotrack_token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    const body = {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    };

    this.http.put('http://localhost:8080/api/users/change-password', body, { headers })
      .subscribe({
        next: async (response: any) => {
          this.changePasswordMessage = 'Password changed successfully!';
          this.currentPassword = '';
          this.newPassword = '';
          this.confirmNewPassword = '';
          await this.authService.getUserProfile();
          this.cdr.detectChanges();
          setTimeout(() => {
            this.changePasswordMessage = '';
            this.cdr.detectChanges();
          }, 4000);
        },
        error: (err) => {
          console.error('Change password failed:', err);
          this.changePasswordError = err.error?.message || 'Failed to change password. Please check your current password.';
          this.cdr.detectChanges();
        }
      });
  }
}