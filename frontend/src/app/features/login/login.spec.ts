import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { LoginComponent } from './login';
import { AuthService } from '../../core/auth/auth.service';

describe('Login', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
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

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra las categorías destacadas del catálogo', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Pastelería');
    expect(el.textContent).toContain('Panadería');
  });
});
