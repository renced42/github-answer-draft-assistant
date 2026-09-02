# Biztonsági modell

- Az eredeti kérdést tartalmazó repository továbbra is csak nyilvános lehet.
- A forrás-workflow beépített `GITHUB_TOKEN` értéke kizárólag `contents`, `issues` és `discussions` olvasási jogot kap.
- Az opcionális `KNOWLEDGE_REPOSITORY_TOKEN` kizárólag a kijelölt privát tudástár Issues erőforrásához kap `Read and write` jogot. Forráskódhoz és más repositoryhoz nem kap írási jogot.
- A rendszer az eredeti kérdéshez soha nem ír kommentet. Kizárólag a konfigurált privát tudástárban hozhat létre review issue-t.
- A kérdés és a források prompt injection szempontból nem megbízható bemenetek; a modell utasítást kap ezek figyelmen kívül hagyására.
- A modell csak az `approved-knowledge` címkés és kitöltött végleges válaszokat kapja meg. A `needs-correction`, `rejected` vagy `outdated` címke kizárja az issue-t.
- A jóváhagyott tudás eredetét és dokumentációs állapotát kötelező besorolni. A `expert-confirmed` + `documentation-gap` kombináció szakértői válaszként használható, de nem jeleníthető meg dokumentált állításként.
- A privát tudástár issue URL-je nem kerül a modellpromptba, ezért a modell nem tudja azt az ügyfélnek szánt forráslistába másolni.
- Az `approved-knowledge` címke hozzáadása egyben azt jelenti, hogy az issue ellenőrzött végleges válasza és forrásai átadhatók a Groq szolgáltatásnak. Titkot, személyes adatot vagy erre nem engedélyezett belső NAV-információt tilos jóváhagyni.
- A Groq-, SMTP-, GitHub- és tudástár-tokeneket kizárólag GitHub Environment secretként tárold, workflow-fájlba soha ne írd bele.
- A tudástár-token értéke nem kerül a promptba, emailbe vagy workflow-naplóba.
