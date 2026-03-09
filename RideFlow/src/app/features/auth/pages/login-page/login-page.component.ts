import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { BrandPanelComponent } from '../../components/brand-panel/brand-panel.component';
import { LoginFormComponent } from '../../components/login-form/login-form.component';
import { AuthFacade } from '../../store/auth.facade';
import { LoginCredentials } from '../../store/auth.models';

@Component({
  selector: 'app-login-page',
  imports: [AsyncPipe, BrandPanelComponent, LoginFormComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css'
})
export class LoginPageComponent {
  private readonly authFacade = inject(AuthFacade);

  readonly isLoading$ = this.authFacade.isLoading$;
  readonly error$ = this.authFacade.error$;

  onLoginSubmit(credentials: LoginCredentials): void {
    this.authFacade.clearError();
    this.authFacade.login(credentials);
  }
}
