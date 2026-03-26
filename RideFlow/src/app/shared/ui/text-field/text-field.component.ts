import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

let inputCounter = 0;

type TextFieldIcon = 'email' | 'lock' | 'user' | 'none';

@Component({
  selector: 'app-text-field',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './text-field.component.html',
  styleUrl: './text-field.component.css'
})
export class TextFieldComponent {
  @Input({ required: true }) label = '';
  @Input({ required: true }) control!: FormControl<string>;
  @Input() type: 'text' | 'email' | 'password' = 'text';
  @Input() placeholder = '';
  @Input() autocomplete = 'off';
  @Input() errorMessage = '';
  @Input() icon: TextFieldIcon = 'none';
  @Input() ariaLabel = '';

  readonly id = `rf-input-${inputCounter++}`;
  protected isPasswordVisible = false;

  get showError(): boolean {
    return this.control.invalid && (this.control.dirty || this.control.touched);
  }

  get resolvedInputType(): 'text' | 'email' | 'password' {
    if (this.type === 'password' && this.isPasswordVisible) {
      return 'text';
    }

    return this.type;
  }

  togglePasswordVisibility(): void {
    this.isPasswordVisible = !this.isPasswordVisible;
  }
}
