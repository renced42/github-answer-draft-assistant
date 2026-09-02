# Telepítési útmutató – privát review és ellenőrzött tudástár

Ez az útmutató a következő repositorykkal számol:

- Action: `renced42/github-answer-draft-assistant`
- Tesztkérdések: `renced42/github-answer-draft-test`
- Privát tudástár: `renced42/github-answer-knowledge`

Az eredeti Issue vagy Discussion soha nem kap automatikus választ. A rendszer privát review issue-t készít, emailt küld, és csak az ember által jóváhagyott végleges választ használhatja későbbi kérdéseknél.

## 1. Az Action repository frissítése

1. Nyisd meg a `renced42/github-answer-draft-assistant` repositoryt.
2. A csomag teljes tartalmát másold a repository gyökerébe.
3. Ellenőrizd, hogy az `action.yml` közvetlenül a gyökérben található.
4. Commitold a fájlokat a `main` ágra.
5. Nyisd meg az **Actions → Java ellenőrzés** futást.
6. A `Fordítás és önellenőrzés` lépésnek sikeresen kell befejeződnie.

A tesztworkflow továbbra is ezt használja:

```yaml
uses: renced42/github-answer-draft-assistant@main
```

## 2. Privát tudástár-repository létrehozása

1. GitHub jobb felső sarok → **New repository**.
2. Owner: `renced42`.
3. Repository name: `github-answer-knowledge`.
4. Visibility: **Private**.
5. Engedélyezd az Issues funkciót: **Settings → General → Features → Issues**.
6. A csomag `knowledge-repository-template` könyvtárának tartalmát másold a privát repository gyökerébe.

A végeredmény:

```text
github-answer-knowledge/
├── .github/
│   └── ISSUE_TEMPLATE/
│       ├── approved-answer.yml
│       └── config.yml
└── README.md
```

## 3. Tudástár-címkék létrehozása

A privát repositoryban nyisd meg: **Issues → Labels → New label**.

Hozd létre ezeket a címkéket pontosan:

| Címke | Feladat |
|---|---|
| `knowledge-candidate` | Még ellenőrzendő tervezet |
| `approved-knowledge` | Jóváhagyott és a külső AI-nak átadható tudás |
| `needs-correction` | Javítás szükséges; nem használható |
| `rejected` | Elutasított; nem használható |
| `outdated` | Elavult; nem használható |
| `published-manually` | Az eredeti kérdésnél kézzel publikálva |

## 4. Fine-grained GitHub token létrehozása

1. Profilkép → **Settings**.
2. **Developer settings → Personal access tokens → Fine-grained tokens**.
3. Válaszd a **Generate new token** lehetőséget.
4. Token name: `github-answer-knowledge`.
5. Állíts be ésszerű lejárati dátumot.
6. Resource owner: `renced42`.
7. Repository access: **Only select repositories**.
8. Válaszd ki kizárólag a `github-answer-knowledge` repositoryt.
9. Repository permissions:
   - **Issues: Read and write**;
   - **Metadata: Read-only** – ezt a GitHub automatikusan biztosítja.
10. Más írási jogosultságot ne adj.
11. Generáld le, és ideiglenesen másold ki a tokent.

Ez a token a privát review issue létrehozásához és a jóváhagyott issue-k olvasásához kell. Az eredeti kérdés repositoryjához nem ad írási jogot.

## 5. Environment változók és secretek

A `renced42/github-answer-draft-test` repositoryban nyisd meg:

**Settings → Environments → Github assistant**

### Environment variables

| Név | Érték |
|---|---|
| `DRAFT_EMAIL_TO` | `rencenji.denes@gmail.com` vagy más címzett |
| `DRAFT_GROQ_MODEL` | `openai/gpt-oss-120b` |
| `DRAFT_QUESTION_MAX_CHARS` | `3000` |
| `KNOWLEDGE_REPOSITORY` | `renced42/github-answer-knowledge` |
| `KNOWLEDGE_LIMIT` | `5` |

### Environment secrets

| Név | Érték |
|---|---|
| `KNOWLEDGE_REPOSITORY_TOKEN` | A 4. lépésben létrehozott fine-grained token |
| `GROQ_API_KEY` | Groq API-kulcs |
| `ORG_READ_TOKEN` | GitHub olvasási token a publikus keresések magasabb limitjéhez |
| `MAIL_FROM` | Feladó email-címe |
| `SMTP_HOST` | Gmail esetén `smtp.gmail.com` |
| `SMTP_PORT` | Gmail STARTTLS esetén `587` |
| `SMTP_USERNAME` | Feladó Gmail-címe |
| `SMTP_PASSWORD` | Gmail alkalmazásjelszó |
| `SMTP_STARTTLS` | `true` |

A GitHub a secret értékét mentés után nem jeleníti meg. Ha bizonytalan vagy az értékben, írd felül újra.

## 6. A kérdésfigyelő workflow telepítése

A csomagból másold:

```text
example/github-answer-draft.yml
```

ide a `github-answer-draft-test` repositoryban:

```text
.github/workflows/github-answer-draft.yml
```

Mindkét jobban szerepelnie kell:

```yaml
environment:
  name: Github assistant
```

és a tudástár-beállításnak:

```yaml
with:
  knowledge-repository: ${{ vars.KNOWLEDGE_REPOSITORY }}
  knowledge-approved-label: approved-knowledge
  knowledge-candidate-label: knowledge-candidate
  create-review-issue: "true"
  knowledge-limit: ${{ vars.KNOWLEDGE_LIMIT || '5' }}

env:
  KNOWLEDGE_REPOSITORY_TOKEN: ${{ secrets.KNOWLEDGE_REPOSITORY_TOKEN }}
```

## 7. Első teszt

1. Hozz létre egy új Issue-t a `github-answer-draft-test` repositoryban.
2. Nyisd meg: **Actions → GitHub választervezet emailben**.
3. A naplóban ezekhez hasonló sorokat kell látni:

```text
Jóváhagyott tudás keresése: renced42/github-answer-knowledge
Felhasznált jóváhagyott tudás-issue-k: 0
Privát review issue létrehozva: https://github.com/renced42/github-answer-knowledge/issues/1
A választervezet elküldve 1 címzettnek. GitHub-publikálás nem történt.
```

4. Ellenőrizd az emailt és szükség esetén a Spam mappát.
5. Az emailben két link lesz:
   - az eredeti kérdés;
   - a privát review issue.

## 8. Tervezet javítása és jóváhagyása

1. Nyisd meg az emailből a privát review issue-t.
2. Kattints az issue jobb felső részén az **Edit** gombra.
3. Keresd meg ezt a szakaszt:

```markdown
## Ellenőrzött végleges válasz

[IDE ÍRD AZ ELLENŐRZÖTT VÉGLEGES VÁLASZT]
```

4. A helyőrzőt teljes egészében cseréld le a szakmailag ellenőrzött válaszra.
5. Ellenőrizd és szükség szerint javítsd a **Felhasznált források** részt.
6. Mentsd az issue-t.
7. Távolítsd el a `knowledge-candidate` címkét.
8. Add hozzá az `approved-knowledge` címkét.

Az `approved-knowledge` alkalmazása egyben engedélyezi, hogy a végleges szöveg és forrásai a következő kérdéseknél a Groq API-hoz kerüljenek.

## 9. A jóváhagyott tudás ellenőrzése

1. Hozz létre egy második, hasonló témájú tesztkérdést.
2. Az Actions naplóban ennek már nagyobbnak kell lennie nullánál:

```text
Felhasznált jóváhagyott tudás-issue-k: 1
```

3. Az új review issue **Felhasznált források** listájában megjelenik a korábbi jóváhagyott tudás-issue.
4. Ellenőrizd, hogy a modell nem kever más NAV-rendszert a válaszba.

## 10. Jóváhagyás visszavonása

Ha egy válasz hibás vagy elavult:

1. Távolítsd el az `approved-knowledge` címkét.
2. Add hozzá a `needs-correction`, `rejected` vagy `outdated` címkét.

A három blokkoló címke akkor is kizárja az issue-t, ha az `approved-knowledge` véletlenül rajta maradt.

## 11. Manuális tudásbejegyzés

A privát repositoryban válaszd:

**Issues → New issue → Ellenőrzött válasz rögzítése**

Töltsd ki az Issue Formot. Az issue először `knowledge-candidate` címkét kap. Ellenőrzés után manuálisan add hozzá az `approved-knowledge` címkét.

## 12. Ismételt futtatás

A workflow ismételt futtatása nem hoz létre újabb review issue-t ugyanahhoz az eredeti kérdés-URL-hez. A rendszer megkeresi a már létező privát issue-t, és annak linkjét küldi ki újra.

## 13. Gyakori hibák

### `KNOWLEDGE_REPOSITORY_TOKEN` hiányzik

Ellenőrizd, hogy a secret a **Github assistant Environment** alatt található, és mindkét job tartalmazza az `environment.name` beállítást.

### HTTP 404 a privát repository olvasásakor

Ellenőrizd:

- a `KNOWLEDGE_REPOSITORY` pontosan `renced42/github-answer-knowledge`;
- a tokennél ez a repository van kijelölve;
- a repository valóban létezik és privát.

### HTTP 403 review issue létrehozásakor

A fine-grained token `Issues` jogosultsága legyen **Read and write**.

### Nem kerül címke az automatikus review issue-ra

A `knowledge-candidate` címkét előzetesen létre kell hozni a privát repositoryban, pontosan ezzel a névvel.

### A jóváhagyott issue nem kerül a promptba

Ellenőrizd, hogy:

- szerepel rajta az `approved-knowledge` címke;
- nincs rajta `needs-correction`, `rejected` vagy `outdated` címke;
- az **Ellenőrzött végleges válasz** helyőrzőjét lecserélted;
- az **Azonosított rendszer** megegyezik az új kérdés rendszerével.

## 14. Biztonsági megjegyzés

A privát repository nem jelenti automatikusan azt, hogy tartalma továbbítható külső AI-szolgáltatónak. Csak olyan issue-ra tedd rá az `approved-knowledge` címkét, amelynek végleges válasza és forrásai a Groq részére átadhatók. A token és más secretek soha ne kerüljenek issue-ba.
