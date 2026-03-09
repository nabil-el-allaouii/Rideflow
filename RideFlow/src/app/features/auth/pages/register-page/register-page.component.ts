import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { BrandPanelComponent } from '../../components/brand-panel/brand-panel.component';
import { RegisterFormComponent } from '../../components/register-form/register-form.component';
import { AuthFacade } from '../../store/auth.facade';
import { RegisterRequest } from '../../store/auth.models';

@Component({
  selector: 'app-register-page',
  imports: [AsyncPipe, BrandPanelComponent, RegisterFormComponent],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.css'
})
export class RegisterPageComponent {
  private readonly authFacade = inject(AuthFacade);

  readonly isLoading$ = this.authFacade.isLoading$;
  readonly error$ = this.authFacade.error$;

  onRegisterSubmit(payload: RegisterRequest): void {
    this.authFacade.clearError();
    this.authFacade.register(payload);
  }
}
