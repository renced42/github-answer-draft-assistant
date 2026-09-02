# Változásnapló

## 2026-09-02 – GitHub Search API 429 kezelés

- A GitHub Search API `429` válasza már nem állítja le a teljes workflow-t.
- Sebességkorlát esetén a program folytatja a NAV-webes és a Groq böngészős keresést.
- Az email figyelmeztet, ha a GitHub-források csak részlegesen voltak elérhetők.
- A keresési terhelés legfeljebb két keresőkifejezésre és keresésenként hat kódtalálatra csökkent.
- A workflow-napló jelzi, hogy a GitHub API-hitelesítés aktív-e, de tokent nem ír ki.
- Az `ORG_READ_TOKEN` ajánlott GitHub Environment Secret lett; hiányában továbbra is a beépített `GITHUB_TOKEN` használatos.
