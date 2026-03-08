import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TextFieldComponent } from '../../../../shared/ui/text-field/text-field.component';
import { RegisterRequest } from '../../store/auth.models';

@Component({
  selector: 'app-register-form',
  imports: [ReactiveFormsModule, RouterLink, TextFieldComponent],
  templateUrl: './register-form.component.html',
  styleUrl: '../login-form/login-form.component.css'
})
export class RegisterFormComponent {
  @Input() loading = false;
  @Input() error: string | null = null;
  @Output() registerSubmit = new EventEmitter<RegisterRequest>();

  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', [Validators.maxLength(30)]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/)
      ]
    ]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { fullName, email, password, phoneNumber } = this.form.getRawValue();
    this.registerSubmit.emit({
      fullName: fullName.trim(),
      email,
      password,
      phoneNumber: phoneNumber.trim() || null
    });
  }

  get fullNameError(): string {
    const control = this.form.controls.fullName;
    if (control.hasError('required')) {
      return 'Full name is required.';
    }
    if (control.hasError('minlength')) {
      return 'Full name must be at least 2 characters.';
    }
    return '';
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

  get phoneNumberError(): string {
    const control = this.form.controls.phoneNumber;
    if (control.hasError('maxlength')) {
      return 'Phone number must not exceed 30 characters.';
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
    if (control.hasError('pattern')) {
      return 'Password must include uppercase, lowercase, and a number.';
    }
    return '';
  }
}
