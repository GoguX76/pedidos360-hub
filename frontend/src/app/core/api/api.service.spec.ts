import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let apiService: ApiService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    apiService = TestBed.inject(ApiService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('elimina un producto usando el endpoint DELETE del catálogo', () => {
    apiService.deleteProduct(42).subscribe();

    const request = httpTestingController.expectOne(
      `${window.location.origin}/api/v1/products/42`,
    );

    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
