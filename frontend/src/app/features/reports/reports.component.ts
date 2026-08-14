import { Component, OnInit, inject,ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportService, ReportSummary, CategoryBreakdown, MonthlyTrend } from '../../core/services/report.service';

type SummaryKey = keyof ReportSummary;

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly cdr = inject(ChangeDetectorRef);

  public summary: ReportSummary | null = null;
  public categories: CategoryBreakdown[] = [];
  public trends: MonthlyTrend[] = [];

  public isLoadingSummary = false;
  public isLoadingCategories = false;
  public isLoadingTrends = false;

  public summaryError = '';
  public categoriesError = '';
  public trendsError = '';

  public reportType = 'carbon';
  public dateRange = 'last-30';
  public format = 'pdf';
  public isGenerating = false;
  public downloadLink = '';
  public message = '';

  public readonly summaryKeys: SummaryKey[] = [
    'todayCarbon', 'weeklyCarbon', 'monthlyCarbon',
    'yearlyCarbon', 'avgDailyEmission', 'momReductionPercent',
  ];

  async ngOnInit() {
    await this.loadAllData();
  }

  async loadAllData() {
    await Promise.all([
      this.loadSummary(),
      this.loadCategories(),
      this.loadTrends(),
    ]);
    // Force Angular to redraw the screen once ALL promises finish!
    this.cdr.detectChanges(); 
  }

  async loadSummary() {
    this.isLoadingSummary = true;
    this.summaryError = '';
    try {
      this.summary = await this.reportService.getSummary();
    } catch (err) {
      console.error('Failed to load summary:', err);
      this.summaryError = 'Could not load summary data.';
    } finally {
      this.isLoadingSummary = false;
      this.cdr.detectChanges();
    }
  }

  async loadCategories() {
    this.isLoadingCategories = true;
    this.categoriesError = '';
    try {
      this.categories = await this.reportService.getCategoryBreakdown();
    } catch (err) {
      console.error('Failed to load categories:', err);
      this.categoriesError = 'Could not load category data.';
    } finally {
      this.isLoadingCategories = false;
      this.cdr.detectChanges();
    }
  }

  async loadTrends() {
    this.isLoadingTrends = true;
    this.trendsError = '';
    try {
      this.trends = await this.reportService.getMonthlyTrends();
    } catch (err) {
      console.error('Failed to load trends:', err);
      this.trendsError = 'Could not load trend data.';
    } finally {
      this.isLoadingTrends = false;
      this.cdr.detectChanges();
    }
  }

  public get categoryChartData() {
    if (!this.categories.length) return [];
    const colors: Record<string, string> = {
      Transport: '#10B981',
      Energy: '#3B82F6',
      Food: '#F59E0B',
      Waste: '#EF4444',
    };
    return this.categories.map((c) => ({
      label: c.category,
      value: c.co2Impact,
      percent: c.percentage,
      color: colors[c.category] || '#6B7280',
    }));
  }

  public get trendChartData() {
    if (!this.trends.length) return [];
    const max = Math.max(...this.trends.map((t) => t.co2Impact), 1);
    return this.trends.map((t) => ({
      label: t.month,
      value: t.co2Impact,
      heightPercent: (t.co2Impact / max) * 100,
    }));
  }

  public get hasAnyData(): boolean {
    return !!(this.summary?.monthlyCarbon || this.categories.length || this.trends.length);
  }

  public getSummaryLabel(key: keyof ReportSummary): string {
    const labels: Record<keyof ReportSummary, string> = {
      todayCarbon: 'Today\'s Carbon',
      weeklyCarbon: 'This Week',
      monthlyCarbon: 'This Month',
      yearlyCarbon: 'This Year',
      avgDailyEmission: 'Daily Average',
      momReductionPercent: 'MoM Change',
    };
    return labels[key] || key;
  }

  public getSummaryIcon(key: SummaryKey): string {
    const icons: Record<string, string> = {
      todayCarbon: 'bi-calendar-day',
      weeklyCarbon: 'bi-calendar-week',
      monthlyCarbon: 'bi-calendar-month',
      yearlyCarbon: 'bi-calendar3',
      avgDailyEmission: 'bi-graph-up',
      momReductionPercent: 'bi-arrow-down-up',
    };
    return icons[key] || 'bi-circle';
  }

  public onGenerateReport() {
    this.isGenerating = true;
    this.message = '';
    this.downloadLink = '';

    setTimeout(() => {
      this.isGenerating = false;
      this.downloadLink = 'prepared';
      this.message = `Report data ready for ${this.getReportLabel(this.reportType)}! Click Export to download.`;
    }, 1500);
  }

  public getReportLabel(type: string): string {
    switch (type) {
      case 'carbon': return 'Carbon Footprint Analysis';
      case 'goals': return 'Goals Achievement Report';
      case 'sustainability': return 'General Sustainability Scorecard';
      case 'challenges': return 'Community Challenge Summary';
      default: return 'Custom Activity Report';
    }
  }

  public exportCsv() {
    if (!this.summary && !this.categories.length && !this.trends.length) {
      alert('No data available to export.');
      return;
    }

    const rows: string[] = [];

    rows.push('EcoTrack Report Export');
    rows.push(`Generated,${new Date().toISOString()}`);
    rows.push('');

    rows.push('=== CARBON SUMMARY ===');
    if (this.summary) {
      rows.push(`Metric,Value,Unit`);
      rows.push(`Today's Carbon,${this.summary.todayCarbon},kg CO₂e`);
      rows.push(`Weekly Carbon,${this.summary.weeklyCarbon},kg CO₂e`);
      rows.push(`Monthly Carbon,${this.summary.monthlyCarbon},kg CO₂e`);
      rows.push(`Yearly Carbon,${this.summary.yearlyCarbon},kg CO₂e`);
      rows.push(`Avg Daily Emission,${this.summary.avgDailyEmission},kg CO₂e`);
      rows.push(`MoM Reduction,${this.summary.momReductionPercent},%`);
    } else {
      rows.push('No data available');
    }

    rows.push('');
    rows.push('=== CATEGORY BREAKDOWN (This Month) ===');
    if (this.categories.length) {
      rows.push(`Category,CO₂ Impact (kg),Percentage (%)`);
      for (const c of this.categories) {
        rows.push(`${c.category},${c.co2Impact},${c.percentage}`);
      }
    } else {
      rows.push('No data available');
    }

    rows.push('');
    rows.push('=== MONTHLY TRENDS ===');
    if (this.trends.length) {
      rows.push(`Month,CO₂ Impact (kg CO₂e)`);
      for (const t of this.trends) {
        rows.push(`${t.month},${t.co2Impact}`);
      }
    } else {
      rows.push('No data available');
    }

    const csvContent = rows.map((r) => `"${r}"`).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `ecotrack-report-${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    this.message = 'Report exported as CSV! Check your downloads.';
    this.downloadLink = '';
  }

  public triggerDownload() {
    this.exportCsv();
    this.message = '';
    this.downloadLink = '';
  }

  public formatNumber(n: number | undefined | null): string {
    if (n == null) return '0.0';
    return n.toFixed(1);
  }

  public getStatColor(key: SummaryKey): string {
    const colors: Record<string, string> = {
      todayCarbon: 'success-bg',
      weeklyCarbon: 'info-bg',
      monthlyCarbon: 'warning-bg',
      yearlyCarbon: 'danger-bg',
      avgDailyEmission: 'success-bg',
      momReductionPercent: 'info-bg',
    };
    return colors[key] || 'info-bg';
  }

  public getStatBadgeClass(key: SummaryKey): string {
    const badges: Record<string, string> = {
      todayCarbon: 'success-badge',
      weeklyCarbon: 'info-badge',
      monthlyCarbon: 'warning-badge',
      yearlyCarbon: 'danger-badge',
      avgDailyEmission: 'success-badge',
      momReductionPercent: 'info-badge',
    };
    return badges[key] || 'info-badge';
  }

  public getCategoryArc(): Array<{ color: string; dashArray: string; dashOffset: string }> {
    if (!this.categories.length) return [];
    const circumference = 2 * Math.PI * 40;
    const arcs: Array<{ color: string; dashArray: string; dashOffset: string }> = [];
    let offset = 0;
    const colors: Record<string, string> = {
      Transport: '#10B981',
      Energy: '#3B82F6',
      Food: '#F59E0B',
      Waste: '#EF4444',
    };
    for (const c of this.categories) {
      const arcLength = (c.percentage / 100) * circumference;
      arcs.push({
        color: colors[c.category] || '#6B7280',
        dashArray: `${arcLength} ${circumference - arcLength}`,
        dashOffset: `${-offset}`,
      });
      offset += arcLength;
    }
    return arcs;
  }
}
