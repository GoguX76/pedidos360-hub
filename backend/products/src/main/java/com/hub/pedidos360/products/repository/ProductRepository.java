package com.hub.pedidos360.products.repository;

import com.hub.pedidos360.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a los datos de productos. Igual que con NotificationRepository,
 * no escribimos SQL a mano — Spring Data JPA genera la consulta a partir
 * del nombre del método.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Solo los productos disponibles — útil para cuando orders-service
     * quiera mostrar el menú activo, sin productos agotados/dados de baja.
     */
    List<Product> findByDisponibleTrue();
}