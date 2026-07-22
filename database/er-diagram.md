# Food Delivery App — ER Diagram

## Text ER Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        FOOD DELIVERY APPLICATION — ER DIAGRAM                       │
└─────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐          ┌────────────────────────────┐
│        USERS         │          │        RESTAURANTS         │
├──────────────────────┤          ├────────────────────────────┤
│ PK  id               │1       1+│ PK  id                     │
│     name             │──────────│ FK  owner_id  ──► users.id │
│     email  (UNIQUE)  │  OWNS    │     name                   │
│     password         │          │     description            │
│     phone  (UNIQUE)  │          │     address                │
│     address          │          │     city                   │
│     role (ENUM)      │          │     phone                  │
│     is_active        │          │     email                  │
│     created_at       │          │     image_url              │
│     updated_at       │          │     rating                 │
└──────────────────────┘          │     delivery_time          │
          │                       │     min_order_amount       │
          │ 1                     │     is_open                │
          │                       │     is_active              │
          │ HAS 1                 │     created_at             │
          ▼                       │     updated_at             │
┌──────────────────────┐          └────────────────────────────┘
│        CARTS         │                      │ 1
├──────────────────────┤                      │
│ PK  id               │                      │ HAS MANY
│ FK  user_id ──►users │                      ▼
│ FK  restaurant_id    │          ┌────────────────────────────┐
│     created_at       │          │        FOOD ITEMS          │
│     updated_at       │          ├────────────────────────────┤
└──────────────────────┘          │ PK  id                     │
          │ 1                     │ FK  restaurant_id          │
          │                       │     ──► restaurants.id     │
          │ HAS MANY              │ FK  category_id            │
          ▼                       │     ──► categories.id      │
┌──────────────────────┐          │     name                   │
│      CART ITEMS      │          │     description            │
├──────────────────────┤          │     price                  │
│ PK  id               │          │     image_url              │
│ FK  cart_id          │          │     is_vegetarian          │
│     ──► carts.id     │          │     is_available           │
│ FK  food_item_id     │          │     created_at             │
│     ──► food_items.id│          │     updated_at             │
│     quantity         │          └────────────────────────────┘
│     unit_price       │                      │ MANY
│     created_at       │                      │
│     updated_at       │                      │ BELONGS TO 1
└──────────────────────┘                      ▼
                                  ┌────────────────────────────┐
                                  │        CATEGORIES          │
                                  ├────────────────────────────┤
                                  │ PK  id                     │
                                  │     name  (UNIQUE)         │
                                  │     description            │
                                  │     image_url              │
                                  │     is_active              │
                                  │     created_at             │
                                  └────────────────────────────┘


┌──────────────────────┐          ┌────────────────────────────┐
│        USERS         │          │          ORDERS            │
│  (CUSTOMER)          │1      1+ ├────────────────────────────┤
│                      │──────────│ PK  id                     │
│                      │ PLACES   │ FK  user_id ──► users.id   │
│  (DELIVERY AGENT)    │          │ FK  restaurant_id          │
│                      │1      0+ │     ──► restaurants.id     │
│                      │──────────│ FK  delivery_agent_id      │
│                      │ DELIVERS │     ──► users.id           │
└──────────────────────┘          │     delivery_address       │
                                  │     status (ENUM)          │
┌──────────────────────┐          │     subtotal               │
│     RESTAURANTS      │1      1+ │     delivery_fee           │
│                      │──────────│     tax                    │
│                      │ HAS      │     total_amount           │
└──────────────────────┘          │     special_instructions   │
                                  │     placed_at              │
                                  │     updated_at             │
                                  └────────────────────────────┘
                                              │ 1
                                              │
                                              │ HAS MANY
                                              ▼
                                  ┌────────────────────────────┐
                                  │        ORDER ITEMS         │
                                  ├────────────────────────────┤
                                  │ PK  id                     │
                                  │ FK  order_id               │
                                  │     ──► orders.id          │
                                  │ FK  food_item_id           │
                                  │     ──► food_items.id      │
                                  │     quantity               │
                                  │     unit_price  (snapshot) │
                                  │     subtotal               │
                                  └────────────────────────────┘

                                  ┌────────────────────────────┐
                                  │         PAYMENTS           │
                                  ├────────────────────────────┤
         orders.id ───────────────│ PK  id                     │
         (1-to-1)          HAS 1  │ FK  order_id (UNIQUE)      │
                                  │     ──► orders.id          │
                                  │     amount                 │
                                  │     payment_method (ENUM)  │
                                  │     status (ENUM)          │
                                  │     transaction_id (UNIQUE)│
                                  │     gateway_response       │
                                  │     paid_at                │
                                  │     created_at             │
                                  │     updated_at             │
                                  └────────────────────────────┘
```

---

## Relationships Summary

| Relationship                         | Type        | FK Column                      |
|--------------------------------------|-------------|-------------------------------|
| User **OWNS** Restaurant             | 1 → Many    | `restaurants.owner_id`        |
| User **HAS** Cart                    | 1 → 1       | `carts.user_id` (UNIQUE)      |
| User **PLACES** Orders               | 1 → Many    | `orders.user_id`              |
| User **DELIVERS** Orders             | 1 → Many    | `orders.delivery_agent_id`    |
| Restaurant **HAS** Food Items        | 1 → Many    | `food_items.restaurant_id`    |
| Restaurant **HAS** Orders            | 1 → Many    | `orders.restaurant_id`        |
| Category **HAS** Food Items          | 1 → Many    | `food_items.category_id`      |
| Cart **BELONGS TO** User             | 1 → 1       | `carts.user_id`               |
| Cart **LOCKED TO** Restaurant        | Many → 1    | `carts.restaurant_id`         |
| Cart **HAS** Cart Items              | 1 → Many    | `cart_items.cart_id`          |
| Cart Item **REFERENCES** Food Item   | Many → 1    | `cart_items.food_item_id`     |
| Order **HAS** Order Items            | 1 → Many    | `order_items.order_id`        |
| Order Item **REFERENCES** Food Item  | Many → 1    | `order_items.food_item_id`    |
| Order **HAS** Payment                | 1 → 1       | `payments.order_id` (UNIQUE)  |

---

## Key Design Decisions

1. **Price Snapshots** — `unit_price` is stored in both `cart_items` and `order_items`.
   This protects historical order data if a restaurant changes a food item's price later.

2. **Cart locked to one restaurant** — `carts.restaurant_id` enforces that a user
   cannot mix items from different restaurants in one cart (common in real apps).

3. **Delivery agent as a User role** — Rather than a separate table, agents are
   `users` with `role = 'DELIVERY_AGENT'`. This keeps auth unified.

4. **Soft deletes via `is_active`** — Users, Restaurants, and Food Items use
   `is_active` instead of hard deletes to preserve referential integrity in orders.

5. **ENUM for status fields** — `orders.status` and `payments.status` use ENUM
   so MySQL enforces valid values at the database level.

6. **Payment gateway response stored** — `payments.gateway_response` stores the
   raw JSON from the payment gateway for debugging and dispute resolution.
