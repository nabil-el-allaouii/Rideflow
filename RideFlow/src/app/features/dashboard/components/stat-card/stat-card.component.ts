import { Component, Input } from '@angular/core';

export type StatIcon = 'rides' | 'distance' | 'time' | 'co2';

export interface StatMetric {
  value: string;
  label: string;
  helper: string;
  icon: StatIcon;
}

@Component({
  selector: 'app-stat-card',
  imports: [],
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.css'
})
export class StatCardComponent {
  @Input({ required: true }) metric!: StatMetric;
}
