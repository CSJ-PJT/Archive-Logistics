# i18n Manual Checklist

Target URL: `http://localhost:8092/`

## Locale Persistence

- Select `한국어`, refresh, and confirm the UI remains Korean.
- Select `English`, refresh, and confirm the UI remains English.
- Select `日本語`, refresh, and confirm the UI remains Japanese.
- Select `简体中文`, refresh, and confirm the UI remains Simplified Chinese.
- Confirm `localStorage.getItem("archive.locale")` matches the selected locale.
- Confirm unsupported stored values fall back to `ko`.

## Dashboard Checks

- Header subtitle changes by locale.
- Buttons change by locale:
  - Refresh
  - Simulate 100
  - Publish Outbox
  - Run Nexus Settlement
- Status strip labels change by locale.
- Pipeline descriptions change by locale.
- Operations summary metric labels change by locale.
- Route / cost filter labels and Apply button change by locale.
- Outbox card, legend, and total event label change by locale.
- Ledger publish labels change by locale.
- Nexus daily settlement labels change by locale.
- Risk signal labels change by locale.
- Activity log titles and generated messages change by locale after a new action.

## Non-Translated Contract Values

Confirm the following remain literal:

- Service names: `Archive-Logistics`, `Archive-Nexus`, `Archive-Ledger`, `ArchiveOS`
- Synthetic factory codes: `FAC-A`, `FAC-B`, `FAC-C`
- API endpoint and port text
- Event/status/domain codes when shown as raw data
- `KRW`

## Responsive Check

- At desktop width, language selector shows the current language name.
- At mobile width, language selector still shows the selected language and wraps without overlapping action buttons.

## Smoke Commands

```powershell
curl.exe http://localhost:8092/actuator/health
curl.exe http://localhost:8092/api/operations/summary
curl.exe http://localhost:8092/manifest.json
curl.exe -I http://localhost:8092/favicon.ico
```
