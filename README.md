# GitHub Answer Draft Assistant

A projekt a `nav-gov-hu` repository-k új Issue és Discussion kérdéseihez forrásalapú magyar választervezetet készít, majd emailben elküldi az ellenőrző személynek.

**A rendszer soha nem publikál választ a GitHubon.** Az emailben lévő GitHub-link megnyitása után a véglegesített választ kézzel kell bemásolni.

## Folyamat

1. Új Issue vagy Discussion érkezik.
2. A GitHub Actions csak olvasási jogosultsággal elindítja az Actiont.
3. A rendszer az organization Issue, Discussion és fájltartalmában, valamint a publikus
   `nav.gov.hu` eÁFA/eNyugta oldalakon keres.
4. A Gemini vagy Groq kizárólag a talált forrásokból készít választervezetet.
5. Az ingyenes Gmail SMTP elküldi a levelet. Microsoft Graph opcionálisan használható.
6. A címzett ellenőrzi, javítja, majd kézzel publikálja a választ.

A keresés magyar ragozott szavakat is normalizál, és nagy fájloknál a kulcsszó körüli releváns részletet adja át a modellnek az állomány eleje helyett.

## Címzett konfigurálása

A figyelt repository vagy a teljes organization Actions változói között hozd létre:

| Név | Kezdőérték |
|---|---|
| `DRAFT_EMAIL_TO` | `rencenji.denes@gmail.com` |

Az érték kódmódosítás nélkül átírható. Több címzett vesszővel választható el.

## AI konfiguráció

Szükséges secret:

| Secret | Tartalom |
|---|---|
| `AI_API_KEY` | Gemini vagy Groq API-kulcs |

Gemini esetén a Google AI Studio aktuális „auth key” típusú kulcsát használd. A kulcsot a program a hivatalos `x-goog-api-key` HTTP-fejlécben továbbítja.

Változók:

| Változó | Alapérték | Példa |
|---|---|---|
| `DRAFT_AI_PROVIDER` | `gemini` | `gemini`, `groq` |
| `DRAFT_AI_MODEL` | `gemini-3.6-flash` | szolgáltatói modellnév |

Az ingyenes AI-szintre csak nyilvános GitHub- és NAV-weboldaladat küldhető. A privát repository-k feldolgozása alapértelmezetten programból tiltott.

## NAV.GOV.HU-források

A `nav-gov-hu-search` Action-bemenet alapértéke `true`. A rendszer kizárólag publikus,
HTTPS-en elérhető HTML-oldalakat olvas a `nav.gov.hu/ado/eafa` és
`nav.gov.hu/ado/enyugta` területekről. Egy futásban legfeljebb 12 kapcsolódó oldalt
vizsgál meg, és a három legrelevánsabb NAV-forrást adja át a modellnek. Bejelentkezett
oldalt, űrlapot vagy más domaint nem ér el; PDF-ek tartalmát ebben a verzióban nem dolgozza fel.

Kikapcsolás a figyelő workflow-ban:

```yaml
with:
  nav-gov-hu-search: "false"
```

## Ingyenes emailküldés Gmail SMTP-vel

Változó:

| Név | Érték |
|---|---|
| `DRAFT_MAIL_PROVIDER` | `smtp` |

Szükséges secretek:

| Secret | Tartalom |
|---|---|
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `465` |
| `SMTP_USERNAME` | a küldő Gmail-cím |
| `SMTP_PASSWORD` | 16 karakteres Google alkalmazásjelszó |
| `SMTP_STARTTLS` | `false` |
| `MAIL_FROM` | a küldő Gmail-cím |

A `SMTP_PASSWORD` nem a Google-fiók normál jelszava. A Google-fiókban kétlépcsős azonosítást kell bekapcsolni, majd külön alkalmazásjelszót kell létrehozni. A feladó és a `DRAFT_EMAIL_TO` címzett egyaránt lehet `rencenji.denes@gmail.com`.

Ajánlott induló beállítás:

```text
DRAFT_EMAIL_TO=rencenji.denes@gmail.com
DRAFT_MAIL_PROVIDER=smtp
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_USERNAME=rencenji.denes@gmail.com
SMTP_STARTTLS=false
MAIL_FROM=rencenji.denes@gmail.com
```

Ezek közül a `DRAFT_EMAIL_TO`, `DRAFT_MAIL_PROVIDER` lehet GitHub Actions Variable; az SMTP-adatokat és különösen az alkalmazásjelszót GitHub Actions Secretként kell felvenni.

## Opcionális Microsoft Graph levélküldés

Változó:

| Név | Érték |
|---|---|
| `DRAFT_MAIL_PROVIDER` | `graph` |

Szükséges secretek:

| Secret | Tartalom |
|---|---|
| `GRAPH_TENANT_ID` | Microsoft Entra tenant azonosító |
| `GRAPH_CLIENT_ID` | alkalmazásregisztráció kliensazonosító |
| `GRAPH_CLIENT_SECRET` | alkalmazás secretje |
| `MAIL_FROM` | dedikált feladó postafiók |

Az alkalmazásnak Graph `Mail.Send` application permission szükséges. Exchange Online Application RBAC segítségével a jogosultságot kizárólag a dedikált feladó postafiókra kell szűkíteni. Ez nem része az alapértelmezett, ingyenes PoC-nak.

## Költségkorlát

Az alapértelmezett összeállítás nem tartalmaz fizetős szolgáltatást:

- nyilvános repository GitHub-hosted Actions futása;
- Gemini Developer API Free Tier vagy Groq Free Plan;
- Gmail SMTP;
- GitHub organization/repository változók és Secrets.

A rendszer nem tartalmaz fizetős API-ra automatikus átváltást. Ingyenes limit kimerülésekor az adott futás sikertelen lesz, ezért nem keletkezhet észrevétlen költség. A szolgáltatói fiókban se engedélyezz automatikus fizetős számlázásra váltást.

## GitHub-hozzáférés

Nyilvános repository-khoz a workflow automatikus `GITHUB_TOKEN` értéke elegendő lehet. Organization-szintű vagy privát kereséshez add meg az `ORG_READ_TOKEN` secretet, kizárólag olvasási jogosultsággal.

Az Action nem tartalmaz GitHub-írási műveletet. A telepítő workflow jogosultságai:

```yaml
permissions:
  contents: read
  issues: read
  discussions: read
```

## Telepítés

1. Hozd létre a `nav-gov-hu/github-answer-draft-assistant` repository-t.
2. Másold bele ennek a csomagnak a tartalmát.
3. Készíts `v1` release taget.
4. Másold az `examples/github-answer-draft.yml` fájlt minden figyelt repository `.github/workflows/` könyvtárába.
5. Állítsd be a fenti organization változókat és secreteket.
6. Először egy tesztrepository-ban, `dry-run: "true"` bemenettel próbáld ki.
7. Sikeres próba után töröld a `dry-run` sort vagy állítsd `false` értékre.

Fontos: a Discussion esemény csak akkor fut, ha a workflow a repository alapértelmezett ágán található. A GitHub a Discussion workflow eseményeket jelenleg public preview funkcióként dokumentálja.

## Helyi teszt

```bash
python3 -m unittest discover -s tests -v
```

## Biztonsági tulajdonságok

- nincs automatikus GitHub-publikálás;
- alapértelmezetten nincs privát forrás;
- minden GitHub-művelet csak olvasás;
- a kulcsok GitHub Secretsben maradnak;
- a források tartalma nem megbízható adatként kerül a promptba;
- a levél HTML-tartalma escape-elt;
- a válaszban kötelező a forrás, a bizonytalanság és a bizonyosság feltüntetése.
- átmeneti `408`, `429` és `5xx` API-hibáknál legfeljebb négyszer, exponenciális késleltetéssel és jitterrel próbálkozik újra.
