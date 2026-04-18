package com.dissertation.inventoryservice.repository;

import com.dissertation.inventoryservice.domain.Product;
import com.dissertation.inventoryservice.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByCodeAndStatusNot(String code, ProductStatus status);

    boolean existsByCode(String code);

    boolean existsByIsbn(String isbn);

    @Modifying
    @Query("UPDATE Product p SET p.reservedQuantity = p.reservedQuantity + :quantity " +
           "WHERE p.code = :code AND p.status <> 'DISCONTINUED' AND (p.stockQuantity - p.reservedQuantity) >= :quantity")
    int reserveStock(String code, Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.reservedQuantity = p.reservedQuantity - :quantity, " +
           "p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.code = :code AND p.reservedQuantity >= :quantity AND p.stockQuantity >= :quantity")
    int commitStock(String code, Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.reservedQuantity = p.reservedQuantity - :quantity " +
           "WHERE p.code = :code AND p.reservedQuantity >= :quantity")
    int releaseStock(String code, Integer quantity);
}
