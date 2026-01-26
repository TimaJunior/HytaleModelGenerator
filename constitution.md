# Hytale Model Generator Development Constitution

This document defines the core principles, standards, and workflows for this project. It is synthesized from the system's core skills and best practices.

---

## 🏛️ Core Engineering Principles (SWE)

### 1. Architectural Integrity
- **SOLID Compliance**: All code must strictly follow the five SOLID principles.
- **Layered Design**: Maintain clear boundaries between:
  - `src/core`: Domain and Business Logic (Enterprise Rules)
  - `src/application`: Use Cases and Services (Application Rules)
  - `src/infrastructure`: Data Access, API Clients, External Tools (Interface Adapters)
  - `src/presentation`: UI Components and Hooks (Framework specific)
- **Dependency Inversion**: Dependencies must be injected, not hardcoded.

### 2. Radical Simplicity
- **KISS**: Favor simple, readable solutions over complex ones.
- **YAGNI**: Implement only what is required for the current task.
- **DRY**: Abstract repeating patterns into reusable utilities, but avoid premature abstraction.

### 3. Документація та якість
- **Мова**: Уся документація, коментарі в коді (за можливості) та артефакти розробки мають бути написані **українською мовою**.
- **Типобезпека**: Суворе дотримання кращих практик JavaScript (або TypeScript, де це можливо).
- **Docstrings**: Усі публічні функції та модулі повинні мати описову документацію.

---

## 🧪 Testing & Validation Workflow (TDD)

### 1. The Red-Green-Refactor Cycle
- No high-level production logic should be written without a corresponding test.
- **Red**: Write a failing test first.
- **Green**: Write the minimal code to pass.
- **Refactor**: Clean up the code without breaking the test.

### 2. Testing Pyramid
- **Unit (70%)**: Test functions in isolation.
- **Integration (20%)**: Test module interactions.
- **E2E (10%)**: Test the full user flow.
Remember:Test first, then implement.

---

## 🔍 Code Review & Health Standards

### 1. Architectural Scoring
Every major feature should be evaluated against the 1-10 scale:
- **9-10**: Loosely coupled, highly cohesive, easily testable.
- **1-2**: Spaghetti code requiring a rewrite.

### 2. Pragmatism Filter
- Avoid over-engineering small scripts. SOLID principles should serve the project's maintainability, not add unnecessary noise.
- Always explain the "Why" behind a refactor.

---

## 🎮 Hytale Specific Guidelines
- **Block-Based Thinking**: All generative logic must respect the voxel/block nature of Hytale.
- **Performance**: GAN processing and 3D rendering should be optimized for the web environment.
- **SEO & UX**: Web interfaces must follow the "Premium Design" guidelines (smooth animations, dark mode support).
