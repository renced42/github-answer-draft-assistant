package hu.gov.nav.answerdraft;

import java.util.*;

record Config(String organization, String eventKind, String repository, int number, String title, String body,
              String questionUrl, String author, List<String> recipients, String groqModel, boolean navSearch,
              boolean browserSearch, boolean dryRun, String githubToken, String groqKey, String mailFrom,
              String smtpHost, int smtpPort, String smtpUser, String smtpPassword, boolean smtpStartTls) {
    static Config fromEnvironment() {
        Map<String,String> e = System.getenv();
        String readToken = optional(e,"GITHUB_READ_TOKEN", optional(e,"GITHUB_TOKEN",""));
        String[] recipients = required(e,"ASSISTANT_EMAIL_TO").split(",");
        return new Config(required(e,"ASSISTANT_ORGANIZATION"), required(e,"ASSISTANT_EVENT_KIND"),
                required(e,"ASSISTANT_REPOSITORY"), Integer.parseInt(required(e,"ASSISTANT_NUMBER")),
                required(e,"ASSISTANT_TITLE"), optional(e,"ASSISTANT_BODY",""), required(e,"ASSISTANT_QUESTION_URL"),
                optional(e,"ASSISTANT_AUTHOR","ismeretlen"), Arrays.stream(recipients).map(String::trim).filter(s->!s.isBlank()).toList(),
                optional(e,"ASSISTANT_GROQ_MODEL","openai/gpt-oss-120b"), bool(e,"ASSISTANT_NAV_SEARCH",true),
                bool(e,"ASSISTANT_BROWSER_SEARCH",true), bool(e,"ASSISTANT_DRY_RUN",false), readToken,
                required(e,"GROQ_API_KEY"), optional(e,"MAIL_FROM", optional(e,"SMTP_USERNAME","")),
                optional(e,"SMTP_HOST","smtp.gmail.com"), Integer.parseInt(optional(e,"SMTP_PORT","587")),
                required(e,"SMTP_USERNAME"), required(e,"SMTP_PASSWORD"), bool(e,"SMTP_STARTTLS",true));
    }
    private static String required(Map<String,String> e,String k) { String v=e.get(k); if(v==null||v.isBlank()) throw new IllegalStateException("Hiányzó kötelező környezeti változó: "+k); return v.trim(); }
    private static String optional(Map<String,String> e,String k,String d) { String v=e.get(k); return v==null||v.isBlank()?d:v.trim(); }
    private static boolean bool(Map<String,String> e,String k,boolean d) { return Boolean.parseBoolean(optional(e,k,String.valueOf(d))); }
}
