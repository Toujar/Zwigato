# 🤝 Contributing to Zwigato

First off, thank you for considering contributing to Zwigato! It's people like you that make Zwigato such a great platform.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)

---

## 📜 Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code.

### Our Standards

- ✅ Be respectful and inclusive
- ✅ Welcome newcomers and help them learn
- ✅ Accept constructive criticism gracefully
- ✅ Focus on what is best for the community
- ❌ No harassment, trolling, or insulting comments
- ❌ No political or off-topic discussions

---

## 🎯 How Can I Contribute?

### 1. Report Bugs 🐛

Found a bug? Please create an issue with:
- Clear title and description
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable
- Environment details (OS, browser, versions)

### 2. Suggest Features ✨

Have an idea? Create a feature request with:
- Clear description of the feature
- Why it would be useful
- Possible implementation approach
- Any examples from other apps

### 3. Write Documentation 📚

Help improve:
- README files
- Code comments
- Setup guides
- API documentation
- Tutorial content

### 4. Submit Code 💻

Fix bugs or implement features:
- Follow our coding standards
- Write tests for new code
- Update documentation
- Submit a pull request

### 5. Review Pull Requests 👀

Help review others' contributions:
- Test the changes locally
- Provide constructive feedback
- Approve or request changes

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- MySQL 8.0+
- Git
- Your favorite IDE (IntelliJ IDEA, VS Code, etc.)

### Setup Development Environment

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/zwigato.git
   cd zwigato
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/zwigato.git
   ```
4. **Follow the setup guide**: See [SETUP_GUIDE.md](SETUP_GUIDE.md)

### Keep Your Fork Synced

```bash
# Fetch upstream changes
git fetch upstream

# Merge into your main branch
git checkout main
git merge upstream/main

# Push to your fork
git push origin main
```

---

## 🔄 Development Workflow

### 1. Create a Feature Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a new branch
git checkout -b feature/your-feature-name
# or
git checkout -b fix/bug-description
```

**Branch naming conventions:**
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Adding tests
- `chore/` - Maintenance tasks

### 2. Make Your Changes

- Write clean, readable code
- Follow coding standards (see below)
- Add tests for new functionality
- Update documentation

### 3. Test Your Changes

**Backend:**
```bash
cd backend
mvn test
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm run lint
npm run build
npm run dev
```

### 4. Commit Your Changes

```bash
git add .
git commit -m "feat: add restaurant search functionality"
```

See [Commit Guidelines](#commit-guidelines) below.

### 5. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 6. Create Pull Request

- Go to your fork on GitHub
- Click "New Pull Request"
- Fill in the PR template
- Wait for review

---

## 📝 Coding Standards

### Backend (Java/Spring Boot)

#### General Guidelines

- ✅ Follow **Java Code Conventions**
- ✅ Use **meaningful variable names**
- ✅ Keep methods **small and focused**
- ✅ Write **self-documenting code**
- ✅ Add **comments for complex logic**
- ✅ Use **Lombok annotations** to reduce boilerplate
- ✅ Follow **SOLID principles**

#### Naming Conventions

```java
// Classes: PascalCase
public class RestaurantService { }

// Methods: camelCase
public Restaurant findRestaurantById(Long id) { }

// Variables: camelCase
private String restaurantName;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRY_ATTEMPTS = 3;

// Packages: lowercase
package com.fooddelivery.service;
```

#### Code Style

```java
// ✅ GOOD: Clear and concise
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    
    public Order createOrder(OrderRequest request) {
        validateRequest(request);
        Order order = buildOrder(request);
        return orderRepository.save(order);
    }
    
    private void validateRequest(OrderRequest request) {
        if (request.getItems().isEmpty()) {
            throw new InvalidOrderException("Order must contain items");
        }
    }
}

// ❌ BAD: Complex and unclear
public class OrderService {
    public Order createOrder(OrderRequest r) {
        if (r.getItems().isEmpty()) throw new InvalidOrderException("Order must contain items");
        Order o = new Order(); o.setRestaurantId(r.getRestaurantId()); 
        o.setUserId(r.getUserId()); o.setItems(r.getItems().stream()
        .map(i -> { OrderItem oi = new OrderItem(); oi.setFoodItemId(
        i.getFoodItemId()); oi.setQuantity(i.getQuantity()); return oi; })
        .collect(Collectors.toList())); return orderRepository.save(o);
    }
}
```

#### Controller Best Practices

```java
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    
    private final RestaurantService restaurantService;
    
    // ✅ GOOD: Clear endpoint, proper response
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(
            @PathVariable Long id) {
        RestaurantResponse response = restaurantService.findById(id);
        return ResponseEntity.ok(response);
    }
    
    // ✅ GOOD: Validation, error handling
    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest request) {
        RestaurantResponse response = restaurantService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### Service Layer Best Practices

```java
// ✅ GOOD: Interface + Implementation
public interface RestaurantService {
    RestaurantResponse findById(Long id);
    RestaurantResponse create(RestaurantRequest request);
}

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {
    
    private final RestaurantRepository repository;
    private final RestaurantMapper mapper;
    
    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse findById(Long id) {
        log.info("Finding restaurant with id: {}", id);
        Restaurant restaurant = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Restaurant not found with id: " + id));
        return mapper.toResponse(restaurant);
    }
}
```

### Frontend (React/JavaScript)

#### General Guidelines

- ✅ Use **functional components** with hooks
- ✅ Follow **React best practices**
- ✅ Use **ESLint** and **Prettier**
- ✅ Keep components **small and focused**
- ✅ Use **descriptive component names**
- ✅ Prefer **composition over inheritance**
- ✅ Use **PropTypes** or TypeScript for type checking

#### Component Structure

```jsx
// ✅ GOOD: Clear structure
import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import restaurantService from '../services/restaurantService'

const RestaurantCard = ({ restaurantId, onSelect }) => {
  const [restaurant, setRestaurant] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchRestaurant()
  }, [restaurantId])

  const fetchRestaurant = async () => {
    try {
      const data = await restaurantService.getById(restaurantId)
      setRestaurant(data)
    } catch (error) {
      console.error('Failed to fetch restaurant:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <LoadingSpinner />
  if (!restaurant) return <ErrorMessage />

  return (
    <div className="restaurant-card" onClick={() => onSelect(restaurant)}>
      <img src={restaurant.image} alt={restaurant.name} />
      <h3>{restaurant.name}</h3>
      <p>{restaurant.description}</p>
    </div>
  )
}

RestaurantCard.propTypes = {
  restaurantId: PropTypes.number.isRequired,
  onSelect: PropTypes.func.isRequired
}

export default RestaurantCard
```

#### Naming Conventions

```jsx
// Components: PascalCase
const RestaurantCard = () => { }

// Functions: camelCase
const handleClick = () => { }

// Constants: UPPER_SNAKE_CASE
const API_BASE_URL = process.env.VITE_API_BASE_URL

// CSS Classes: kebab-case
<div className="restaurant-card"></div>
```

#### Hooks Best Practices

```jsx
// ✅ GOOD: Custom hook
const useRestaurant = (restaurantId) => {
  const [restaurant, setRestaurant] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchRestaurant = async () => {
      try {
        setLoading(true)
        const data = await restaurantService.getById(restaurantId)
        setRestaurant(data)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    fetchRestaurant()
  }, [restaurantId])

  return { restaurant, loading, error }
}

// Usage
const RestaurantDetails = ({ id }) => {
  const { restaurant, loading, error } = useRestaurant(id)
  
  // Component logic...
}
```

#### State Management

```jsx
// ✅ GOOD: Organized state
const [formData, setFormData] = useState({
  name: '',
  email: '',
  phone: ''
})

const handleInputChange = (e) => {
  const { name, value } = e.target
  setFormData(prev => ({
    ...prev,
    [name]: value
  }))
}

// ❌ BAD: Separate states for related data
const [name, setName] = useState('')
const [email, setEmail] = useState('')
const [phone, setPhone] = useState('')
```

### CSS/Tailwind

```jsx
// ✅ GOOD: Semantic, reusable classes
<button className="btn-primary">
  Click Me
</button>

// ✅ GOOD: Tailwind utilities
<div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-md">
  <img className="w-12 h-12 rounded-full" src={avatar} />
  <div className="flex-1">
    <h3 className="text-lg font-semibold">{name}</h3>
    <p className="text-sm text-gray-500">{role}</p>
  </div>
</div>

// ❌ BAD: Inline styles
<div style={{ display: 'flex', padding: '16px' }}>
  Content
</div>
```

---

## 📝 Commit Guidelines

We follow the **Conventional Commits** specification.

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, missing semicolons, etc.)
- **refactor**: Code refactoring
- **test**: Adding or updating tests
- **chore**: Maintenance tasks, dependency updates

### Examples

```bash
# Feature
git commit -m "feat(restaurant): add search functionality with filters"

# Bug fix
git commit -m "fix(cart): resolve quantity update issue"

# Documentation
git commit -m "docs(readme): update setup instructions"

# Style
git commit -m "style(order): fix code formatting and indentation"

# Refactor
git commit -m "refactor(auth): simplify JWT token generation"

# Test
git commit -m "test(payment): add unit tests for payment service"

# Chore
git commit -m "chore(deps): update spring boot to 3.2.5"
```

### Commit Message Rules

- ✅ Use imperative mood ("add" not "added" or "adds")
- ✅ Don't capitalize first letter
- ✅ No period at the end
- ✅ Keep subject line under 50 characters
- ✅ Use body to explain *what* and *why*, not *how*

---

## 🔄 Pull Request Process

### Before Submitting

1. ✅ **Test your changes** thoroughly
2. ✅ **Run linter** and fix warnings
3. ✅ **Update documentation** if needed
4. ✅ **Add tests** for new features
5. ✅ **Rebase** on latest main branch
6. ✅ **Squash commits** if needed

### PR Title Format

Follow the same format as commit messages:

```
feat(restaurant): add advanced search with filters
fix(order): resolve payment confirmation bug
docs(api): update authentication endpoints
```

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## How Has This Been Tested?
Describe testing performed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] All tests passing
- [ ] No new warnings

## Screenshots (if applicable)
Add screenshots here

## Related Issues
Closes #123
```

### Review Process

1. **Automated checks** run (linting, tests)
2. **Code review** by maintainers
3. **Changes requested** (if needed)
4. **Approval** by at least one maintainer
5. **Merge** to main branch

---

## 🐛 Reporting Bugs

### Before Reporting

1. **Search existing issues** - it might already be reported
2. **Try latest version** - it might be fixed already
3. **Check documentation** - it might be expected behavior

### Bug Report Template

```markdown
## Bug Description
Clear description of the bug

## Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## Expected Behavior
What you expected to happen

## Actual Behavior
What actually happened

## Screenshots
If applicable, add screenshots

## Environment
- OS: [e.g., Windows 11]
- Browser: [e.g., Chrome 120]
- Java Version: [e.g., 21]
- Node Version: [e.g., 18.17]

## Additional Context
Any other context about the problem
```

---

## ✨ Suggesting Features

### Feature Request Template

```markdown
## Feature Description
Clear description of the feature

## Problem It Solves
What problem does this solve?

## Proposed Solution
How would you implement it?

## Alternatives Considered
What other solutions did you consider?

## Additional Context
Mockups, examples, references, etc.
```

---

## 🎨 UI/UX Contributions

### Design Guidelines

- Follow existing design system
- Maintain consistency with current UI
- Ensure responsive design
- Test on multiple devices
- Follow accessibility guidelines (WCAG 2.1)

### Before Submitting UI Changes

- [ ] Test on desktop (Chrome, Firefox, Safari)
- [ ] Test on mobile (iOS, Android)
- [ ] Test with keyboard navigation
- [ ] Test with screen reader (if applicable)
- [ ] Check color contrast ratios
- [ ] Verify all interactions work

---

## 📚 Documentation Contributions

Help improve:

- **README.md** - Project overview
- **SETUP_GUIDE.md** - Setup instructions
- **API documentation** - Swagger/OpenAPI docs
- **Code comments** - Inline documentation
- **Wiki pages** - Tutorials and guides

### Documentation Guidelines

- Use clear, simple language
- Include code examples
- Add screenshots where helpful
- Keep content up-to-date
- Proofread for typos

---

## 🏆 Recognition

Contributors will be:

- ✨ Listed in CONTRIBUTORS.md
- 🎖️ Credited in release notes
- 💙 Thanked in community channels

---

## 📞 Questions?

- 💬 Join our [Discord](https://discord.gg/zwigato)
- 📧 Email: dev@zwigato.com
- 🐦 Twitter: [@zw igato_dev](https://twitter.com/zwigato_dev)

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

---

Thank you for contributing to Zwigato! 🎉🚀
