import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-brand-panel',
  imports: [],
  templateUrl: './brand-panel.component.html',
  styleUrl: './brand-panel.component.css'
})
export class BrandPanelComponent {
  @Input() titleLight = 'Ride';
  @Input() titleAccent = 'Flow';
  @Input() subtitle = 'Smart electric scooter rentals for modern urban mobility';
}
