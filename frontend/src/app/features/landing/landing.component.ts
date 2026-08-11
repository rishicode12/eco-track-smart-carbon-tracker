import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent {
  public stats = [
    { value: '85%', label: 'Emission Reduction' },
    { value: '2.5M', label: 'Tons CO₂ Offset' },
    { value: '120+', label: 'Enterprise Partners' },
    { value: '99%', label: 'Client Satisfaction' }
  ];
}
