import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LoginCredentials } from '../../store/auth.models';
import { TextFieldComponent } from '../../../../shared/ui/text-field/text-field.component';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, RouterLink, TextFieldComponent],
  templateUrl: './login-form.component.html',
  styleUrl: './login-form.component.css'
})
export class LoginFormComponent {
  @Input() loading = false;
  @Input() error: string | null = null;
  @Output() loginSubmit = new EventEmitter<LoginCredentials>();

  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loginSubmit.emit(this.form.getRawValue());
  }

  get emailError(): string {
    const control = this.form.controls.email;
    if (control.hasError('required')) {
      return 'Email is required.';
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address.';
    }
    return '';
  }

  get passwordError(): string {
    const control = this.form.controls.password;
    if (control.hasError('required')) {
      return 'Password is required.';
    }
    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }
    return '';
  }
}
