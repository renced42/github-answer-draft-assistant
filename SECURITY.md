# Biztonsági modell

## Bizalmi határok

- Az Issue, Discussion, komment és repository-fájl tartalma nem megbízható bemenet.
- Az AI-kimenet tervezet, nem hivatalos válasz.
- A rendszer nem rendelkezik GitHub-komment létrehozási vagy módosítási funkcióval.

## Kötelező üzemeltetési szabályok

1. A GitHub-token csak olvasási jogosultságot kaphat.
2. Az AI- és emailkulcsokat kizárólag GitHub Secretsben szabad tárolni.
3. Ingyenes külső AI API-ba csak nyilvános GitHub-adat küldhető.
4. Microsoft Graph esetén a `Mail.Send` hozzáférést egy dedikált postafiókra kell korlátozni.
5. A választervezetet publikálás előtt embernek kell ellenőriznie.
6. Titok, személyes adat vagy belső infrastruktúra-adat észlelésekor a workflow-t le kell állítani és az esetet ki kell vizsgálni.

