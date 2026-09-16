package com.hub.pedidos360.products.service;

import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProductServiceConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
    }

    @Test
    void decrementStockReduceElStockDisponible() {
        Product saved = createProductWithStock(10);

        productService.decrementStock(saved.getId(), 3);

        Product actual = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(actual.getStock()).isEqualTo(7);
    }

    @Test
    void decrementStockBeyondLanceExcepcionYNoDejaStockNegativo() {
        Product saved = createProductWithStock(2);

        assertThatThrownBy(() -> productService.decrementStock(saved.getId(), 5))
                .isInstanceOf(InsufficientStockException.class);

        Product actual = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(actual.getStock()).isEqualTo(2);
    }

    @Test
    void decrementStockConCantidadInvalidaLanzaIllegalArgumentException() {
        Product saved = createProductWithStock(10);

        assertThatThrownBy(() -> productService.decrementStock(saved.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> productService.decrementStock(saved.getId(), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrementStockNuncaDejaStockNegativoBajoConcurrencia() throws InterruptedException {
        // El caso pide consistencia ante compras simultáneas: 30 pedidos
        // concurrentes necesitan el mismo producto con stock = 10.
        Product saved = createProductWithStock(10);
        int totalRequests = 30;

        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch everyoneReady = new CountDownLatch(totalRequests);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                everyoneReady.countDown();
                try {
                    start.await();
                    productService.decrementStock(saved.getId(), 1);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        everyoneReady.await();
        start.countDown();
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(20);

        Product actual = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(actual.getStock()).isZero();
    }

    private Product createProductWithStock(int stock) {
        Product product = new Product();
        product.setName("Arepa Sencilla");
        product.setDescription("De queso amarillo, con mantequilla");
        product.setPrice(new BigDecimal("8.50"));
        product.setStock(stock);
        product.setCategory("Desayunos");
        product.setAvailable(true);
        return productRepository.save(product);
    }
}