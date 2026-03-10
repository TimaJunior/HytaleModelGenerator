# MEMORY.md - Long-Term Memory for Naoya Zenin

## 📜 Project Constitution & Principles

This project follows the **Hytale Model Generator Development Constitution**. These are the foundational guidelines for all work here.

### 🏛️ Core Engineering (SWE)
- **SOLID Compliance**: Strict adherence to SOLID principles.
- **Layered Design**: Separation between `core`, `application`, `infrastructure`, and `presentation`.
- **Dependency Inversion**: Use dependency injection.
- **Simplicity**: Follow **KISS**, **YAGNI**, and **DRY**.

### 🧪 Development Workflow
- **TDD (Test-Driven Development)**: Follow the Red-Green-Refactor cycle. Test first, then implement.
- **Testing Pyramid**: Focus on Unit Tests (70%), followed by Integration (20%) and E2E (10%).

### 🇺🇦 Documentation & Quality
- **Language**: All documentation, comments, and artifacts must be in **Ukrainian**.
- **Type Safety**: Adhere to best JS/TS practices.
- **Docstrings**: Public functions and modules must be documented.

### 🔍 Standards
- **Architectural Scoring**: Aim for 9-10 on the scale (loosely coupled, cohesive).
- **Pragmatism**: Avoid over-engineering; explain the "Why" behind refactors.

### 🎮 Hytale & UI
- **Block-Based Thinking**: Generative logic must respect voxel/block nature.
- **Optimization**: Optimize GAN processing and 3D rendering for web.
- **Premium Design**: Interfaces must be dynamic, smooth, and support dark mode.

## 🤖 Telegram Bot Cloner (TBC) - Архітектура та План
Додано 2026-02-01 на запит Тимофія для майбутніх сесій.

### 🛠 Технічна архітектура:
1.  **Парсер (Scanner) - Telethon/Pyrogram:**
    *   Використання Userbot для взаємодії з цільовими ботами.
    *   Автоматичний обхід дерева меню (BFS/DFS).
    *   Парсинг: тексти, кнопки (Inline/Reply), медіа (фото/відео/документи).
2.  **Реєстр станів (State Registry):**
    *   Зберігання структури меню у вигляді графа (JSON/SQLite).
    *   Мапінг переходів: `[State] + [Button] -> [New State]`.
3.  **Реплікатор (Replicator) - Aiogram:**
    *   Відтворення інтерфейсу та логіки на основі зібраних даних.

### 📋 План реалізації:
1.  Створення модуля `bot_cloner` (SOLID/Constitution).
2.  Інтеграція існуючого коду Тимофія.
3.  Налаштування Telegram API (ID/Hash).
4.  Запуск циклу рекурсивного парсингу.

---
*Last Updated: 2026-02-01*

## 🚀 Теперішній стан проєкту (Feb 2026)
- **Архітектура**: Python GAN (Encoder: ResNet18, Generator: 3D Transposed Conv, Discriminator: 3D CNN).
- **Дані**: Датасет із 500 семплів (зображення 256x256 + вокселі 32x32x32).
- **Стек**: Next.js + Three.js для веб-візуалізації.
- **Поточне завдання**: Пошук та збір якісних 3D моделей Hytale для навчання AI.
- **Нагляд**: Naoya Zenin координує роботу через групу `clawbot workspace` (у процесі підключення).
