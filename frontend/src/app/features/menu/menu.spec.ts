import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { MenuComponent } from './menu';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/api/api.service';

describe('Menu', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MenuComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
        {
          provide: AuthService,
          useValue: {
            authenticatedUser: signal(false),
            userName: signal<string | null>(null),
            login: () => {},
            logOut: () => {},
          },
        },
        { provide: ApiService, useValue: { getProducts: () => of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('muestra el estado vacío cuando no hay productos', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No encontramos productos');
  });
});
