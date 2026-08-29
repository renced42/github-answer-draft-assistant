package hu.gov.nav.answerdraft;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** A GitHub Action bemeneteit és titkait tartalmazó változtathatatlan konfiguráció. */
public record Config(
        String eventKind,
        String repository,
        String number,
        String title,
        String body,
        String questionUrl,
        String author,
        List<String> recipients,
        String agentTriggerId,
        String agentAccessToken,
        int pollTimeoutSeconds,
        String smtpHost,
        int smtpPort,
        String smtpUsername,
        String smtpPassword,
        boolean smtpStartTls,
        String mailFrom) {

    /** Környezeti változókból létrehozza és ellenőrzi a konfigurációt. */
    public static Config fromEnvironment() {
        Map<String, String> env = System.getenv();
        return new Config(
                required(env, "ASSISTANT_EVENT_KIND"),
                required(env, "ASSISTANT_REPOSITORY"),
                required(env, "ASSISTANT_NUMBER"),
                required(env, "ASSISTANT_TITLE"),
                env.getOrDefault("ASSISTANT_BODY", ""),
                required(env, "ASSISTANT_QUESTION_URL"),
                env.getOrDefault("ASSISTANT_AUTHOR", "ismeretlen"),
                recipients(required(env, "ASSISTANT_EMAIL_TO")),
                required(env, "WORKSPACE_AGENT_TRIGGER_ID"),
                required(env, "WORKSPACE_AGENT_ACCESS_TOKEN"),
                integer(env, "ASSISTANT_POLL_TIMEOUT_SECONDS", 900),
                required(env, "SMTP_HOST"),
                integer(env, "SMTP_PORT", 587),
                required(env, "SMTP_USERNAME"),
                required(env, "SMTP_PASSWORD"),
                Boolean.parseBoolean(env.getOrDefault("SMTP_STARTTLS", "true")),
                required(env, "MAIL_FROM"));
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hiányzó kötelező környezeti változó: " + name);
        }
        return value.trim();
    }

    private static int integer(Map<String, String> env, String name, int defaultValue) {
        String value = env.get(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    private static List<String> recipients(String value) {
        List<String> result = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Az ASSISTANT_EMAIL_TO nem tartalmaz címzettet.");
        }
        return result;
    }
}
