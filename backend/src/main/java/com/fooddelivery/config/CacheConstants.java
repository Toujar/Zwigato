package com.fooddelivery.config;

/**
 * ============================================================
 *  CacheConstants
 * ============================================================
 *
 *  Single source of truth for every Redis cache name and TTL
 *  used in this application.
 *
 *  Rules:
 *   - ONE constant per cache.  Never use a string literal in
 *     @Cacheable / @CachePut / @CacheEvict — always reference
 *     the constant here.
 *   - TTL values are in SECONDS and feed directly into RedisConfig.
 *   - Key format convention:  zwigato:<cache-name>::<key>
 *     (the prefix "zwigato:" is set in application.properties)
 *
 *  Cache strategy per domain:
 *
 *  RESTAURANTS      — public browse, high traffic.
 *                     Single record + full list.  TTL 10 min.
 *  FOOD_ITEMS       — per-restaurant menu list + single item.
 *                     Evicted when any item in the restaurant changes.
 *                     TTL 10 min.
 *  CATEGORIES       — small, almost static reference table.
 *                     TTL 30 min.  Only evicted on admin writes.
 *  USER_PROFILES    — per-user profile data.
 *                     TTL 15 min.  Evicted on profile update.
 * ============================================================
 */
public final class CacheConstants {

    private CacheConstants() { /* utility class — no instantiation */ }

    // ----------------------------------------------------------
    // Cache names
    // ----------------------------------------------------------

    /** Single restaurant by ID  +  paginated restaurant list. */
    public static final String RESTAURANTS   = "restaurants";

    /** Single food item by ID  +  all items for a restaurant. */
    public static final String FOOD_ITEMS    = "foodItems";

    /** All active categories  +  single category by ID. */
    public static final String CATEGORIES    = "categories";

    /** Currently-authenticated user's profile response. */
    public static final String USER_PROFILES = "userProfiles";

    // ----------------------------------------------------------
    // TTL values (seconds)
    // ----------------------------------------------------------

    /** 10 minutes — restaurant data is moderately dynamic. */
    public static final long TTL_RESTAURANTS   = 600L;

    /** 10 minutes — menu items change when owner edits them. */
    public static final long TTL_FOOD_ITEMS    = 600L;

    /** 30 minutes — categories barely ever change. */
    public static final long TTL_CATEGORIES    = 1800L;

    /** 15 minutes — profile data is user-specific. */
    public static final long TTL_USER_PROFILES = 900L;
}
