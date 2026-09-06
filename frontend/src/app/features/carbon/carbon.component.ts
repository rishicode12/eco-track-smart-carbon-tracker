import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  CarbonService,
  CarbonLogRequest,
  CarbonLogResponse
} from '../../core/services/carbon.service';
import { GamificationService } from '../../core/services/gamification.service';

@Component({
  selector: 'app-carbon',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './carbon.component.html',
  styleUrls: ['./carbon.component.css']
})
export class CarbonComponent implements OnInit {

  private readonly carbonService = inject(CarbonService);
  private readonly gamificationService = inject(GamificationService);
  private readonly cdr = inject(ChangeDetectorRef);

  public selectedCategory = 'transport';

  public isLoading = false;
  public isSaving = false;

  public loadError: string | null = null;
  public saveError: string | null = null;

  public transportType = 'car-petrol';
  public distance = 0;

  public electricityKwh = 0;
  public heatingGas = 0;

  public dietMeals = 1;
  public dietType = 'meat';

  public wasteBags = 0;
  public wasteRecycleRate = 50;

  public waterConsumed = 0;
  public waterUnit = 'Liters';

  public calculatedEmissions = 0;

  public get predictedXp(): number {
    // 1. Agar input hi nahi hai toh 0 XP
    if (!this.hasValidInput()) {
      return 0;
    }

    // 2. Green choices ke liye flat 50 XP
    if (this.isGreenChoice()) {
      return 50;
    }

    // 3. UI me jo impact display hota hai use directly read ya calculate karo
    let currentImpact = 0;
    
    // Agar component me already impact variable/method exist karta hai
    const self = this as any;
    if (typeof self.calculatedImpact === 'number') {
      currentImpact = self.calculatedImpact;
    } else if (typeof self.co2Impact === 'number') {
      currentImpact = self.co2Impact;
    } else if (typeof self.calculateImpact === 'function') {
      currentImpact = Number(self.calculateImpact()) || 0;
    } else if (typeof self.getCalculatedImpact === 'function') {
      currentImpact = Number(self.getCalculatedImpact()) || 0;
    } else {
      // Direct fallback: input values se live impact nikaalo
      if (this.selectedCategory === 'transport') {
        currentImpact = (Number(this.distance) || 0) * 0.35; // 1000 miles = 350 kg
      } else if (this.selectedCategory === 'energy') {
        currentImpact = ((Number(this.electricityKwh) || 0) * 0.4) + ((Number(this.heatingGas) || 0) * 0.2);
      } else if (this.selectedCategory === 'waste') {
        const rate = Number(this.wasteRecycleRate) || 0;
        currentImpact = (Number(this.wasteBags) || 0) * (1 - rate / 100) * 2.5;
      }
    }

    // Tier based matching (Jaise aapko chahiye tha)
    if (currentImpact > 500) return 200; // Max cap
    if (currentImpact > 200) return 100; // 350.00 kg yahan aayega -> +100 XP
    if (currentImpact > 100) return 50;  // 104.65 kg yahan aayega -> +50 XP
    if (currentImpact > 50)  return 30;
    if (currentImpact > 0)   return 20;

    return 0;
  }

  private hasValidInput(): boolean {
    switch (this.selectedCategory) {
      case 'transport': return (Number(this.distance) || 0) > 0;
      case 'energy':    return (Number(this.electricityKwh) || 0) > 0 || (Number(this.heatingGas) || 0) > 0;
      case 'food':      return (Number(this.dietMeals) || 0) > 0;
      case 'waste':     return (Number(this.wasteBags) || 0) > 0;
      case 'water':     return (Number(this.waterConsumed) || 0) > 0;
      default:          return false;
    }
  }

  private isGreenChoice(): boolean {
    const transportGreen = ['public-bus', 'public-train', 'walk', 'bike', 'cycle'];
    const foodGreen = ['vegetarian', 'vegan'];
    const wasteGreen = Number(this.wasteRecycleRate) === 100;

    switch (this.selectedCategory) {
      case 'transport':
        return transportGreen.some(k => (this.transportType || '').toLowerCase().includes(k));
      case 'food':
        return foodGreen.some(k => (this.dietType || '').toLowerCase().includes(k));
      case 'waste':
        return wasteGreen;
      case 'energy':
        return (Number(this.electricityKwh) || 0) === 0 && (Number(this.heatingGas) || 0) === 0;
      case 'water':
        return (Number(this.waterConsumed) || 0) === 0;
      default:
        return false;
    }
  }
  public activityLogs: CarbonLogResponse[] = [];

  ngOnInit(): void {
    this.loadUserLogs();
    this.calculateCurrent();
  }

  public async loadUserLogs(): Promise<void> {
    this.isLoading = true;
    this.loadError = null;

    try {
      this.activityLogs = await this.carbonService.getLogs();
    } catch (error) {
      console.error('Error fetching carbon activities:', error);
      this.loadError = 'Unable to load your carbon activities. Please try again.';
    } finally {
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }

  public setCategory(category: string): void {
    this.selectedCategory = category;
    this.calculatedEmissions = 0;
    this.calculateCurrent();
  }

  public calculateCurrent(): void {
    let result = 0;

    if (this.selectedCategory === 'transport') {
      let factor = 0;
      switch (this.transportType) {
        case 'car-petrol':
          factor = 0.35;
          break;
        case 'car-ev':
          factor = 0.12;
          break;
        case 'public-bus':
          factor = 0.08;
          break;
        case 'public-train':
          factor = 0.05;
          break;
      }
      result = this.distance * factor;
    } else if (this.selectedCategory === 'energy') {
      result = (this.electricityKwh * 0.39) + (this.heatingGas * 0.18);
    } else if (this.selectedCategory === 'food') {
      let mealFactor = 0;
      switch (this.dietType) {
        case 'meat':
          mealFactor = 2.5;
          break;
        case 'vegetarian':
          mealFactor = 0.8;
          break;
        case 'vegan':
          mealFactor = 0.3;
          break;
      }
      result = this.dietMeals * mealFactor;
    } else if (this.selectedCategory === 'waste') {
      result = this.wasteBags * 2.5 * (1 - this.wasteRecycleRate / 100);
    } else if (this.selectedCategory === 'water') {
      const factor = this.waterUnit === 'Gallons' ? 0.000344 * 3.785 : 0.000344;
      result = this.waterConsumed * factor;
    }

    this.calculatedEmissions = Number(result.toFixed(4));
  }

  public async onLogActivity(): Promise<void> {
    if (this.calculatedEmissions <= 0 || this.isSaving) {
      return;
    }

    this.isSaving = true;
    this.saveError = null;

    const request: CarbonLogRequest = {
      activityCategory: this.getCategoryLabel(this.selectedCategory),
      co2Impact: this.calculatedEmissions,
      description: this.buildDescription(),
      waterConsumed: this.selectedCategory === 'water' ? this.waterConsumed : undefined,
      waterUnit: this.selectedCategory === 'water' ? this.waterUnit : undefined
    };

    try {
      await this.carbonService.addLog(request);
      await this.loadUserLogs();
      this.resetCalculator();

      // Notify gamification subscribers (navbar, sidebar) to refresh XP/level
      this.gamificationService.notifyProfileUpdated();

    } catch (error) {
      console.error('Error saving carbon activity:', error);
      this.saveError = 'Unable to save the activity. Please try again.';
      this.cdr.detectChanges();

    } finally {
      this.isSaving = false;
      this.cdr.detectChanges();
    }
  }

  public async deleteLog(id: number): Promise<void> {
    const confirmed = window.confirm('Are you sure you want to delete this activity?');
    if (!confirmed) {
      return;
    }

    try {
      await this.carbonService.deleteLog(id);
      await this.loadUserLogs();

      this.gamificationService.notifyProfileUpdated();

    } catch (error) {
      console.error('Error deleting carbon activity:', error);
      this.loadError = 'Unable to delete the activity. Please try again.';
    } finally {
      this.cdr.detectChanges();
    }
  }

  private getCategoryLabel(category: string): string {
    switch (category) {
      case 'transport':
        return 'Transport';
      case 'energy':
        return 'Energy';
      case 'food':
        return 'Food';
      case 'waste':
        return 'Waste';
      case 'water':
        return 'Water';
      default:
        return 'Other';
    }
  }

  private buildDescription(): string {
    switch (this.selectedCategory) {
      case 'transport':
        return `Commute by ${this.transportType.replace('-', ' ')} (${this.distance} mi)`;
      case 'energy':
        return `Electricity ${this.electricityKwh} kWh, heating ${this.heatingGas} units`;
      case 'food':
        return `${this.dietType} diet - ${this.dietMeals} meal(s)`;
      case 'waste':
        return `Household waste (${this.wasteBags} bags, ${this.wasteRecycleRate}% recycled)`;
      case 'water':
        return `${this.waterConsumed} ${this.waterUnit} consumed`;
      default:
        return 'Carbon activity';
    }
  }

  private resetCalculator(): void {
    this.distance = 0;
    this.electricityKwh = 0;
    this.heatingGas = 0;
    this.dietMeals = 1;
    this.dietType = 'meat';
    this.wasteBags = 0;
    this.wasteRecycleRate = 50;
    this.waterConsumed = 0;
    this.waterUnit = 'Liters';
    this.calculatedEmissions = 0;
  }

  public getCategoryIcon(log: CarbonLogResponse): string {
    const text = `${log.activityCategory} ${log.description ?? ''}`.toLowerCase();

    if (text.includes('transport') || text.includes('commute')) {
      return 'bi-car-front';
    }
    if (text.includes('energy') || text.includes('electricity')) {
      return 'bi-lightning-charge';
    }
    if (text.includes('food') || text.includes('diet')) {
      return 'bi-egg-fried';
    }
    if (text.includes('waste')) {
      return 'bi-trash3';
    }
    if (text.includes('water') || text.includes('liters') || text.includes('gallons')) {
      return 'bi-droplet-fill';
    }

    return 'bi-activity';
  }

  public getCategoryClass(log: CarbonLogResponse): string {
    const text = `${log.activityCategory} ${log.description ?? ''}`.toLowerCase();

    if (text.includes('transport') || text.includes('commute')) {
      return 'transport-icon';
    }
    if (text.includes('energy') || text.includes('electricity')) {
      return 'energy-icon';
    }
    if (text.includes('food') || text.includes('diet')) {
      return 'food-icon';
    }
    if (text.includes('waste')) {
      return 'waste-icon';
    }
    if (text.includes('water') || text.includes('liters') || text.includes('gallons')) {
      return 'water-icon';
    }

    return 'transport-icon';
  }

  public getActivityXp(log: CarbonLogResponse): number {
    const impact = Number(log.co2Impact ?? 0);
    const desc = (log.description ?? '').toLowerCase();
    const isGreen = impact === 0 ||
      ['walk', 'bike', 'cycl', 'recycl', 'compost', 'public transport', 'bus', 'train'].some(k => desc.includes(k));

    if (isGreen) return 50;
    if (impact > 500) return 200;
    if (impact > 200) return 100;
    if (impact > 100) return 50;
    if (impact > 50)  return 30;
    if (impact > 0)   return 20;
    return 0;
  }
}