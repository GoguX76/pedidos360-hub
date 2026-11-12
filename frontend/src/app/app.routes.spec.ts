import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { routes } from './app.routes';

describe('routes', () => {
  it('redirige la ruta raíz a la página de inicio', async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: AuthService,
          useValue: {
            authenticatedUser: signal(false),
            userName: signal<string | null>(null),
            login: () => {},
            logOut: () => {},
          },
        },
      ],
    }).compileComponents();

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');

    expect(harness.routeNativeElement?.textContent).toContain('Tu cocina');
    expect(harness.routeNativeElement?.textContent).not.toContain('No encontramos productos');
  });
});
