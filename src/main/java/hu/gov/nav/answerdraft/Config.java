package hu.gov.nav.answerdraft;

import java.util.*;

record Config(String organization, String eventKind, String repository, int number, String title, String body,
              String questionUrl, String author, List<String> recipients, int questionMaxChars, String groqModel, boolean navSearch,
              boolean browserSearch, boolean dryRun, String githubToken, String knowledgeRepository,
              String knowledgeToken, String knowledgeApprovedLabel, String knowledgeCandidateLabel,
              boolean createReviewIssue, int knowledgeLimit, String groqKey, String mailFrom,
              String smtpHost, int smtpPort, String smtpUser, String smtpPassword, boolean smtpStartTls) {
    static Config fromEnvironment() {
        Map<String,String> e = System.getenv();
        String readToken = optional(e,"GITHUB_READ_TOKEN", optional(e,"GITHUB_TOKEN",""));
        String[] recipients = required(e,"ASSISTANT_EMAIL_TO").split(",");
        String knowledgeRepository=optional(e,"ASSISTANT_KNOWLEDGE_REPOSITORY","");
        String knowledgeToken=optional(e,"KNOWLEDGE_REPOSITORY_TOKEN","");
        if(!knowledgeRepository.isBlank()){
            validateRepository(knowledgeRepository);
            if(knowledgeToken.isBlank())throw new IllegalStateException("Az ASSISTANT_KNOWLEDGE_REPOSITORY be van állítva, de hiányzik a KNOWLEDGE_REPOSITORY_TOKEN secret.");
        }
        return new Config(required(e,"ASSISTANT_ORGANIZATION"), required(e,"ASSISTANT_EVENT_KIND"),
                required(e,"ASSISTANT_REPOSITORY"), Integer.parseInt(required(e,"ASSISTANT_NUMBER")),
                required(e,"ASSISTANT_TITLE"), optional(e,"ASSISTANT_BODY",""), required(e,"ASSISTANT_QUESTION_URL"),
                optional(e,"ASSISTANT_AUTHOR","ismeretlen"), Arrays.stream(recipients).map(String::trim).filter(s->!s.isBlank()).toList(),
                boundedInt(e,"ASSISTANT_QUESTION_MAX_CHARS",3000,500,10000),
                optional(e,"ASSISTANT_GROQ_MODEL","openai/gpt-oss-120b"), bool(e,"ASSISTANT_NAV_SEARCH",true),
                bool(e,"ASSISTANT_BROWSER_SEARCH",true), bool(e,"ASSISTANT_DRY_RUN",false), readToken,
                knowledgeRepository,knowledgeToken,optional(e,"ASSISTANT_KNOWLEDGE_APPROVED_LABEL","approved-knowledge"),
                optional(e,"ASSISTANT_KNOWLEDGE_CANDIDATE_LABEL","knowledge-candidate"),
                bool(e,"ASSISTANT_CREATE_REVIEW_ISSUE",true),boundedInt(e,"ASSISTANT_KNOWLEDGE_LIMIT",5,1,20),
                required(e,"GROQ_API_KEY"), optional(e,"MAIL_FROM", optional(e,"SMTP_USERNAME","")),
                optional(e,"SMTP_HOST","smtp.gmail.com"), Integer.parseInt(optional(e,"SMTP_PORT","587")),
                required(e,"SMTP_USERNAME"), required(e,"SMTP_PASSWORD"), bool(e,"SMTP_STARTTLS",true));
    }
    private static String required(Map<String,String> e,String k) { String v=e.get(k); if(v==null||v.isBlank()) throw new IllegalStateException("Hiányzó kötelező környezeti változó: "+k); return v.trim(); }
    private static String optional(Map<String,String> e,String k,String d) { String v=e.get(k); return v==null||v.isBlank()?d:v.trim(); }
    private static boolean bool(Map<String,String> e,String k,boolean d) { return Boolean.parseBoolean(optional(e,k,String.valueOf(d))); }
    private static int boundedInt(Map<String,String> e,String k,int d,int min,int max) { int value=Integer.parseInt(optional(e,k,String.valueOf(d))); if(value<min||value>max)throw new IllegalStateException(k+" értéke "+min+" és "+max+" közötti egész szám legyen."); return value; }
    private static void validateRepository(String value){if(!value.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))throw new IllegalStateException("Hibás tudástár-repository azonosító: "+value+". Elvárt alak: owner/repository");}
}
