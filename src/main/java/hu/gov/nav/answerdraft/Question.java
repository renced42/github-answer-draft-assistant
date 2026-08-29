package hu.gov.nav.answerdraft;

/** Egy GitHubon érkezett kérdés biztonságosan továbbítható adatai. */
public record Question(
        String eventKind,
        String repository,
        String number,
        String title,
        String body,
        String url,
        String author) {

    /** A konfigurációból felépíti a kérdést. */
    public static Question from(Config config) {
        return new Question(config.eventKind(), config.repository(), config.number(), config.title(),
                config.body(), config.questionUrl(), config.author());
    }
}
