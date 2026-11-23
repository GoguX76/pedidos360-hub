import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { LoginComponent } from './features/login/login';
import { MenuComponent } from './features/menu/menu';
import { CartComponent } from './features/cart/cart';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'menu', component: MenuComponent },
  { path: 'carrito', component: CartComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];