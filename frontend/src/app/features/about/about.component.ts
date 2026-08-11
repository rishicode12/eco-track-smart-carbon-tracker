import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.css']
})
export class AboutComponent {
  public coreValues = [
    {
      icon: 'bi-flower1',
      title: 'Sustainability First',
      desc: 'Our primary goal is reducing global emissions. Every feature we build serves environmental recovery.'
    },
    {
      icon: 'bi-cpu',
      title: 'AI Innovation',
      desc: 'We utilize advanced predictive models and recommendations to optimize carbon offsets in real-time.'
    },
    {
      icon: 'bi-shield-check',
      title: 'Data Integrity',
      desc: 'Accurate carbon auditing requires transparent calculations. We uphold strict data validation protocols.'
    },
    {
      icon: 'bi-people',
      title: 'Community Driven',
      desc: 'Individual habits accumulate into global changes. We create gamified features to connect and empower.'
    }
  ];
}
