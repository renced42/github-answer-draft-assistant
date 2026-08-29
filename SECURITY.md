# Biztonsági modell

- Csak nyilvános kérdés-repository és nyilvános NAV-források használhatók.
- A workflow kizárólag `contents`, `issues` és `discussions` olvasási jogot kap.
- A rendszer emailt küld, GitHubon nem hoz létre és nem módosít tartalmat.
- A kérdés és a források prompt injection szempontból nem megbízható bemenetek; a modell utasítást kap ezek figyelmen kívül hagyására.
- Titkot, személyes adatot, belső NAV-dokumentumot vagy privát repository tartalmát ne adj át ennek a workflow-nak.
- A Groq- és SMTP-kulcsokat kizárólag GitHub Environment secretként tárold, workflow-fájlba soha ne írd bele.
