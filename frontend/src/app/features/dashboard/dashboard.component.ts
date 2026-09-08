import { 
  Component, 
  OnDestroy, 
  OnInit, 
  AfterViewInit,
  inject, 
  ChangeDetectorRef, 
  HostListener,
  ViewChild,
  ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { trigger, transition, style, animate, query, stagger } from '@angular/animations';
import { Subscription } from 'rxjs';
import { Chart, registerables } from 'chart.js';

import { ApiService } from '../../core/services/api.service';
import { GoalService, GoalResponse } from '../../core/services/goal.service';
import { AuthService } from '../auth/auth.service';
import { EcoGlobeComponent } from './components/eco-globe/eco-globe.component';

Chart.register(...registerables);

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
  imports: [CommonModule, EcoGlobeComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  animations: [
    trigger('dashboardStagger', [
      transition(':enter', [
        query('.stagger-globe', [
          style({ opacity: 0, transform: 'scale(0.95)' }),
          animate('600ms cubic-bezier(0.35, 0, 0.25, 1)', style({ opacity: 1, transform: 'scale(1)' }))
        ], { optional: true }),
        query('.stagger-kpi', [
          style({ opacity: 0, transform: 'translateY(24px)' }),
          stagger(100, [
            animate('500ms cubic-bezier(0.35, 0, 0.25, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
          ])
        ], { optional: true }),
        query('.stagger-bottom', [
          style({ opacity: 0, transform: 'translateY(24px)' }),
          stagger(150, [
            animate('500ms cubic-bezier(0.35, 0, 0.25, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
          ])
        ], { optional: true })
      ])
    ])
  ]
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('barChartCanvas') barChartCanvas!: ElementRef<HTMLCanvasElement>;

  private readonly apiService = inject(ApiService);
  private readonly goalService = inject(GoalService);
  public readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  private goalsSubscription?: Subscription;
  private chartInstance: Chart | null = null;

  public isLoading = true;
  public error: string | null = null;
  public dashboardData: DashboardData | null = null;
  public dashboardGoals: GoalResponse[] = [];
  public aggregateGoalProgress = 0;

  private carbonLogListener = () => this.onCarbonLogUpdated();
  private goalUpdateListener = () => this.onGoalUpdated();
  private profileUpdateListener = () => this.onProfileUpdated();

  ngOnInit(): void {
    this.loadDashboardData();

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

  ngAfterViewInit(): void {
    if (this.dashboardData) {
      this.renderBarChart();
    }
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
    this.authService.getUserProfile().catch((err) => console.error('Profile refresh failed:', err));
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    window.removeEventListener('carbon-log-updated', this.carbonLogListener);
    window.removeEventListener('goal-updated', this.goalUpdateListener);
    window.removeEventListener('profile-updated', this.profileUpdateListener);
    this.goalsSubscription?.unsubscribe();
    if (this.chartInstance) {
      this.chartInstance.destroy();
    }
  }

  private loadDashboardData(): void {
    this.isLoading = true;
    this.error = null;
    this.cdr.detectChanges();

    this.apiService.get<any>('/dashboard').subscribe({
      next: (response) => {
        this.dashboardData = response?.data ? response.data : response;
        this.isLoading = false;
        this.cdr.detectChanges();
        setTimeout(() => this.renderBarChart(), 50);
      },
      error: (err) => {
        console.error('Dashboard API failed:', err);
        this.error = 'Failed to load dashboard data.';
        this.isLoading = false;
        this.cdr.detectChanges();
        setTimeout(() => this.renderBarChart(), 50);
      },
      complete: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private renderBarChart(): void {
    if (!this.barChartCanvas?.nativeElement) return;

    if (this.chartInstance) {
      this.chartInstance.destroy();
      this.chartInstance = null;
    }

    const ctx = this.barChartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const labels = this.dashboardData?.chartData?.labels?.length
      ? this.dashboardData.chartData.labels
      : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    const values = this.dashboardData?.chartData?.values?.length
      ? this.dashboardData.chartData.values
      : [2.4, 3.8, 1.9, 4.2, 2.7, 1.5, 3.1];

    const gradient = ctx.createLinearGradient(0, 0, 0, 220);
    gradient.addColorStop(0, 'rgba(16, 185, 129, 0.9)');
    gradient.addColorStop(1, 'rgba(5, 150, 105, 0.15)');

    this.chartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Emissions (kg CO₂e)',
            data: values,
            backgroundColor: gradient,
            borderColor: '#10b981',
            borderWidth: 1.5,
            borderRadius: 6,
            borderSkipped: false,
            hoverBackgroundColor: '#34d399'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            titleColor: '#ecfdf5',
            bodyColor: '#10b981',
            borderColor: 'rgba(16, 185, 129, 0.4)',
            borderWidth: 1,
            padding: 10,
            displayColors: false,
            callbacks: {
              label: (context) => `${context.parsed.y} kg CO₂e`
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#94a3b8', font: { size: 11, weight: 'bold' } }
          },
          y: {
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: { color: '#94a3b8', font: { size: 11 } },
            beginAtZero: true
          }
        }
      }
    });
  }

  public get top3Activities(): Activity[] {
    if (!this.dashboardData?.recentActivities) return [];
    return this.dashboardData.recentActivities.slice(0, 3);
  }

  /**
   * Helper method to map an activity's category to standardized keys:
   * 'food' | 'energy' | 'transport' | 'waste' | 'general'
   */
  public getActivityCategory(activity: Activity): 'food' | 'energy' | 'transport' | 'waste' | 'general' {
    const rawType = (activity.type || '').toUpperCase();
    const rawDesc = (activity.description || '').toUpperCase();

    if (rawType.includes('FOOD') || rawDesc.includes('FOOD') || rawDesc.includes('DIET') || rawDesc.includes('MEAL')) {
      return 'food';
    }
    if (rawType.includes('ENERGY') || rawType.includes('ELECTRIC') || rawDesc.includes('ENERGY') || rawDesc.includes('ELECTRIC') || rawDesc.includes('POWER') || rawDesc.includes('GAS')) {
      return 'energy';
    }
    if (rawType.includes('TRANSPORT') || rawType.includes('COMMUTE') || rawDesc.includes('TRANSPORT') || rawDesc.includes('COMMUTE') || rawDesc.includes('CAR') || rawDesc.includes('DRIVE') || rawDesc.includes('FLIGHT')) {
      return 'transport';
    }
    if (rawType.includes('WASTE') || rawDesc.includes('WASTE') || rawDesc.includes('GARBAGE') || rawDesc.includes('TRASH') || rawDesc.includes('RECYCLE')) {
      return 'waste';
    }
    return 'general';
  }

  /**
   * Returns matching CSS modifier class for the icon container based on category
   * (e.g. 'act-icon-food', 'act-icon-energy', 'act-icon-transport', 'act-icon-waste')
   */
  public getActivityIconClass(activity: Activity): string {
    const cat = this.getActivityCategory(activity);
    return `act-icon-${cat}`;
  }

  /**
   * Returns matching CSS modifier class for the emission impact badge
   */
  public getActivityBadgeClass(activity: Activity): string {
    const cat = this.getActivityCategory(activity);
    return `act-badge-${cat}`;
  }

  /**
   * Returns an appropriate Bootstrap Icon if one is not already set
   */
  public getActivityIcon(activity: Activity): string {
    if (activity.icon && activity.icon.trim().length > 0 && !activity.icon.includes('bi-calculator')) {
      return activity.icon;
    }
    const cat = this.getActivityCategory(activity);
    switch (cat) {
      case 'food':
        return 'bi-egg-fried';
      case 'energy':
        return 'bi-lightning-charge-fill';
      case 'transport':
        return 'bi-car-front-fill';
      case 'waste':
        return 'bi-trash3-fill';
      default:
        return 'bi-activity';
    }
  }
}