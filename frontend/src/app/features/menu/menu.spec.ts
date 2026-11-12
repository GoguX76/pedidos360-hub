import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { MenuComponent } from './menu';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/api/api.service';
import type { Product } from '../../core/api/models/product.model';

describe('Menu', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;
  let deleteProduct: ReturnType<typeof vi.fn>;

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
            isAdmin: signal(false),
            userName: signal<string | null>(null),
            login: () => {},
            logOut: () => {},
          },
        },
        {
          provide: ApiService,
          useValue: {
            getProducts: () => of([]),
            deleteProduct: (deleteProduct = vi.fn(() => of(undefined))),
          },
        },
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

  it('permite a un administrador eliminar un producto desde la tarjeta', () => {
    const product: Product = {
      id: 7,
      name: 'Pastel de prueba',
      description: 'Descripción',
      price: 1000,
      stock: 5,
      category: 'Pastelería',
      available: true,
    };
    const authService = TestBed.inject(AuthService);
    authService.isAdmin.set(true);
    component['products'].set([product]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fixture.detectChanges();

    const buttons = (): HTMLButtonElement[] =>
      Array.from(
        fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
      );
    buttons().find((button) => button.textContent?.includes('Admin'))?.click();
    fixture.detectChanges();
    buttons()
      .find((button) => button.textContent?.includes('Eliminar productos'))
      ?.click();
    fixture.detectChanges();
    (
      fixture.nativeElement.querySelector(
        '[aria-label="Eliminar Pastel de prueba"]',
      ) as HTMLButtonElement | null
    )?.click();
    fixture.detectChanges();

    expect(deleteProduct).toHaveBeenCalledWith(product.id);
    expect(fixture.nativeElement.querySelector('article h3')).toBeNull();
    vi.restoreAllMocks();
  });
});
