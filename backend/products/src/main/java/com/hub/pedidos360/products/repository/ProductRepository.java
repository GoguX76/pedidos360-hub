package com.hub.pedidos360.products.repository;

import com.hub.pedidos360.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a los datos de productos. Como en los demás microservicios,
 * no escribimos SQL a mano — Spring Data JPA genera la consulta a partir
 * del nombre del método.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Solo los productos disponibles — útil para cuando orders-service
     * quiera mostrar el menú activo, sin productos agotados/dados de baja.
     */
    List<Product> findByAvailableTrue();

    /**
     * Descuenta stock de forma atómica: la base de datos actualiza y verifica
     * en una sola operación (WHERE stock >= :quantity). Así, dos descuentos
     * simultáneos nunca dejan el stock en negativo. Retorna cuántas filas
     * se actualizaron (0 = sin stock suficiente o id inexistente).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);
}