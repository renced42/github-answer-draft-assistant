package hu.gov.nav.answerdraft;

import java.util.List;

final class EmailComposer {
    record Email(String subject,String body){}
    static Email compose(Question q,String draft,List<Source> sources){
        String subject="[GITHUB DRAFT] "+q.repository()+" #"+q.number()+" – "+q.title();
        String body="""
                Ez automatikusan készített választervezet. GitHubra semmi nem került publikálásra.

                EREDETI KÉRDÉS
                Repository: %s
                Típus/sorszám: %s #%d
                Szerző: @%s
                Cím: %s

                %s

                ============================================================
                A kérdés megnyitása és a válasz kézi beillesztése:
                %s

                Előzetesen összegyűjtött források száma: %d
                A tervezetet publikálás előtt mindig ellenőrizd.
                """.formatted(q.repository(),q.kind(),q.number(),q.author(),q.title(),draft,q.url(),sources.size());
        return new Email(subject,body);
    }
}
