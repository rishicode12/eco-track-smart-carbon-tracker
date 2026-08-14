import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService, AIInsightResponse } from './ai.service';

interface Message {
  sender: 'user' | 'ai';
  text: string;
  timestamp: string;
}

@Component({
  selector: 'app-ai',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai.component.html',
  styleUrls: ['./ai.component.css']
})
export class AiComponent implements OnInit {
  private readonly aiService = inject(AiService);
  private readonly cdr = inject(ChangeDetectorRef);

  public messages: Message[] = [];
  public chatInput = '';
  public isTyping = false;
  public isLoadingInsights = true;
  public insights: AIInsightResponse | null = null;

  public promptSuggestions = [
    'How can I reduce my home electricity consumption?',
    'Is public transport better than driving an EV?',
    'Suggest a green alternative for plastic packaging.'
  ];

  ngOnInit(): void {
    this.fetchInsights();
  }

  public fetchInsights(): void {
    this.isLoadingInsights = true;
    this.aiService.getInsights().subscribe({
      next: (response) => {
        this.isLoadingInsights = false;
        
        if (response && response.data) {
          this.insights = response.data;
          this.initAiWelcomeMessage();
        } else {
          this.setFallbackMessage();
        }
        
        // MOVED HERE: Updates the UI after messages are loaded
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        console.error('Failed to fetch AI insights:', err);
        this.isLoadingInsights = false;
        this.setFallbackMessage();
        
        // MOVED HERE: Updates the UI if it fails
        this.cdr.detectChanges();
      }
    });
  }

  private initAiWelcomeMessage(): void {
    if (!this.insights) return;

    let welcome = `Hello! I have evaluated your carbon logs. Your current emission risk level is classified as **${this.insights.riskLevel}**.\n\n`;

    if (this.insights.recommendations && this.insights.recommendations.length > 0) {
      welcome += `**Top Action Plan:**\n• ${this.insights.recommendations[0]}\n\n`;
    }
    if (this.insights.nextGoalSuggestion) {
      welcome += `**Suggested Next Goal:** ${this.insights.nextGoalSuggestion}`;
    }

    this.messages.push({
      sender: 'ai',
      text: welcome,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });
  }

  private setFallbackMessage(): void {
    this.messages.push({
      sender: 'ai',
      text: 'Hello! I am your Eco-AI Assistant. Start logging your daily carbon activities to generate real-time predictive insights!',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });
  }

  public onSendMessage(text?: string) {
    const messageText = text || this.chatInput.trim();
    if (!messageText) return;

    this.messages.push({
      sender: 'user',
      text: messageText,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    });

    if (!text) {
      this.chatInput = '';
    }

    this.isTyping = true;
    // ADDED HERE: Forces the UI to show the typing bubble immediately
    this.cdr.detectChanges(); 

    setTimeout(() => {
      this.isTyping = false;
      let reply = "Small changes in daily habits can reduce your overall footprint by up to 25% annually.";

      const query = messageText.toLowerCase();
      if (query.includes('electricity') || query.includes('home')) {
        reply = 'To reduce electricity consumption at home:\n• Switch to energy-star labeled heat pump appliances.\n• Unplug standby phantom loads.\n• Utilize smart thermostats to lower heating when away.\n• This can save up to 450kg CO₂ annually.';
      } else if (query.includes('transport') || query.includes('ev') || query.includes('car')) {
        reply = 'Comparing public transport to EVs:\n• EVs reduce lifecycle emissions by ~60% vs petrol engines.\n• Trains and buses yield up to 85% emission reductions per passenger-mile.';
      } else if (query.includes('plastic') || query.includes('packaging')) {
        reply = 'Green alternatives for plastic packaging include:\n• Biodegradable mushroom packaging (mycelium).\n• Post-consumer recycled paperboard.\n• Beeswax wraps or reusable silicon pouches.';
      } else if (this.insights && (query.includes('goal') || query.includes('suggest'))) {
        reply = `Based on your profile, I recommend setting this goal next: **${this.insights.nextGoalSuggestion}**`;
      }

      this.messages.push({
        sender: 'ai',
        text: reply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      });
      
      // ADDED HERE: Forces the UI to remove the typing bubble and show the AI's reply
      this.cdr.detectChanges(); 
    }, 1200);
  }
}