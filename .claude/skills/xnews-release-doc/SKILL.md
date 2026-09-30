---
name: xnews-release-doc
description: Produce an X-NEWS version document as a PDF (e.g. V4 detailed technical documentation) in the house style of the V2/V3 documents and the X-NEWS frontend colours. Use when asked for a release, version, project or technical documentation PDF for X-NEWS.
---

# X-NEWS release document (PDF)

No PDF library is installed and adding one needs approval, so build HTML and print it with the Edge browser that is already installed.

## Structure (same as V2 and V3)
Cover (wordmark "X-NEWS" with red hyphen, eyebrow, title, subtitle, **Document status** table, 8 stack tiles, scope note) → Contents → numbered sections, each starting on a new page with a red "SECTION N" eyebrow → startup-quality checklist → repository/handoff notes → glossary.

References: V3 source HTML is not kept in the repo; the V3 PDF is `C:\Users\DELL\Downloads\X-NEWS_V3_Detailed_Technical_Documentation.pdf`; V2 is `D:\All Files\ShareHub\com.whatsapp.provider.media\DOC-20260825-WA0008.pdf` (read text with `pdftotext -layout`).

## Colours (from `frontend/src/App.css`, light theme)
`--bg #f7f7f5`, `--surface #ffffff`, `--surface-sunken #f1f1ee`, `--text #171717`, `--text-body #444444`, `--text-muted #737373`, `--border #e3e3de`, `--accent #c62828`, `--success #2e7d32` on `#edf7ee`, `--warning #b45309` on `#fff7e8`, `--accent-soft #fff0ef`. Font Inter (Google Fonts), code JetBrains Mono.

## CSS essentials
`@page { size: A4; margin: 17mm 16mm 18mm; @bottom-left { content: "X-NEWS Vn — Detailed Technical Documentation" } @bottom-right { content: "Page " counter(page) } }`, `@page :first` without footers, `print-color-adjust: exact`, `section.chapter { page-break-before: always }`, tables/callouts `page-break-inside: avoid`. Charts as inline SVG using the tokens.

## Print (PowerShell)
```powershell
$html = '<scratchpad>\xnews-vN-doc.html'; $out = 'C:\Users\DELL\Downloads\X-NEWS_VN_Detailed_Technical_Documentation.pdf'
Start-Process 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe' -ArgumentList @('--headless=new','--disable-gpu','--no-pdf-header-footer','--virtual-time-budget=15000',"--user-data-dir=<scratchpad>\edge-print-profile","--print-to-pdf=$out",([Uri]$html).AbsoluteUri) -Wait -WindowStyle Hidden
```
Check: `pdftotext -layout` → page count (`grep -c $'\f'`), footers, first line per page. For a visual check, `--screenshot=<png> --window-size=900,5200` and view it scaled.

## Content rules
Every number must come from something checked in the session (repo, production DB via `xnews-prod-db`, Azure, logs); say explicitly what was not verified. Keep an honest "known limitations" section.
