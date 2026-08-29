# GitHub Answer Workspace Agent – Java V2

Ez a GitHub Action új Issue vagy Discussion létrehozásakor elindít egy ChatGPT Workspace Agentet.
Az Agent a nyilvános `nav-gov-hu` repository-kban és a NAV hivatalos weboldalain kutat, majd egy
ChatGPT-beszélgetésben választervezetet készít. A Java alkalmazás megvárja a futás végét, és Gmail
SMTP-n elküldi a beszélgetés linkjét a konfigurált címre.

Az Action **nem publikál választ GitHubon**, és nincs GitHub írási jogosultsága.

## Miért link érkezik, nem a válasz teljes szövege?

A Workspace Agent Trigger Runs API jelenleg visszaadja a futás állapotát és a beszélgetés URL-jét,
de az Agent válaszának teljes szövege nem kérhető le az API-ból. Emiatt az email a kérdést, az
eredeti GitHub-linket és a kész ChatGPT-beszélgetés linkjét tartalmazza.

## 1. Workspace Agent létrehozása

1. ChatGPT Workben hozz létre egy új Agentet.
2. Másold be az `agent/NAV_ANSWER_AGENT_INSTRUCTIONS.md` tartalmát az Agent utasításaihoz.
3. Adj neki hozzáférést a GitHub nyilvános tartalmaihoz és a webes kereséshez.
4. A GitHub-kapcsolat csak olvasási jogosultságú legyen. Ne adj publikálási vagy írási eszközt.
5. Tedd közzé az Agent API trigger csatornáját.
6. Jegyezd fel az `agtch_...` alakú triggerazonosítót.
7. Hozz létre Workspace Agent access tokent.

Hivatalos API-leírás: <https://developers.openai.com/workspace-agents/trigger-runs>

## 2. Az Action repository frissítése

1. Nyisd meg a meglévő `renced42/github-answer-draft-assistant` repository-t.
2. Másold bele ennek a csomagnak a tartalmát. A gyökérben lévő `action.yml` írja felül a V1 fájlját.
3. Commitold és pushold a fájlokat az alapértelmezett branchre.
4. Készíts az új commitra `v2.0.0` nevű, kisbetűs taget és ugyanilyen Release-t.

```bash
git tag v2.0.0
git push origin v2.0.0
```

A ZIP-fájlt nem kell feltölteni a Release assetjei közé. A GitHub Action a taghez tartozó repository-
fájlokat használja; ezért az `action.yml` és a `src/` könyvtár legyen benne a tag commitjában.

## 3. Teszt repository environment beállítása

A `renced42/github-answer-draft-test` repository-ban nyisd meg:

`Settings → Environments → Github assistant`

Environment variable:

| Név | Érték |
|---|---|
| `DRAFT_EMAIL_TO` | `rencenji.denes@gmail.com` vagy más cím |
| `WORKSPACE_AGENT_TRIGGER_ID` | az `agtch_...` azonosító |

Environment secrets:

| Név | Érték |
|---|---|
| `WORKSPACE_AGENT_ACCESS_TOKEN` | Workspace Agent access token |
| `MAIL_FROM` | a küldő Gmail-cím |
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `587` |
| `SMTP_USERNAME` | a küldő Gmail-cím |
| `SMTP_PASSWORD` | Google alkalmazásjelszó, szóközök nélkül |
| `SMTP_STARTTLS` | `true` |

A korábbi `AI_API_KEY`, `DRAFT_AI_PROVIDER`, `DRAFT_AI_MODEL` és `ORG_READ_TOKEN` nem szükséges.

## 4. Workflow telepítése

A csomagban az `example/github-answer-draft.yml` példafájl található. Ezt másold a teszt repository
`.github/workflows/github-answer-draft.yml` útvonalára. A két jobban szereplő alábbi
blokk szükséges ahhoz, hogy az environment változói és secretjei elérhetők legyenek:

```yaml
environment:
  name: Github assistant
```

Ha az Action repository-d neve eltér, módosítsd mindkét `uses:` sort. A tag kis- és
nagybetűérzékeny: `@v2.0.0`, nem `@V2.0.0`.

## 5. Tesztelés

Hozz létre új Issue-t a teszt repository-ban. A futást itt látod:

`Actions → GitHub választervezet ChatGPT-ben`

A logban várhatóan ezek jelennek meg:

```text
Workspace Agent indítása: https://github.com/...
Workspace Agent állapota: queued
Workspace Agent állapota: in_progress
A ChatGPT-beszélgetés linkjét tartalmazó email elküldve: 1 címzett.
```

Az Agent futása néhány percet is igénybe vehet. A kód legfeljebb 15 percig vár.

## 6. Helyi Java-ellenőrzés

Java 21 mellett:

```bash
chmod +x scripts/test.sh
./scripts/test.sh
```

Nincs Maven- vagy külső Java-függőség; az Action futás közben `javac`-kal fordít.

## Költség

Az Action nem használ Gemini- vagy OpenAI Platform API-kulcsot. A GitHub publikus repository standard
runnerhasználata általában nem jelent külön Actions-költséget. A Workspace Agent futtatása azonban a
ChatGPT Workspace csomagodhoz tartozó Work/credit keretet használhat; ezért a teljes működés csak akkor
marad külön díj nélküli, ha a meglévő workspace-keretbe belefér és nincs engedélyezett túlfogyasztás.
