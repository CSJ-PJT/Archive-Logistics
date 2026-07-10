# i18n Audit

Date: 2026-07-10

## Scope

Archive-Logistics uses a Spring Boot static operations dashboard under `src/main/resources/static`.
The i18n work is limited to user-visible UI copy. API contracts, event codes, enum values, package/class names, and database identifiers were not changed.

## Implemented Structure

- `src/main/resources/static/i18n.js`
  - Defines supported locales: `ko`, `en`, `ja`, `zh-CN`
  - Stores the selected locale in `localStorage` under `archive.locale`
  - Falls back to `ko` for unsupported or missing locale values
  - Provides `t()`, locale normalization, locale names, and missing-key audit helper
- `src/main/resources/static/index.html`
  - Uses `data-i18n`, `data-i18n-title`, and `data-i18n-aria-label`
  - Keeps service names and system identifiers unchanged
- `src/main/resources/static/app.js`
  - Applies translations at runtime
  - Re-renders dynamic labels after language changes
  - Localizes status labels, action logs, numbers, time, and KRW display wrappers

## Handled Screens / Components

- Top navigation and language selector
- Action buttons: refresh, simulation, outbox publish, Nexus settlement
- Service status strip
- Pipeline stages
- Operations summary card
- Route / cost card and filter labels
- Outbox state card, legend, and event count label
- Ledger publish card
- Nexus daily settlement card
- Risk signals card
- Activity log titles and action summaries
- Browser page title
- Accessibility labels for key controls and sections

## Remaining Hardcoded UI Text

- `Archive-Logistics`, `Archive-Nexus`, `Archive-Ledger`, `ArchiveOS`
  - Reason: product/service names are proper nouns and should not be translated.
- `Outbox`, `Ledger`, `route`, `ETA`, `cost`, `DRY_RUN`, `PUBLISHED`
  - Reason: these are system/domain terms used in the product and event contract. UI status labels are translated where helpful, but raw event/status values remain unchanged in API payloads.
- `FAC-A`, `FAC-B`, `FAC-C`, `KRW`
  - Reason: synthetic factory codes and currency code.
- API endpoint text displayed in the Ledger endpoint field
  - Reason: API paths and ports are contract/debug information and should remain literal.
- Server error detail text
  - Reason: API contract is unchanged. The UI translates the error/action prefix and leaves raw server detail for debugging.

## Translation Exclusions

The following categories remain untranslated by design:

- API URLs and paths
- eventType values such as `LOGISTICS_DISPATCHED`
- enum/status code values in payloads such as `APPROVAL_REQUIRED`, `SETTLEMENT_READY`, `PENDING`, `PUBLISHED`
- DB table/column names
- Java package/class/repository names
- traceId and correlationId
- GitHub links, file paths, ports, and commands

## Missing Key Audit

The translation table was checked with a Node VM script:

```text
ko: 92 / 92
en: 92 / 92
ja: 92 / 92
zh-CN: 92 / 92
missing: none
```

## Future Improvements

- Add automated browser assertions for every `data-i18n` element.
- Add per-locale screenshot comparison in CI if a browser runner is introduced.
- Move very large translation tables into split files if the static dashboard grows beyond a single page.
