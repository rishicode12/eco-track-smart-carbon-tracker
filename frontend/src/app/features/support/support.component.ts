import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

interface FaqItem {
  question: string;
  answer: string;
  category: 'general' | 'carbon' | 'account';
  isOpen?: boolean;
}

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './support.component.html',
  styleUrls: ['./support.component.scss']
})
export class SupportComponent {
  public contactForm = {
    name: '',
    email: '',
    subject: 'general',
    message: ''
  };

  public isSubmitting = false;
  public submitSuccess = false;
  public activeFilter: 'all' | 'general' | 'carbon' | 'account' = 'all';

  public faqs: FaqItem[] = [
    {
      category: 'carbon',
      question: 'How is my carbon footprint calculated?',
      answer: 'EcoTrack uses internationally recognized emission factor standards (IPCC and DEFRA). When you log an activity, our engine calculates equivalent CO₂ in kilograms (kg CO₂e) by multiplying your activity volume (e.g. kilometers driven, kWh consumed, meal type, or kilograms of waste) by vetted lifecycle emission factors.'
    },
    {
      category: 'account',
      question: 'How do I reset my account password?',
      answer: 'You can reset your password anytime by navigating to Settings in your Profile or by clicking "Forgot Password?" on the login page. An email with a secure, time-limited reset verification token will be delivered to your registered email address.'
    },
    {
      category: 'carbon',
      question: 'How are sustainability goals tracked and verified?',
      answer: 'Your goals track cumulative carbon reduction against your historical 30-day baseline. When you complete actions that reduce emissions below your baseline, the delta is credited directly to your active goal progress. You can also manually mark milestones complete.'
    },
    {
      category: 'general',
      question: 'How does the AI Assistant generate personalized eco-recommendations?',
      answer: 'EcoTrack utilizes OpenAI APIs with specialized prompt engineering. The system securely analyzes your recent categorical emission patterns (anonymized) to synthesize actionable, high-impact habits tailored to your lifestyle.'
    },
    {
      category: 'account',
      question: 'Can I export my carbon reports for official or ESG purposes?',
      answer: 'Yes! Navigate to the Reports tab to download high-resolution PDF or CSV data summaries, complete with category breakdowns, monthly trajectories, and audit-ready sustainability metrics.'
    },
    {
      category: 'general',
      question: 'How do XP, Levels, and Badges work?',
      answer: 'You earn XP for logging emissions consistently, achieving goals, and participating in challenges. As your XP grows, your tier advances from Iron through Bronze, Silver, Gold, Platinum, and Diamond, unlocking exclusive profile badges.'
    }
  ];

  public get filteredFaqs(): FaqItem[] {
    if (this.activeFilter === 'all') {
      return this.faqs;
    }
    return this.faqs.filter(f => f.category === this.activeFilter);
  }

  public toggleFaq(faq: FaqItem): void {
    faq.isOpen = !faq.isOpen;
  }

  public onSubmitContact(): void {
    if (!this.contactForm.name || !this.contactForm.email || !this.contactForm.message) {
      return;
    }

    this.isSubmitting = true;
    
    // Simulate contact ticket submission
    setTimeout(() => {
      this.isSubmitting = false;
      this.submitSuccess = true;
      this.contactForm = {
        name: '',
        email: '',
        subject: 'general',
        message: ''
      };
    }, 900);
  }

  public dismissSuccess(): void {
    this.submitSuccess = false;
  }
}
