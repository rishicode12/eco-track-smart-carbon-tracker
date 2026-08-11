import {
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Subscription } from 'rxjs';

import {
  GoalService,
  GoalResponse
} from '../../core/services/goal.service';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './goals.component.html',
  styleUrls: ['./goals.component.css']
})
export class GoalsComponent implements OnInit, OnDestroy {

  private readonly goalService = inject(GoalService);
  private readonly fb = inject(FormBuilder);
  private goalsSubscription?: Subscription;

  public goals: GoalResponse[] = [];
  public isLoading = false;
  public isSubmitting = false;
  public errorMessage: string | null = null;
  public editingGoalId: number | null = null;

  public goalForm = this.fb.group({
    title: [
      '',
      [Validators.required, Validators.maxLength(150)]
    ],
    targetCarbonReduction: [
      10,
      [Validators.required, Validators.min(0.1)]
    ],
    deadline: [
      '',
      Validators.required
    ]
  });

  ngOnInit(): void {
    // 1. Subscribe to reactive goals stream
    this.goalsSubscription = this.goalService.goals$.subscribe((goals) => {
      this.goals = goals;
    });

    // 2. Fetch data from backend
    this.loadGoals();
  }

  ngOnDestroy(): void {
    this.goalsSubscription?.unsubscribe();
  }

  // FIXED: loadGoals method
  
  async loadGoals() {
    this.isLoading = true;
    this.errorMessage = '';

    try {
      // FIX: 'getGoals()' ki jagah 'refreshGoals()' use kiya gaya hai
      this.goals = await this.goalService.refreshGoals(); 
      
    } catch (error: any) {
      console.error('Error loading goals:', error);
      this.errorMessage = 'Failed to load goals. Please try again.';
      
    } finally {
      this.isLoading = false; 
    }
  }

  async onSubmitGoal(): Promise<void> {
    if (this.goalForm.invalid || this.isSubmitting) {
      this.goalForm.markAllAsTouched();
      return;
    }

    const { title, targetCarbonReduction, deadline } = this.goalForm.getRawValue();

    this.isSubmitting = true;
    this.errorMessage = null;

    try {
      const request = {
        title: title!.trim(),
        targetCarbonReduction: Number(targetCarbonReduction),
        deadline: deadline!
      };

      if (this.editingGoalId !== null) {
        await this.goalService.updateGoal(this.editingGoalId, request);
      } else {
        await this.goalService.createGoal(request);
      }

      this.resetForm();

      // UI update ke liye wapas load karein agar service me auto-update nahi hai
      await this.loadGoals();

    } catch (error) {
      console.error('Failed to save goal', error);
      this.errorMessage = this.editingGoalId !== null
        ? 'Could not update the goal. Please try again.'
        : 'Could not create the goal. Please try again.';
    } finally {
      this.isSubmitting = false;
    }
  }

  public onEditGoal(goal: GoalResponse): void {
    this.editingGoalId = goal.id;

    this.goalForm.patchValue({
      title: goal.title,
      targetCarbonReduction: goal.targetCarbonReduction,
      deadline: goal.deadline
    });

    this.errorMessage = null;

    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  public cancelEdit(): void {
    this.resetForm();
  }

  private resetForm(): void {
    this.editingGoalId = null;
    this.goalForm.reset({
      title: '',
      targetCarbonReduction: 10,
      deadline: ''
    });
  }

  async onCompleteGoal(goalId: number): Promise<void> {
    try {
      await this.goalService.completeGoal(goalId);
    } catch (error) {
      console.error('Failed to complete goal', error);
      this.errorMessage = 'Could not complete the goal. Please try again.';
    }
  }

  async onDeleteGoal(goalId: number): Promise<void> {
    try {
      await this.goalService.deleteGoal(goalId);
    } catch (error) {
      console.error('Failed to delete goal', error);
      this.errorMessage = 'Could not delete the goal. Please try again.';
    }
  }

  public progressPercent(goal: GoalResponse): number {
    return Math.min(
      100,
      Math.max(0, goal.progressPercent ?? 0)
    );
  }

  public isCompleted(goal: GoalResponse): boolean {
    return goal.isCompleted || this.progressPercent(goal) >= 100;
  }
}