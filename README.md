# GitHub Answer Draft Assistant – Java 21 + Groq

Nyilvános GitHub Issue vagy Discussion létrehozásakor a rendszer:

1. keres a teljes `nav-gov-hu` GitHub organization nyilvános fájljaiban, issue-iban és discussionjeiben;
2. előnyben részesíti az XSD-, XML-, minta- és specifikációfájlokat, és követi az XSD include/import hivatkozásokat;
3. opcionálisan keres a nyilvános `nav.gov.hu` oldalakon;
4. a Groq modell és böngészős keresés segítségével magyar választervezetet készít;
5. a teljes tervezetet és az eredeti kérdés linkjét emailben elküldi;
6. **soha nem ír és nem publikál semmit GitHubra**.

Az alkalmazás Java 21, külső Java-függőség és saját szerver nélkül fut GitHub Actionsben. A tesztverzió közvetlenül a `main` ágról használható, ezért nem kell tag vagy GitHub Release.

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

Siker esetén a napló végén ez jelenik meg:

`A választervezet elküldve 1 címzettnek. GitHub-publikálás nem történt.`

Az email spam mappáját is ellenőrizd.

## 6. Biztonsági és költségkorlátok

- A csomag blokkolja a privát kérdés-repositoryt, és csak nyilvános NAV/GitHub-forrásokra készült.
- Az Issue/Discussion szövegét és a talált publikus forrásokat a Groq szolgáltatás megkapja.
- A GitHub-jogosultságok csak olvasási jogok.
- Nincs automatikus válasz, komment vagy publikálás.
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
