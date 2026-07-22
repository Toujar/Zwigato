package com.fooddelivery.repository;

import com.fooddelivery.entity.Payment;
import com.fooddelivery.entity.enums.PaymentMethod;
import com.fooddelivery.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  Repository : PaymentRepository
 *  Entity     : Payment
 *  Table      : payments
 * ============================================================
 *
 *  Payment records are write-once / status-update-only.
 *  Once a payment row exists for an order, the amount and
 *  payment method never change — only the status and gateway
 *  fields are updated on callbacks.
 *
 *  Custom methods cover:
 *   1. Lookup by order and transaction (primary access patterns)
 *   2. Existence checks (prevent duplicate payments)
 *   3. Targeted status update (gateway webhook handler)
 *   4. Status and method filtering (admin reconciliation)
 *   5. Failed payment retry list
 *   6. Revenue statistics
 *   7. Date-range queries for reconciliation reports
 * ============================================================
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ----------------------------------------------------------
    // 1. Primary Lookups
    // ----------------------------------------------------------

    /**
     * Finds the payment record for a given order.
     *
     * Used by: OrderService / PaymentService — after placing an order,
     * the customer is redirected to the payment page which calls this
     * to display payment status.
     * Also used by the admin order detail page.
     *
     * SQL: SELECT * FROM payments WHERE order_id = ?
     */
    Optional<Payment> findByOrder_Id(Long orderId);

    /**
     * Finds a payment by its gateway transaction ID.
     *
     * Used by: Payment gateway webhook handler — when the gateway
     * sends a callback (success / failure), it includes its own
     * transaction ID. This query locates the matching Payment row
     * so its status can be updated.
     * This is the most security-sensitive query — validate the
     * transaction ID against the gateway signature before using it.
     *
     * SQL: SELECT * FROM payments WHERE transaction_id = ?
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Finds a payment by order ID, eagerly fetching the order
     * and its user in one query.
     *
     * Used by: Payment confirmation email sender — needs order details
     * and customer email without triggering lazy loads.
     *
     * JPQL:
     *   SELECT p FROM Payment p
     *   JOIN FETCH p.order o
     *   JOIN FETCH o.user
     *   WHERE p.order.id = :orderId
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.order o
        JOIN FETCH o.user
        WHERE p.order.id = :orderId
        """)
    Optional<Payment> findByOrder_IdWithOrderAndUser(@Param("orderId") Long orderId);

    // ----------------------------------------------------------
    // 2. Existence Checks
    // ----------------------------------------------------------

    /**
     * Checks whether a payment record already exists for an order.
     *
     * Used by: PaymentService.initiatePayment() — prevents creating
     * a duplicate payment row if the customer clicks "Pay" twice.
     * Should return HTTP 409 Conflict if this returns true.
     *
     * SQL: SELECT COUNT(*) > 0 FROM payments WHERE order_id = ?
     */
    boolean existsByOrder_Id(Long orderId);

    /**
     * Checks whether a transaction ID is already stored in the system.
     *
     * Used by: Gateway webhook handler — rejects duplicate callbacks
     * with the same transaction ID (idempotency guard).
     *
     * SQL: SELECT COUNT(*) > 0 FROM payments WHERE transaction_id = ?
     */
    boolean existsByTransactionId(String transactionId);

    // ----------------------------------------------------------
    // 3. Targeted Status Update (Gateway Callback Handler)
    // ----------------------------------------------------------

    /**
     * Updates payment status, transaction ID, gateway response, and
     * paidAt timestamp in a single targeted UPDATE — without loading
     * the full Payment entity.
     *
     * Used by: Payment gateway webhook handler on SUCCESS callback.
     * A single write covers all fields that change on confirmation.
     *
     * @param id               the payment row to update
     * @param status           the new PaymentStatus (SUCCESS / FAILED / REFUNDED)
     * @param transactionId    the gateway's transaction reference
     * @param gatewayResponse  raw JSON response from the gateway
     * @param paidAt           timestamp when payment was confirmed
     *
     * @Modifying   — required for JPQL UPDATE.
     * @Transactional — runs atomically; partial updates are rolled back on error.
     *
     * Returns: 1 if updated, 0 if payment not found.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Payment p
        SET p.status          = :status,
            p.transactionId   = :transactionId,
            p.gatewayResponse = :gatewayResponse,
            p.paidAt          = :paidAt
        WHERE p.id = :id
        """)
    int updatePaymentStatus(
            @Param("id")               Long id,
            @Param("status")           PaymentStatus status,
            @Param("transactionId")    String transactionId,
            @Param("gatewayResponse")  String gatewayResponse,
            @Param("paidAt")           LocalDateTime paidAt);

    /**
     * Updates only the status and gateway response (FAILED / REFUNDED cases).
     *
     * Used by: Gateway webhook handler on FAILED or REFUNDED callbacks.
     * paidAt remains null for failed payments.
     *
     * Returns: 1 if updated, 0 if not found.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Payment p
        SET p.status          = :status,
            p.gatewayResponse = :gatewayResponse
        WHERE p.id = :id
        """)
    int updatePaymentStatusOnly(
            @Param("id")              Long id,
            @Param("status")          PaymentStatus status,
            @Param("gatewayResponse") String gatewayResponse);

    // ----------------------------------------------------------
    // 4. Status and Method Filtering (Admin Reconciliation)
    // ----------------------------------------------------------

    /**
     * Returns a paginated list of payments in a given status.
     *
     * Used by: Admin reconciliation — "Show all PENDING payments"
     * to find stale transactions that may need manual intervention.
     *
     * SQL: SELECT * FROM payments WHERE status = ? ORDER BY created_at DESC
     */
    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    /**
     * Returns all payments made using a specific payment method.
     *
     * Used by: Finance team reporting — "How many UPI payments this month?"
     *
     * SQL: SELECT * FROM payments WHERE payment_method = ?
     */
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    /**
     * Returns payments with a specific status AND payment method.
     *
     * Used by: Finance reconciliation — e.g., "All failed CREDIT_CARD payments"
     * to identify problematic card processing issues.
     *
     * SQL: SELECT * FROM payments WHERE status = ? AND payment_method = ?
     */
    List<Payment> findByStatusAndPaymentMethod(PaymentStatus status, PaymentMethod paymentMethod);

    // ----------------------------------------------------------
    // 5. Failed Payment Retry List
    // ----------------------------------------------------------

    /**
     * Returns all FAILED or PENDING payments created before a given
     * timestamp that have no transaction ID yet.
     *
     * Used by: Scheduled retry job — finds stale PENDING payments
     * (e.g., user abandoned after initiation, no gateway callback received)
     * so they can be expired or retried.
     *
     * @param statuses   list of statuses to include (e.g., [PENDING, FAILED])
     * @param before     timestamp threshold — only payments older than this
     *
     * JPQL: WHERE status IN (:statuses)
     *         AND transaction_id IS NULL
     *         AND created_at < :before
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.status         IN :statuses
          AND p.transactionId  IS NULL
          AND p.createdAt      < :before
        """)
    List<Payment> findStalePayments(
            @Param("statuses") List<PaymentStatus> statuses,
            @Param("before")   LocalDateTime before);

    // ----------------------------------------------------------
    // 6. Revenue Statistics
    // ----------------------------------------------------------

    /**
     * Calculates total revenue from all successful payments.
     *
     * Used by: Admin financial dashboard — "Total Platform Revenue".
     * Returns null if no successful payments exist; service should handle null.
     *
     * JPQL: SELECT SUM(p.amount) FROM Payment p WHERE p.status = SUCCESS
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = com.fooddelivery.entity.enums.PaymentStatus.SUCCESS")
    BigDecimal calculateTotalSuccessfulRevenue();

    /**
     * Calculates successful payment revenue within a date range.
     *
     * Used by: Admin weekly/monthly revenue reports.
     *
     * @param from  start of range (inclusive)
     * @param to    end of range (inclusive)
     *
     * JPQL: SELECT SUM(amount) WHERE status = SUCCESS AND paid_at BETWEEN ? AND ?
     */
    @Query("""
        SELECT SUM(p.amount) FROM Payment p
        WHERE p.status = com.fooddelivery.entity.enums.PaymentStatus.SUCCESS
          AND p.paidAt BETWEEN :from AND :to
        """)
    BigDecimal calculateRevenueByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Counts the number of successful payments per payment method.
     *
     * Used by: Admin analytics pie chart — payment method breakdown.
     *
     * Returns Object[] rows: [PaymentMethod, Long count]
     *
     * JPQL:
     *   SELECT p.paymentMethod, COUNT(p)
     *   FROM Payment p
     *   WHERE p.status = SUCCESS
     *   GROUP BY p.paymentMethod
     */
    @Query("""
        SELECT p.paymentMethod, COUNT(p) AS cnt
        FROM   Payment p
        WHERE  p.status = com.fooddelivery.entity.enums.PaymentStatus.SUCCESS
        GROUP  BY p.paymentMethod
        ORDER  BY cnt DESC
        """)
    List<Object[]> countSuccessfulPaymentsByMethod();

    // ----------------------------------------------------------
    // 7. Date-Range Reconciliation
    // ----------------------------------------------------------

    /**
     * Returns all payments within a date range, paginated.
     *
     * Used by: Finance team — daily / weekly reconciliation with the
     * payment gateway's settlement report.
     * Ordered by createdAt so records match gateway export order.
     *
     * @param from  start of range (inclusive), based on createdAt
     * @param to    end of range (inclusive)
     *
     * JPQL: WHERE created_at BETWEEN :from AND :to ORDER BY created_at ASC
     */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.createdAt BETWEEN :from AND :to
        ORDER BY p.createdAt ASC
        """)
    Page<Payment> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            Pageable pageable);

    // ----------------------------------------------------------
    // 8. Counts
    // ----------------------------------------------------------

    /**
     * Counts payments in a given status.
     *
     * Used by: Admin dashboard — "Pending payments: 12" alert card.
     *
     * SQL: SELECT COUNT(*) FROM payments WHERE status = ?
     */
    long countByStatus(PaymentStatus status);
}
