# 📝 Changelog

All notable changes to the Zwigato Food Delivery Platform will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned Features
- 🔔 Push notifications for order updates
- 🌍 Multi-language support (i18n)
- 🌙 Dark mode theme
- 📊 Advanced analytics dashboard
- 🤖 AI-powered restaurant recommendations
- 🎁 Loyalty program and rewards system
- 📱 Progressive Web App (PWA) support
- 🗺️ Enhanced map integration with route optimization

---

## [1.2.0] - 2026-08-15

### Added - UI Enhancements 🎨
- ✨ **Beautiful animated background blobs** on Register and Login pages
- 🎨 **Input field icons** for better visual hierarchy (user, email, phone, location, lock icons)
- 💪 **Password strength indicator** with real-time feedback (Weak/Fair/Good/Strong)
- 👁️ **Show/hide password toggles** for better user experience
- 🔒 **Trust badges** and security indicators on auth pages
- ➡️ **Enhanced buttons** with icons and smooth animations
- 📝 **Improved form layouts** with better spacing and typography
- 🎭 **Glass-morphism design** maintained throughout

### Added - Documentation 📚
- 📖 Comprehensive **README.md** with detailed project information
- 🚀 Step-by-step **SETUP_GUIDE.md** for easy project setup
- 🤝 **CONTRIBUTING.md** with coding standards and workflows
- 📝 **CHANGELOG.md** to track all project changes
- 🎨 **UI_ENHANCEMENTS.md** documenting all UI improvements
- 💾 **.env.example** file for frontend configuration

### Added - CSS Animations
- 🌊 **Blob animation** with floating effect (7-second cycles)
- ⏱️ **Staggered timing** for multiple background elements
- 🎬 **Smooth transitions** throughout the application
- 🖼️ **Animation classes** for reusable effects

### Improved
- 📱 **Mobile responsiveness** on authentication pages
- ♿ **Accessibility** with proper ARIA labels and keyboard navigation
- 🎯 **User feedback** with password strength validation
- 🔐 **Security indicators** to build user trust

### Technical
- ⚛️ Used **React hooks** (useState, useMemo) for optimal performance
- 🎨 **Tailwind CSS** utilities for consistent styling
- 🚀 **Vite HMR** for instant development feedback
- 💅 **Pure CSS animations** (no external libraries needed)

---

## [1.1.0] - 2026-08-12

### Added - Backend Features
- 📧 **Email OTP verification** for account security
- 📄 **PDF invoice generation** using iTextPDF
- 💳 **Razorpay payment integration** with multiple payment methods
- 🔄 **WebSocket support** for real-time order updates
- 🗄️ **Redis caching** for improved performance
- 📊 **Admin dashboard** with comprehensive analytics
- ⭐ **Review and rating system** for restaurants and food items

### Added - Payment Methods
- 💳 Credit Card
- 💳 Debit Card
- 📱 UPI
- 🏦 Net Banking
- 👛 Wallet
- 💰 Cash on Delivery (COD)
- 🌐 Online Payment

### Added - User Roles
- 👤 **Customer** - Order food and track deliveries
- 🏪 **Restaurant Owner** - Manage restaurants and menus
- 🚚 **Delivery Agent** - Handle deliveries
- 👨‍💼 **Admin** - Full system management

### Fixed
- 🐛 **PaymentMethod enum mismatch** - Added missing ONLINE and COD values
- 🐛 **Database constraint errors** - Fixed duplicate column issues
- 🐛 **Email authentication** - Resolved Gmail App Password issues
- 🐛 **Form input focus loss** - Fixed React component recreation issue

### Database
- 📊 Added **comprehensive seed data** with test accounts
- 🏪 Seeded **10 restaurants** with full menus
- 🍕 Added **45+ food items** across categories
- 👥 Created **9 test users** with different roles
- 📦 Included **sample orders** for testing

### Configuration
- ⚙️ Set `spring.jpa.hibernate.ddl-auto=none` for production safety
- 📧 Added email configuration with Gmail SMTP
- 🔑 Implemented secure JWT token authentication
- 🌐 Configured CORS for frontend-backend communication

---

## [1.0.0] - 2026-08-01

### Initial Release 🎉

#### Backend Features
- ✅ **Spring Boot 3.2.5** REST API
- ✅ **Spring Security** with JWT authentication
- ✅ **Spring Data JPA** with MySQL database
- ✅ **Swagger/OpenAPI** documentation
- ✅ **MapStruct** for DTO mapping
- ✅ **Lombok** for cleaner code
- ✅ **Spring Validation** for input validation
- ✅ **Global exception handling**

#### Frontend Features
- ✅ **React 18** with functional components
- ✅ **Vite** for fast development
- ✅ **React Router** for client-side routing
- ✅ **Axios** for HTTP requests
- ✅ **Tailwind CSS** for styling
- ✅ **React Leaflet** for maps
- ✅ **Context API** for state management

#### Core Functionality
- 🔐 User registration and login
- 🏪 Restaurant browsing and search
- 🍔 Menu viewing with categories
- 🛒 Shopping cart management
- 📦 Order placement and tracking
- 💳 Payment processing
- 🚚 Delivery management
- ⭐ Review and rating system

#### Database Schema
- 👤 Users table with role-based access
- 🏪 Restaurants table with operating hours
- 🍕 Food items with categories
- 🛒 Cart and cart items
- 📦 Orders with status tracking
- 💳 Payments with transaction details
- 🚚 Delivery assignments
- ⭐ Reviews and ratings

---

## Version History

| Version | Date | Description |
|---------|------|-------------|
| **1.2.0** | 2026-08-15 | UI enhancements, documentation, animations |
| **1.1.0** | 2026-08-12 | Email OTP, payments, WebSocket, Redis |
| **1.0.0** | 2026-08-01 | Initial release with core features |

---

## Migration Guides

### Upgrading from 1.1.0 to 1.2.0

No breaking changes. Simply pull the latest code and:

1. **Frontend**: No configuration changes needed
2. **Backend**: No database migrations required
3. **Documentation**: Review new guides for better understanding

### Upgrading from 1.0.0 to 1.1.0

#### Database Changes
```sql
-- Add new payment method enums
ALTER TABLE payments MODIFY COLUMN payment_method ENUM(
    'ONLINE', 'COD', 'CREDIT_CARD', 'DEBIT_CARD', 
    'UPI', 'NET_BANKING', 'WALLET', 'CASH_ON_DELIVERY'
);

-- Add payment tracking columns
ALTER TABLE payments ADD COLUMN max_retries INT NOT NULL DEFAULT 3;
ALTER TABLE payments ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN razorpay_order_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN razorpay_payment_id VARCHAR(255);
```

#### Configuration Changes
```properties
# Add to application.properties

# Email configuration
spring.mail.host=smtp.gmail.com
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Razorpay configuration
razorpay.key.id=your_key_id
razorpay.key.secret=your_secret

# Redis configuration (optional)
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

## Breaking Changes

### Version 1.1.0
- **PaymentMethod Enum**: Added new values. Existing payments may need migration.
- **User Entity**: Added `address` field. Existing users should update profiles.

### Version 1.0.0
- Initial release, no breaking changes

---

## Security Updates

### Version 1.2.0
- ✅ Enhanced password validation with strength indicator
- ✅ Added visual security indicators for user confidence
- ✅ Maintained secure JWT token authentication

### Version 1.1.0
- ✅ Added email OTP verification
- ✅ Implemented secure payment gateway integration
- ✅ Enhanced JWT token security

### Version 1.0.0
- ✅ JWT-based authentication
- ✅ Password encryption with BCrypt
- ✅ Role-based access control
- ✅ Input validation and sanitization

---

## Known Issues

### Current
- 📱 PWA support not yet implemented
- 🌍 Only English language supported
- 🗺️ Basic map integration (room for enhancement)

### Fixed
- ~~🐛 Form input loses focus after one character~~ (Fixed in 1.1.0)
- ~~🐛 Email authentication fails with regular password~~ (Fixed in 1.1.0)
- ~~🐛 PaymentMethod enum mismatch~~ (Fixed in 1.1.0)

---

## Performance Improvements

### Version 1.2.0
- ⚡ Optimized password strength calculation with useMemo
- ⚡ GPU-accelerated CSS animations
- ⚡ Reduced bundle size with tree-shaking

### Version 1.1.0
- ⚡ Added Redis caching for frequent queries
- ⚡ Optimized database queries with indexes
- ⚡ Implemented lazy loading for images

---

## Contributors

Special thanks to all contributors who helped make Zwigato better!

### Version 1.2.0
- [@yourusername](https://github.com/yourusername) - UI enhancements and documentation

### Version 1.1.0
- [@yourusername](https://github.com/yourusername) - Payment integration and email features

### Version 1.0.0
- [@yourusername](https://github.com/yourusername) - Initial project setup and core features

---

## Feedback & Support

- 🐛 **Report bugs**: [GitHub Issues](https://github.com/yourusername/zwigato/issues)
- ✨ **Request features**: [Feature Requests](https://github.com/yourusername/zwigato/issues/new?template=feature_request.md)
- 💬 **Get help**: [Discord Community](https://discord.gg/zwigato)
- 📧 **Email**: support@zwigato.com

---

## Links

- 📖 [Documentation](README.md)
- 🚀 [Setup Guide](SETUP_GUIDE.md)
- 🤝 [Contributing](CONTRIBUTING.md)
- 📄 [License](LICENSE)
- 🌐 [Website](https://zwigato.com)

---

**Legend:**
- ✨ New Feature
- 🐛 Bug Fix
- 📚 Documentation
- 🎨 UI/UX
- ⚡ Performance
- 🔒 Security
- 🔧 Configuration
- 🗑️ Deprecated
- ❌ Breaking Change

---

*Last Updated: August 15, 2026*
