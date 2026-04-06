package org.example.paymentservice.repository;

import org.example.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    Optional<Payment> findByWompiReference(String wompiReference);

    Optional<Payment> findByWompiTransactionId(String wompiTransactionId);
}
