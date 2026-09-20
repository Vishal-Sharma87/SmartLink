Yes. I’d make these **project documentation files**, not frontend runtime files. They become the persistent source of
truth that you and Codex can refer to during the whole frontend build.

# 1. `docs/frontend-design.md`

````md
# SmartLink Frontend Design System

## 1. Purpose

The SmartLink frontend is a supporting web interface for the SmartLink Spring Boot application.

The backend is the core of the project. The frontend exists to make the existing functionality accessible,
understandable, and pleasant to use.

The frontend should therefore be:

- clean
- minimal
- calm
- technical
- professional
- consistent
- responsive
- easy to maintain

The UI should not attempt to become a separate frontend application or compete with the backend as the primary
engineering focus.

---

## 2. Core Visual Principle

Every visual component should complement the surrounding components rather than compete with them.

Visual hierarchy should primarily come from:

- spacing
- typography
- contrast
- alignment
- component size
- restrained use of color

Avoid creating visual hierarchy through excessive:

- gradients
- shadows
- colors
- decorative elements
- animations
- oversized cards
- rounded/pill-shaped controls

The interface should feel intentionally designed rather than heavily decorated.

---

## 3. Color System

SmartLink uses a dark-neutral foundation rather than a white-dominant interface.

### Base colors

| Purpose | Color |
|---|---|
| Page background | `#171820` |
| Primary surface | `#1E202A` |
| Secondary surface | `#252834` |
| Elevated surface | `#2B2E3A` |
| Border | `#343846` |
| Primary text | `#F1F1F3` |
| Secondary text | `#A7A9B4` |
| Muted text | `#777B88` |
| Primary accent | `#8B6CFF` |
| Accent hover/active | `#7554E8` |

### Semantic colors

| Purpose | Color |
|---|---|
| Success | `#4ADE80` |
| Warning | `#FBBF24` |
| Error | `#F87171` |
| Information | `#60A5FA` |

Semantic colors communicate state only. They should not be used as decorative colors.

### Color usage rule

Use:

- neutral colors for structure
- purple for interaction and SmartLink identity
- semantic colors for system state

Do not make every component purple.

---

## 4. Typography

Primary font:

```text
Inter, system-ui, sans-serif
````

Typography should be clean and restrained.

### Suggested hierarchy

| Element           | Approximate size |
|-------------------|-----------------:|
| Hero heading      |             48px |
| Page heading      |             32px |
| Section heading   |             24px |
| Component heading |             18px |
| Body              |          15–16px |
| Small/meta        |          13–14px |

### Font weights

* `400` — body text
* `500` — labels/navigation
* `600` — headings/buttons
* `700` — major hero headings

Avoid using very heavy font weights throughout the interface.

---

## 5. Spacing

Use a consistent spacing scale:

```text
4px
8px
12px
16px
24px
32px
48px
64px
80px
```

Prefer these values instead of arbitrary spacing values.

Typical component spacing should use:

```text
8px
12px
16px
24px
32px
```

Larger spacing values should primarily separate major page sections.

Generous whitespace is an important part of the design.

---

## 6. Border Radius

Use moderate corner rounding.

| Element          | Radius |
|------------------|-------:|
| Small controls   |  6–8px |
| Inputs           |    8px |
| Buttons          |    8px |
| Cards            |   12px |
| Large containers |   16px |

Do not turn normal controls into large pills.

---

## 7. Borders and Shadows

Borders should establish structure more often than shadows.

Use subtle borders based on the project's border color.

Shadows should be:

* subtle
* infrequent
* used only when elevation improves comprehension

Avoid large floating shadows.

---

## 8. Buttons

Use three general button levels.

### Primary

The primary action of the current page.

Characteristics:

* SmartLink purple background
* light text
* strongest visual emphasis
* clear hover state
* clear focus state
* disabled/loading state

### Secondary

For actions that are important but not the primary action.

Characteristics:

* dark/neutral surface
* subtle border
* light text

### Ghost

For low-priority actions.

Characteristics:

* minimal visual weight
* no strong background
* subtle hover state

### Button rule

A page should normally have one visually dominant primary action.

Avoid multiple competing primary actions.

---

## 9. Inputs

Inputs should be simple and consistent.

Every input should have:

* clear label
* appropriate semantic type
* consistent height
* subtle border
* visible focus state
* clear validation state
* accessible contrast

Normal input:

* dark surface
* subtle border

Focused input:

* SmartLink accent indication

Invalid input:

* semantic error indication

Do not rely exclusively on color to communicate validation errors.

---

## 10. Cards

Cards are containers for related information, not decorative objects.

General hierarchy:

```text
Page background
    ↓
Primary surface
    ↓
Secondary surface
    ↓
Elevated surface
```

Cards should generally use:

* slightly lighter surface than their surroundings
* subtle border
* moderate radius
* generous internal spacing
* little or no shadow

Do not create a card for every small piece of information.

Use a card when grouping information improves comprehension.

---

## 11. Navigation

Navigation should remain visually lightweight.

The navbar should:

* use the dark-neutral foundation
* have subtle separation from page content
* clearly indicate the active page
* use the SmartLink accent sparingly
* adapt to authenticated and unauthenticated users

The exact navigation items must be determined from the actual application functionality.

Do not invent navigation based solely on this document.

---

## 12. Page Layout

Use a centered content container.

General direction:

```text
max-width: approximately 1200px
horizontal padding: approximately 24px
```

Focused pages such as authentication forms should use a narrower content width.

Pages should have sufficient horizontal and vertical breathing room.

---

## 13. Status Indicators

Status indicators should be compact and restrained.

They should use semantic colors only when communicating actual system state.

Avoid large decorative badges.

The visual treatment should make the state clear without dominating the surrounding content.

---

## 14. Tables

Tables are appropriate when the user needs to compare multiple pieces of structured information.

For example, a link-management page may use a table on desktop.

On smaller screens, the same information may become:

* stacked cards
* responsive rows
* horizontally scrollable content where appropriate

Do not sacrifice usability merely to preserve a desktop table layout.

---

## 15. Analytics

Analytics is one area where visualizations are appropriate.

Analytics pages should prioritize:

1. important metrics
2. trends
3. useful breakdowns
4. supporting details

Avoid filling the page with many competing charts.

Charts should use the SmartLink visual language and restrained accent colors.

---

## 16. Home Page

The home page should:

* explain what SmartLink is
* communicate the primary value
* introduce major capabilities
* provide a clear path into the application
* provide a path toward the project/engineering story

It should not become a large marketing website.

---

## 17. Behind SmartLink

The Behind SmartLink page can provide more visual storytelling while remaining consistent with the main design system.

It may communicate:

* the creator
* why SmartLink was built
* architecture
* engineering decisions
* technology choices
* security
* link processing
* analytics
* challenges
* lessons learned
* project/developer links

Technical claims must reflect the actual implementation.

---

## 18. Responsive Design

The UI must support:

* desktop
* tablet
* mobile

The same visual hierarchy should remain across screen sizes.

Responsive behavior may include:

* collapsing navigation
* converting multi-column layouts to single-column layouts
* converting tables into cards where appropriate
* reducing spacing
* scaling typography

Do not simply shrink desktop content until it becomes difficult to use.

---

## 19. Accessibility

Use semantic HTML.

Ensure:

* inputs have associated labels
* keyboard navigation works
* focus states are visible
* buttons are real buttons
* links are real links
* interactive elements have accessible names
* color contrast is sufficient
* errors are understandable

Do not use color as the only indication of state.

---

## 20. Animation

Animation should be minimal.

Use animation only when it improves:

* feedback
* state transition
* interaction clarity

Avoid decorative animation.

The interface should remain fast and calm.

---

## 21. Design Anti-Patterns

Avoid:

* excessive gradients
* glassmorphism
* giant rounded containers
* excessive pill-shaped controls
* heavy shadows
* excessive purple
* excessive colored cards
* excessive animations
* decorative UI without purpose
* unnecessarily large typography
* multiple competing primary buttons
* dense pages without whitespace
* unrelated functionality combined into one page

---

## 22. Core Design Rule

When making a visual decision, prefer:

```text
simple
    >
decorative

consistent
    >
novel

clear hierarchy
    >
visual complexity

useful interaction
    >
animation

restrained color
    >
many colors
```

The frontend should feel like a polished interface around a technically sophisticated backend.

````
