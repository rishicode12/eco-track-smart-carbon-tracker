import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AdminService,
  AdminUser,
  AdminChallenge,
  AdminChallengeCreate,
} from '../../core/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  public activeTab: 'users' | 'challenges' = 'users';

  public users: AdminUser[] = [];
  public isLoadingUsers = false;
  public usersError = '';

  public challenges: AdminChallenge[] = [];
  public isLoadingChallenges = false;
  public challengesError = '';

  public form = {
    title: '',
    description: '',
    challengeType: 'WEEKLY' as 'DAILY' | 'WEEKLY',
    rewardPoints: 100,
    badgeName: '',
    category: 'Energy',
    targetGoal: 100,
    durationDays: 30,
  };

  public isCreating = false;
  public createMessage = '';
  public createError = '';

  public async ngOnInit(): Promise<void> {
    await Promise.all([this.loadUsers(), this.loadChallenges()]);
  }

  public async loadUsers(): Promise<void> {
    this.isLoadingUsers = true;
    this.usersError = '';
    try {
      this.users = await this.adminService.getUsers();
    } catch (error) {
      console.error('Failed to load users', error);
      this.usersError = 'Could not load users.';
    } finally {
      this.isLoadingUsers = false;
    }
  }

  public async loadChallenges(): Promise<void> {
    this.isLoadingChallenges = true;
    this.challengesError = '';
    try {
      this.challenges = await this.adminService.getChallenges();
    } catch (error) {
      console.error('Failed to load challenges', error);
      this.challengesError = 'Could not load challenges.';
    } finally {
      this.isLoadingChallenges = false;
    }
  }

  public async onCreateChallenge(): Promise<void> {
    if (!this.form.title.trim() || !this.form.description.trim() || !this.form.badgeName.trim()) {
      this.createError = 'Title, description, and badge name are required.';
      this.createMessage = '';
      return;
    }

    this.isCreating = true;
    this.createMessage = '';
    this.createError = '';

    const start = new Date();
    const end = new Date();
    end.setDate(end.getDate() + (Number(this.form.durationDays) || 0));

    const payload: AdminChallengeCreate = {
      title: this.form.title.trim(),
      description: this.form.description.trim(),
      challengeType: this.form.challengeType,
      rewardPoints: Number(this.form.rewardPoints),
      badgeName: this.form.badgeName.trim(),
      category: this.form.category,
      targetGoal: Number(this.form.targetGoal) || undefined,
      startDate: this.toDateString(start),
      endDate: this.toDateString(end),
    };

    try {
      await this.adminService.createChallenge(payload);
      this.createMessage = 'Challenge created successfully!';
      this.form.title = '';
      this.form.description = '';
      this.form.badgeName = '';
      await this.loadChallenges();
    } catch (error) {
      console.error('Failed to create challenge', error);
      this.createError = 'Could not create challenge.';
    } finally {
      this.isCreating = false;
    }
  }

  public async onDeleteChallenge(id: number): Promise<void> {
    if (!window.confirm('Delete this challenge?')) {
      return;
    }
    this.createError = '';
    this.createMessage = '';
    try {
      await this.adminService.deleteChallenge(id);
      this.createMessage = 'Challenge deleted successfully.';
      await this.loadChallenges();
    } catch (error) {
      console.error('Failed to delete challenge', error);
      this.createError = 'Could not delete challenge.';
    }
  }

  private toDateString(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}