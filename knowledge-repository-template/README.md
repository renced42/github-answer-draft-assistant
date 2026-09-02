# GitHub Answer Knowledge

Privát, ember által ellenőrzött válaszok GitHub Issues-alapú tudástára.

## Címkék

- `knowledge-candidate`: még ellenőrzendő tervezet;
- `approved-knowledge`: ellenőrzött és a külső AI-szolgáltatónak átadható tudás;
- `needs-correction`: javítás szükséges, ezért nem használható;
- `rejected`: elutasított, ezért nem használható;
- `outdated`: elavult, ezért nem használható;
- `published-manually`: az eredeti kérdésnél kézzel publikált válasz.

Az asszisztens csak az `approved-knowledge` címkés, kitöltött **Ellenőrzött végleges válasz** szakaszt olvassa. A `needs-correction`, `rejected` vagy `outdated` címke minden esetben kizárja a tudást a felhasználásból.

Az automatikusan létrehozott review issue-ban az ellenőrnek az **Ellenőrzött végleges válasz** helyőrzőjét kell lecserélnie. Ezután eltávolítható a `knowledge-candidate`, és hozzáadható az `approved-knowledge` címke.
