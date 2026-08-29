package hu.gov.nav.answerdraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

/** A ChatGPT Workspace Agent Trigger Runs API-jának kliense. */
public final class WorkspaceAgentClient {
    private static final String API_ROOT = "https://api.chatgpt.com/v1/workspace_agents/";
    private static final String BETA_HEADER = "workspace_agent_runs=v1";
    private static final Set<String> TERMINAL_STATUSES = Set.of("completed", "failed");
    private final HttpClient httpClient;
    private final String triggerId;
    private final String accessToken;

    /** Létrehozza a klienst a megadott Agent-azonosítóval és hozzáférési tokennel. */
    public WorkspaceAgentClient(String triggerId, String accessToken) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.triggerId = triggerId;
        this.accessToken = accessToken;
    }

    /** Elindítja az Agentet, majd a megadott időkorlátig megvárja a befejezést. */
    public AgentRun triggerAndWait(String conversationKey, String prompt, int timeoutSeconds)
            throws IOException, InterruptedException {
        AgentRun run = trigger(conversationKey, prompt);
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        while (!TERMINAL_STATUSES.contains(run.status())) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("A Workspace Agent nem fejeződött be " + timeoutSeconds
                        + " másodpercen belül. Beszélgetés: " + run.conversationUrl());
            }
            System.out.println("Workspace Agent állapota: " + run.status());
            Thread.sleep(10_000);
            run = status(run.runId(), run.conversationUrl());
        }
        if ("failed".equals(run.status())) {
            throw new IllegalStateException("A Workspace Agent futása sikertelen. Beszélgetés: "
                    + run.conversationUrl());
        }
        return run;
    }

    private AgentRun trigger(String conversationKey, String prompt) throws IOException, InterruptedException {
        String body = "{" +
                "\"conversation_key\":" + Json.quote(conversationKey) + "," +
                "\"input\":" + Json.quote(prompt) + "}";
        HttpRequest request = baseRequest(URI.create(API_ROOT + triggerId + "/trigger"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", sha256(conversationKey))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        String response = send(request);
        return new AgentRun(
                requiredField(response, "agent_trigger_run_id"),
                valueOrDefault(Json.stringField(response, "status"), "queued"),
                requiredField(response, "conversation_url"));
    }

    private AgentRun status(String runId, String conversationUrl) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(URI.create(API_ROOT + triggerId + "/runs/" + runId)).GET().build();
        String response = send(request);
        return new AgentRun(runId, requiredField(response, "status"),
                valueOrDefault(Json.stringField(response, "conversation_url"), conversationUrl));
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + accessToken)
                .header("OpenAI-Beta", BETA_HEADER)
                .header("Accept", "application/json");
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Workspace Agent API HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static String requiredField(String json, String field) {
        String value = Json.stringField(json, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A Workspace Agent válaszából hiányzik: " + field + ". Válasz: " + json);
        }
        return value;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("A SHA-256 nem érhető el.", exception);
        }
    }

    /** Egy elindított Workspace Agent futás legfontosabb adatai. */
    public record AgentRun(String runId, String status, String conversationUrl) {
    }
}
