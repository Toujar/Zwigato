# Owner Portal — Quick Start

## What was built

A complete **Restaurant Owner Portal** where owners can:

1. **Manage Restaurants** — Add, edit, delete, and toggle open/closed status
2. **Manage Menu** — Add/edit/delete food items, set prices, mark items unavailable, veg/non-veg flags
3. **Track Orders** — View incoming orders and update status (PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY)
4. **Dashboard Overview** — See stats: total restaurants, active orders, revenue

## Role-based login redirect

After login, users are automatically redirected based on their role:

- **RESTAURANT_OWNER** → `/dashboard/restaurants`
- **ADMIN** → `/dashboard`
- **CUSTOMER** → Home page or the page they came from

## Test credentials

| Email | Password | Role |
|---|---|---|
| `raj@owner.com` | `Password@1` | RESTAURANT_OWNER |
| `priya@owner.com` | `Password@1` | RESTAURANT_OWNER |
| `admin@zwigato.com` | `Password@1` | ADMIN |

## Pages built

| Route | Description |
|---|---|
| `/dashboard` | Owner overview (stats, quick links, active orders preview) |
| `/dashboard/restaurants` | List all restaurants, add/edit/delete, toggle open/closed |
| `/dashboard/menu/:restaurantId` | Manage food items for a restaurant |
| `/dashboard/orders` | View all orders, filter by status, advance order status |

## How to test

1. **Start backend:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Start frontend:**
   ```bash
   cd frontend
   npm run dev
   ```

3. **Login as owner:**
   - Go to `http://localhost:5173/login`
   - Email: `raj@owner.com`
   - Password: `Password@1`
   - You'll be redirected to `/dashboard/restaurants`

4. **Add a restaurant:**
   - Click "Add Restaurant"
   - Fill the form (name, city, address, phone, etc.)
   - Click "Create Restaurant"

5. **Manage menu:**
   - Click the "🍽 Menu" button on a restaurant card
   - Click "Add Item"
   - Fill item details (name, price, category, veg flag, image URL)
   - Click "Add Item"
   - Toggle availability, edit, or delete items

6. **View orders:**
   - Go to `/dashboard/orders`
   - Filter by status
   - Click "Mark as CONFIRMED" → "Mark as PREPARING" → "Mark as OUT_FOR_DELIVERY"

## Features

✅ **Full CRUD** for restaurants and food items  
✅ **Image previews** when adding/editing items  
✅ **Veg/Non-veg filtering** on menu page  
✅ **Status workflow** for orders (PLACED → CONFIRMED → PREPARING → OUT_FOR_DELIVERY)  
✅ **Glass UI theme** — matches the customer-facing pages  
✅ **Mobile responsive** — sidebar drawer on mobile  
✅ **Role-based access** — only RESTAURANT_OWNER and ADMIN can access `/dashboard`  

## What's NOT included (future scope)

- Image upload (currently uses image URLs)
- Real-time order notifications
- Analytics/charts for revenue
- Owner-specific order filtering (currently shows all orders — backend returns all)
- Multi-restaurant owners seeing only their restaurants (backend doesn't filter by ownerId yet)

## Backend notes

The backend endpoints already exist and are working:

- `POST /restaurants` — create restaurant
- `PUT /restaurants/:id` — update
- `DELETE /restaurants/:id` — delete
- `PATCH /restaurants/:id/toggle-open` — toggle status
- `POST /food-items` — add menu item
- `PUT /food-items/:id` — update item
- `DELETE /food-items/:id` — remove item
- `PATCH /food-items/:id/toggle-availability` — toggle available
- `PATCH /orders/:id/status` — update order status

All endpoints are protected by JWT and role-checked with `@PreAuthorize`.
