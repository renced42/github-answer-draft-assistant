# Változásnapló

## 2026-09-02 – Privát, GitHub Issues-alapú ellenőrzött tudástár

- A választervezet opcionálisan privát review issue-ként is létrejön a konfigurált tudástár-repositoryban.
- Az email tartalmazza az eredeti kérdés és a privát review issue közvetlen linkjét.
- A rendszer kizárólag az `approved-knowledge` címkés, kitöltött végleges választ használja későbbi RAG-forrásként.
- A `needs-correction`, `rejected` és `outdated` címkék kizárják az issue-t a visszakeresésből.
- Ismételt workflow-futtatáskor a rendszer felismeri a már létező review issue-t, és nem hoz létre másolatot.
- A Groq-hívás valódi `system` és `user` üzenetre váltott; a rendszer- és forráshűségi utasítások külön rendszerüzenetben szerepelnek.
- A prompt külön kezeli az eNyugta, Online pénztárgép, Online Számla és eÁFA rendszereket.
- Elkészült a privát tudástár manuális bejegyzéseihez használható GitHub Issue Form.
- Az Action `actions/setup-java@v6` verzióra frissült.

## 2026-09-02 – Groq `output_parse_failed` kezelés

- A hibásan formázott böngészős eszközhívás nem állítja le azonnal a workflow-t.
- A program egyszer automatikusan megismétli a kötelező böngészős keresést.
- Ismételt hiba esetén böngésző nélküli Groq-hívásra vált, felhasználva az előzetesen összegyűjtött GitHub- és NAV-webforrásokat.
- A csökkentett internetes forrásfeltárásról figyelmeztetés kerül az emailbe.

## 2026-09-02 – GitHub Search API 429 kezelés

- A GitHub Search API `429` válasza már nem állítja le a teljes workflow-t.
- Sebességkorlát esetén a program folytatja a NAV-webes és a Groq böngészős keresést.
- Az email figyelmeztet, ha a GitHub-források csak részlegesen voltak elérhetők.
- A keresési terhelés legfeljebb két keresőkifejezésre és keresésenként hat kódtalálatra csökkent.
- A workflow-napló jelzi, hogy a GitHub API-hitelesítés aktív-e, de tokent nem ír ki.
- Az `ORG_READ_TOKEN` ajánlott GitHub Environment Secret lett; hiányában továbbra is a beépített `GITHUB_TOKEN` használatos.
