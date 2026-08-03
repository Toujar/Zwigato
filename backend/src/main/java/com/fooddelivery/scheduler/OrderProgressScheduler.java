package com.fooddelivery.scheduler;

/**
 * OrderProgressScheduler — DISABLED
 *
 * The 60-second auto-advance demo has been removed.
 *
 * Order status is now advanced through the proper flow:
 *   1. Restaurant owner logs into /dashboard/orders
 *   2. Clicks "Mark as CONFIRMED", "Mark as PREPARING", etc.
 *   3. Customer's tracking page polls GET /orders/:id every 30s
 *      and reflects the real status instantly.
 *
 * This class is kept as a placeholder so the package structure
 * is preserved. It does nothing.
 */
public class OrderProgressScheduler {
    // No beans registered — class is not annotated with @Component
}
