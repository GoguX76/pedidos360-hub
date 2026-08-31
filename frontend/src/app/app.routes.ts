import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { LoginComponent } from './features/login/login';
import { Home } from './features/home/home';


export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'home', component: Home, canActivate: [MsalGuard] },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];