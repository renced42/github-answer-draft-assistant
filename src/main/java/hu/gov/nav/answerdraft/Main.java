package hu.gov.nav.answerdraft;

/** A GitHub Action belépési pontja. */
public final class Main {
    private Main() {
    }

    /** Elindítja a Workspace Agentet, megvárja, majd emailben elküldi a beszélgetés linkjét. */
    public static void main(String[] args) throws Exception {
        Config config = Config.fromEnvironment();
        Question question = Question.from(config);
        String conversationKey = "github:%s:%s:%s".formatted(
                question.repository(), question.eventKind(), question.number());

        System.out.println("Workspace Agent indítása: " + question.url());
        WorkspaceAgentClient.AgentRun run = new WorkspaceAgentClient(
                config.agentTriggerId(), config.agentAccessToken())
                .triggerAndWait(conversationKey, AgentPromptBuilder.build(question), config.pollTimeoutSeconds());

        String subject = "[CHATGPT DRAFT READY] %s #%s – %s".formatted(
                question.repository(), question.number(), question.title());
        String body = """
                Elkészült a ChatGPT Workspace Agent választervezete.

                KÉRDÉS
                ======
                %s

                %s

                CHATGPT-VÁLASZTERVEZET MEGNYITÁSA
                =================================
                %s

                EREDETI GITHUB-KÉRDÉS
                ======================
                %s

                A választ ellenőrizd és szükség esetén javítsd. A rendszer semmit nem publikált GitHubon.
                """.formatted(question.title(), question.body(), run.conversationUrl(), question.url());

        new SmtpMailer(config).send(subject, body);
        System.out.println("A ChatGPT-beszélgetés linkjét tartalmazó email elküldve: "
                + config.recipients().size() + " címzett.");
    }
}
