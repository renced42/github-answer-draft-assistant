# GitHub Answer Draft Assistant – Java 21 + Groq

Nyilvános GitHub Issue vagy Discussion létrehozásakor a rendszer:

1. keres a teljes `nav-gov-hu` GitHub organization nyilvános fájljaiban, issue-iban és discussionjeiben;
2. előnyben részesíti az XSD-, XML-, minta- és specifikációfájlokat, és követi az XSD include/import hivatkozásokat;
3. opcionálisan keres a nyilvános `nav.gov.hu` oldalakon;
4. opcionálisan beolvassa a privát tudástár azonos NAV-rendszerhez tartozó, ember által jóváhagyott válaszait;
5. a Groq modell és böngészős keresés segítségével magyar választervezetet készít;
6. a tervezetet privát review issue-ként rögzíti, és emailben elküldi mindkét GitHub-linket;
7. **az eredeti Issue vagy Discussion alatt soha nem publikál automatikusan**.

Az alkalmazás Java 21, külső Java-függőség és saját szerver nélkül fut GitHub Actionsben. A tesztverzió közvetlenül a `main` ágról használható, ezért nem kell tag vagy GitHub Release.

A teljes, lépésről lépésre követhető beállítás a [TELEPITES.md](TELEPITES.md) fájlban található.

## Privát review és jóváhagyott tudás

Az opcionális `renced42/github-answer-knowledge` privát repository GitHub Issues felülete biztosítja a review-folyamatot. Az automatikus tervezet `knowledge-candidate` címkét kap. A modell később csak az `approved-knowledge` címkés, kitöltött **Ellenőrzött végleges válasz** szakaszt használja.

A `needs-correction`, `rejected` és `outdated` címke kizárja az issue-t. Az `approved-knowledge` hozzáadása egyben engedélyezi, hogy az ellenőrzött tartalom a Groq API-hoz kerüljön.

## 1. Az Action repository frissítése

A ZIP tartalmát másold a meglévő:

`renced42/github-answer-draft-assistant`

repository gyökerébe. Az `action.yml` közvetlenül a repository gyökerében legyen. Commitold és pushold a fájlokat a `main` ágra.

A régi Python-, Gemini- és Workspace Agent-fájlok a működéshez nem kellenek. A biztos átálláshoz érdemes azokat a repositoryból eltávolítani, de a Java Action számára az új `action.yml` és `src/` a meghatározó.

## 2. A tesztrepository GitHub Environment beállítása

A `renced42/github-answer-draft-test` repositoryban nyisd meg:

`Settings → Environments → Github assistant`

Az environment neve pontosan: `Github assistant`.

### Environment variables

| Név | Érték |
|---|---|
| `DRAFT_EMAIL_TO` | `rencenji.denes@gmail.com` (szabadon módosítható) |
| `DRAFT_GROQ_MODEL` | `openai/gpt-oss-120b` |
| `DRAFT_QUESTION_MAX_CHARS` | `3000` |
| `KNOWLEDGE_REPOSITORY` | `renced42/github-answer-knowledge` |
| `KNOWLEDGE_LIMIT` | `5` |

### Environment secrets

| Név | Érték |
|---|---|
| `GROQ_API_KEY` | a már létrehozott Groq API-kulcs |
| `MAIL_FROM` | a feladó email-címe |
| `SMTP_HOST` | Gmail esetén `smtp.gmail.com` |
| `SMTP_PORT` | Gmail STARTTLS esetén `587` |
| `SMTP_USERNAME` | a feladó Gmail-címe |
| `SMTP_PASSWORD` | Gmail alkalmazásjelszó, nem a normál jelszó |
| `SMTP_STARTTLS` | `true` |
| `ORG_READ_TOKEN` | ajánlott, csak olvasási célú GitHub Personal Access Token; ha nincs megadva, a workflow beépített `GITHUB_TOKEN` értékét használja |
| `KNOWLEDGE_REPOSITORY_TOKEN` | a privát tudástárhoz kötött fine-grained PAT, kizárólag `Issues: Read and write` joggal |

Az `AI_API_KEY`, `WORKSPACE_AGENT_TRIGGER_ID`, `WORKSPACE_AGENT_ACCESS_TOKEN` és Gemini-beállítások ehhez a változathoz nem kellenek.

## 3. A workflow telepítése a tesztrepositoryba

Az `example/github-answer-draft.yml` fájlt másold ide:

`.github/workflows/github-answer-draft.yml`

A mellékelt példa már tartalmazza mindkét jobnál:

```yaml
environment:
  name: Github assistant
```

és az Action hivatkozása:

```yaml
uses: renced42/github-answer-draft-assistant@main
```

Először az Action repository `main` ágát frissítsd, csak utána a tesztrepository workflow-ját.

## 4. Feladás előtti Issue-figyelmeztetés

Másold az alábbi fájlokat a tesztrepositoryba:

| Csomagban | Tesztrepositoryban |
|---|---|
| `example/question.yml` | `.github/ISSUE_TEMPLATE/question.yml` |
| `example/config.yml` | `.github/ISSUE_TEMPLATE/config.yml` |

Az Issue Form feladás közben jól láthatóan jelzi a 3000 karakteres korlátot, és kötelező visszaigazolást kér. A `config.yml` kikapcsolja az üres Issue lehetőségét, így a webes felületen a figyelmeztetést tartalmazó űrlap lesz használható.

A GitHub Issue Form nem támogat beépített dinamikus karakterszámlálót vagy `maxLength` validációt. Emiatt az alkalmazás a beállított korlátot szerveroldalon is kikényszeríti: a GitHubon tárolt Issue változatlan marad, de a Groqnak csak az első 3000 karaktert adja át, és a workflow-naplóban figyelmeztetést ír.

## 5. Tesztelés

Hozz létre **új** issue-t a tesztrepositoryban. A workflow az `opened` eseményre indul; egy régi futás `Re-run jobs` parancsa az adott futásban rögzített workflow-verziót használhatja, ezért Action- vagy workflow-frissítés után az új issue a biztos teszt.

A futás itt követhető:

`github-answer-draft-test → Actions → GitHub választervezet emailben`

Siker esetén a naplóban a privát review link, majd a végén ez jelenik meg:

`A választervezet elküldve 1 címzettnek. GitHub-publikálás nem történt.`

Az email spam mappáját is ellenőrizd.

## 6. Biztonsági és költségkorlátok

- A csomag blokkolja a privát kérdés-repositoryt. Az opcionális privát tudástárból kizárólag címkével jóváhagyott válaszokat olvas.
- Az Issue/Discussion szövegét, a talált publikus forrásokat és a jóváhagyott tudást a Groq szolgáltatás megkapja.
- A forrás-workflow beépített tokenje csak olvasási jogú. A külön tudástár-token kizárólag a kijelölt privát repository Issues erőforrását olvashatja és írhatja.
- Nincs automatikus válasz vagy komment az eredeti kérdés alatt; az egyetlen automatikus GitHub-írás a privát review issue.
- A Groq Free Plan jelenlegi `openai/gpt-oss-120b` limitje 30 kérés/perc, 1000 kérés/nap, 8000 token/perc és 200 000 token/nap. A csomag ezért 5000 karakterre korlátozza a rangsorolt előzetes forráskörnyezetet, és legfeljebb 1200 választokent kér. Ez biztonságos tartalékot hagy a promptnak és a böngészős keresésnek is. A limitek változhatnak; túllépéskor a workflow hibával leáll, és később újrapróbálható.
- A Groq dokumentációja szerint a `browser_search` támogatott az `openai/gpt-oss-120b` modellen, és szerveroldalon fut; külön böngészőszolgáltatást nem kell telepíteni.
- A GitHub Search API `429` sebességkorlátja nem állítja le a teljes workflow-t. A program ilyenkor megszakítja a további GitHub-kereséseket, folytatja a NAV-webes és Groq böngészős keresést, az emailben pedig jelzi, hogy a GitHub-források részlegesek lehetnek.
- A Search API terhelésének csökkentésére kérdésenként legfeljebb két keresőkifejezés és keresésenként legfeljebb hat kódtalálat kerül feldolgozásra.
- GitHub tokent soha ne írj az `action.yml`, a workflow YAML vagy a Java forráskód tartalmába. A tesztrepository `Github assistant` environmentjében, `ORG_READ_TOKEN` nevű secretként tárold. A napló csak azt jelzi, hogy a hitelesítés aktív-e; a token értékét nem írja ki.
- Groq `output_parse_failed` hiba esetén a böngészős keresést a program egyszer automatikusan újrapróbálja. Ha a második kísérlet is ugyanígy hibázik, a már összegyűjtött GitHub- és NAV-webforrásokból készít tervezetet, és az emailben jelzi a részleges internetes forrásfeltárást.

## Helyi/CI önellenőrzés

```bash
bash scripts/test.sh
```

Ehhez Java 21 JDK szükséges. A GitHub Action és a mellékelt CI automatikusan telepíti a Temurin Java 21-et.
