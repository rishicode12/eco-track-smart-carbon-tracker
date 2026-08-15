import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
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
    this.downloadLink = '';
    this.message = '';
    // Force the button loader to render immediately so it never gets stuck.
    this.cdr.detectChanges();

    setTimeout(() => {
      this.isGenerating = false;
      this.downloadLink = 'ready';
      this.message = `Report data ready for ${this.getReportLabel(this.reportType)}! Click Export to download.`;
      // Prevent the button from staying stuck on "Preparing...".
      this.cdr.detectChanges();
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
      this.message = 'No data available to export yet.';
      this.downloadLink = '';
      this.cdr.detectChanges();
      return;
    }

    const lines: string[] = [];

    lines.push(this.csvLine('EcoTrack Report Export'));
    lines.push(this.csvLine('Report Type', this.getReportLabel(this.reportType)));
    lines.push(this.csvLine('Date Range', this.getDateRangeLabel(this.dateRange)));
    lines.push(this.csvLine('Generated', new Date().toLocaleString()));
    lines.push('');

    lines.push('=== CARBON SUMMARY ===');
    if (this.summary) {
      lines.push(this.csvLine('Metric', 'Value', 'Unit'));
      lines.push(this.csvLine('Today\'s Carbon', this.summary.todayCarbon, 'kg CO2e'));
      lines.push(this.csvLine('Weekly Carbon', this.summary.weeklyCarbon, 'kg CO2e'));
      lines.push(this.csvLine('Monthly Carbon', this.summary.monthlyCarbon, 'kg CO2e'));
      lines.push(this.csvLine('Yearly Carbon', this.summary.yearlyCarbon, 'kg CO2e'));
      lines.push(this.csvLine('Daily Average', this.summary.avgDailyEmission, 'kg CO2e'));
      lines.push(this.csvLine('MoM Reduction', this.summary.momReductionPercent, '%'));
    } else {
      lines.push(this.csvLine('No summary data available'));
    }

    lines.push('');
    lines.push('=== CATEGORY BREAKDOWN (This Month) ===');
    if (this.categories.length) {
      lines.push(this.csvLine('Category', 'CO2 Impact (kg)', 'Percentage (%)'));
      for (const c of this.categories) {
        lines.push(this.csvLine(c.category, c.co2Impact, c.percentage));
      }
    } else {
      lines.push(this.csvLine('No category data available'));
    }

    lines.push('');
    lines.push('=== MONTHLY TRENDS ===');
    if (this.trends.length) {
      lines.push(this.csvLine('Month', 'CO2 Impact (kg)'));
      for (const t of this.trends) {
        lines.push(this.csvLine(t.month, t.co2Impact));
      }
    } else {
      lines.push(this.csvLine('No trend data available'));
    }

    // BOM helps Excel detect UTF-8.
    const csvContent = '\uFEFF' + lines.join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const fileName = `ecotrack-report-${new Date().toISOString().split('T')[0]}.csv`;
    this.downloadBlob(blob, fileName);

    this.message = 'Report exported as CSV! Check your downloads.';
    this.downloadLink = '';
    this.cdr.detectChanges();
  }

  /** Quote/escape a single CSV field; returns a plain value when safe. */
  private csvValue(value: string | number | null | undefined): string {
    const str = value === null || value === undefined ? '' : String(value);
    const escaped = str.replace(/"/g, '""');
    return /[",\n\r]/.test(str) ? `"${escaped}"` : escaped;
  }

  /** Build one comma-separated CSV line with each field sanitized. */
  private csvLine(...fields: Array<string | number | null | undefined>): string {
    return fields.map((f) => this.csvValue(f)).join(',');
  }

  /** Trigger a blob download via a temporary anchor, cleaning up even on failure. */
  private downloadBlob(blob: Blob, fileName: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', fileName);
    try {
      document.body.appendChild(link);
      link.click();
    } finally {
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }
  }

  public getDateRangeLabel(range: string): string {
    switch (range) {
      case 'last-7': return 'Last 7 Days';
      case 'last-30': return 'Last 30 Days';
      case 'last-12m': return 'Last 12 Months';
      case 'ytd': return 'Year-to-date (YTD)';
      default: return 'Custom Range';
    }
  }

  public exportPdf() {
    if (!this.summary && !this.categories.length && !this.trends.length) {
      this.message = 'No data available to export yet.';
      this.downloadLink = '';
      this.cdr.detectChanges();
      return;
    }

    const doc = new jsPDF({ orientation: 'p', unit: 'mm', format: 'a4' });
    const pageWidth = doc.internal.pageSize.getWidth();
    const margin = 14;
    const now = new Date();

    // EcoTrack header banner.
    doc.setFillColor(16, 185, 129);
    doc.rect(0, 0, pageWidth, 22, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.text('EcoTrack Report', margin, 10);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.text(
      `Generated: ${now.toLocaleString()}  |  Report Type: ${this.getReportLabel(this.reportType)}  |  Date Range: ${this.getDateRangeLabel(this.dateRange)}`,
      margin,
      16
    );

    let y = 28;

    // Carbon Footprint Summary.
    doc.setTextColor(30, 41, 59);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text('Carbon Footprint Summary', margin, y);
    y += 4;

    if (this.summary) {
      const summaryBody = this.summaryKeys.map((key) => [
        this.getSummaryLabel(key),
        this.formatNumber(this.summary?.[key]),
        key === 'momReductionPercent' ? '%' : 'kg CO2e',
      ]);
      autoTable(doc, {
        startY: y,
        margin: { left: margin, right: margin },
        head: [['Metric', 'Value', 'Unit']],
        body: summaryBody,
        theme: 'grid',
        headStyles: { fillColor: [16, 185, 129], textColor: [255, 255, 255], fontStyle: 'bold', fontSize: 9 },
        alternateRowStyles: { fillColor: [236, 253, 245] },
        styles: { fontSize: 9, cellPadding: 3, textColor: [30, 41, 59] },
        columnStyles: { 0: { fontStyle: 'bold' } },
      });
      y = ((doc as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY ?? y) + 10;
    } else {
      this.addPdfEmptyNote(doc, y, 'No summary data available.');
      y += 14;
    }

    // Category Breakdown.
    doc.setTextColor(30, 41, 59);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text('Category Breakdown (This Month)', margin, y);
    y += 4;

    if (this.categories.length) {
      const categoryBody = this.categories.map((c) => [
        c.category,
        this.formatNumber(c.co2Impact),
        `${c.percentage}`,
      ]);
      autoTable(doc, {
        startY: y,
        margin: { left: margin, right: margin },
        head: [['Category', 'CO2 Impact (kg)', 'Percentage (%)']],
        body: categoryBody,
        theme: 'grid',
        headStyles: { fillColor: [16, 185, 129], textColor: [255, 255, 255], fontStyle: 'bold', fontSize: 9 },
        alternateRowStyles: { fillColor: [236, 253, 245] },
        styles: { fontSize: 9, cellPadding: 3, textColor: [30, 41, 59] },
        columnStyles: { 0: { fontStyle: 'bold' } },
      });
      y = ((doc as unknown as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY ?? y) + 10;
    } else {
      this.addPdfEmptyNote(doc, y, 'No category data available.');
      y += 14;
    }

    // Monthly Trends.
    doc.setTextColor(30, 41, 59);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text('Monthly Trends', margin, y);
    y += 4;

    if (this.trends.length) {
      const trendBody = this.trends.map((t) => [t.month, this.formatNumber(t.co2Impact)]);
      autoTable(doc, {
        startY: y,
        margin: { left: margin, right: margin },
        head: [['Month', 'CO2 Impact (kg)']],
        body: trendBody,
        theme: 'grid',
        headStyles: { fillColor: [16, 185, 129], textColor: [255, 255, 255], fontStyle: 'bold', fontSize: 9 },
        alternateRowStyles: { fillColor: [236, 253, 245] },
        styles: { fontSize: 9, cellPadding: 3, textColor: [30, 41, 59] },
        columnStyles: { 0: { fontStyle: 'bold' } },
      });
    } else {
      this.addPdfEmptyNote(doc, y, 'No trend data available.');
    }

    doc.save(`ecotrack-report-${now.toISOString().split('T')[0]}.pdf`);

    this.message = 'Report exported as PDF! Check your downloads.';
    this.downloadLink = '';
    this.cdr.detectChanges();
  }

  private addPdfEmptyNote(doc: jsPDF, y: number, text: string) {
    doc.setFont('helvetica', 'italic');
    doc.setFontSize(10);
    doc.setTextColor(107, 114, 128);
    doc.text(text, 14, y);
  }

  public triggerDownload() {
    if (this.format === 'pdf') {
      this.exportPdf();
    } else {
      this.exportCsv();
    }
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
