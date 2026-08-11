import { 
  Component, 
  OnDestroy, 
  OnInit, 
  inject, 
  ChangeDetectorRef, 
  HostListener 
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { GoalService, GoalResponse } from '../../core/services/goal.service';
import { AuthService } from '../auth/auth.service';

interface Activity {
  id: string;
  icon: string;
  type: string;
  description: string;
  impact: number;
  timestamp: string;
}

interface ChartData {
  labels: string[];
  values: number[];
  period: string;
}

interface DashboardData {
  fullName: string;
  profilePicture: string;
  rewardPoints: number;
  badgeName: string;
  todayCarbon: number;
  monthlyCarbon: number;
  streak: number;
  goalProgress: number;
  ecoScore: number;
  recentActivities: Activity[];
  chartData: ChartData;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly apiService = inject(ApiService);
  private readonly goalService = inject(GoalService);
  public readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  private goalsSubscription?: Subscription;

  public isLoading = true;
  public error: string | null = null;
  public dashboardData: DashboardData | null = null;
  public dashboardGoals: GoalResponse[] = [];
  public aggregateGoalProgress = 0;

  public activeIndivPoint: {
    index: number;
    label: string;
    value: string;
    x: number;
    y: number;
  } | null = null;

  private carbonLogListener = () => this.onCarbonLogUpdated();
  private goalUpdateListener = () => this.onGoalUpdated();
  private profileUpdateListener = () => this.onProfileUpdated();

  ngOnInit(): void {
    // Component initialize hote hi API call hoga
    this.loadDashboardData();

    // Event Listeners attach karna
    window.addEventListener('carbon-log-updated', this.carbonLogListener);
    window.addEventListener('goal-updated', this.goalUpdateListener);
    window.addEventListener('profile-updated', this.profileUpdateListener);

    this.goalsSubscription = this.goalService.goals$.subscribe((goals) => {
      this.dashboardGoals = goals.filter((goal) => !goal.isCompleted).slice(0, 3);
      this.aggregateGoalProgress = this.goalService.aggregateProgressPercent;
      this.cdr.detectChanges();
    });

    this.goalService
      .refreshGoals()
      .catch((err) => console.error('Goal refresh failed:', err));
  }

  @HostListener('window:carbon-log-updated')
  public onCarbonLogUpdated(): void {
    this.loadDashboardData();
    this.goalService
      .refreshGoals()
      .catch((err) => console.error('Goal refresh failed:', err));
  }

  @HostListener('window:goal-updated')
  public onGoalUpdated(): void {
    this.loadDashboardData();
    this.goalService
      .refreshGoals()
      .catch((err) => console.error('Goal refresh failed:', err));
  }

  @HostListener('window:profile-updated')
  public onProfileUpdated(): void {
    // Refresh the authService userProfile signal so navbar/sidebar/dashboard greeting update
    this.authService.getUserProfile().catch((err) => console.error('Profile refresh failed:', err));
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    window.removeEventListener('carbon-log-updated', this.carbonLogListener);
    window.removeEventListener('goal-updated', this.goalUpdateListener);
    window.removeEventListener('profile-updated', this.profileUpdateListener);
    this.goalsSubscription?.unsubscribe();
  }

  private loadDashboardData(): void {
    this.isLoading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.apiService.get<any>('/api/dashboard').subscribe({
      next: (response) => {
        this.dashboardData = response?.data ? response.data : response;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Dashboard API failed:', err);
        this.error = 'Failed to load dashboard data.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  public showIndivTooltip(
    point: { x: number; y: number; label: string; val: string },
    index: number
  ): void {
    this.activeIndivPoint = {
      index,
      label: point.label,
      value: `${point.val} kg CO₂e`,
      x: point.x,
      y: point.y - 12
    };
  }

  public hideIndivTooltip(): void {
    this.activeIndivPoint = null;
  }

  public getPathString(points: Array<{ x: number; y: number }>): string {
    if (points.length === 0) return '';

    let path = `M ${points[0].x} ${points[0].y}`;

    for (let i = 1; i < points.length; i++) {
      const cpX1 = points[i - 1].x + (points[i].x - points[i - 1].x) / 2;
      const cpY1 = points[i - 1].y;
      const cpX2 = points[i - 1].x + (points[i].x - points[i - 1].x) / 2;
      const cpY2 = points[i].y;

      path += ` C ${cpX1} ${cpY1}, ${cpX2} ${cpY2}, ${points[i].x} ${points[i].y}`;
    }

    return path;
  }

  public getAreaPathString(points: Array<{ x: number; y: number }>, height: number): string {
    if (points.length === 0) return '';

    const linePath = this.getPathString(points);

    return `${linePath} L ${points[points.length - 1].x} ${height} L ${points[0].x} ${height} Z`;
  }

  public get chartPoints(): Array<{
    x: number;
    y: number;
    label: string;
    val: string;
  }> {
    if (!this.dashboardData?.chartData?.values?.length) {
      return [];
    }

    const labels = this.dashboardData.chartData.labels;
    const values = this.dashboardData.chartData.values;

    const maxValue = Math.max(...values, 1);
    const minValue = Math.min(...values, 0);

    const range = maxValue - minValue || 1;

    return values.map((val, index) => {
      const x = 20 + index * (260 / Math.max(values.length - 1, 1));
      const normalizedY = range > 0 ? (val - minValue) / range : 0.5;
      const y = 90 - normalizedY * 60;

      return {
        x,
        y,
        label: labels[index] || `Day ${index + 1}`,
        val: val.toFixed(1)
      };
    });
  }
}