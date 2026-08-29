package hu.gov.nav.answerdraft;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Külső Java-függőség nélkül küld egyszerű UTF-8 szöveges levelet SMTP-n. */
public final class SmtpMailer {
    private final Config config;

    /** Létrehozza a levélküldőt. */
    public SmtpMailer(Config config) {
        this.config = config;
    }

    /** Elküldi a levelet valamennyi beállított címzettnek. */
    public void send(String subject, String body) throws IOException {
        try (Socket initialSocket = new Socket(config.smtpHost(), config.smtpPort())) {
            Connection connection = new Connection(initialSocket);
            connection.expect(220);
            connection.command("EHLO github-actions", 250);

            if (config.smtpStartTls()) {
                connection.command("STARTTLS", 220);
                Socket tlsSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(
                        initialSocket, config.smtpHost(), config.smtpPort(), true);
                connection = new Connection(tlsSocket);
                connection.command("EHLO github-actions", 250);
            }

            connection.command("AUTH LOGIN", 334);
            connection.command(base64(config.smtpUsername()), 334);
            connection.command(base64(config.smtpPassword()), 235);
            connection.command("MAIL FROM:<" + config.mailFrom() + ">", 250);
            for (String recipient : config.recipients()) {
                connection.command("RCPT TO:<" + recipient + ">", 250, 251);
            }
            connection.command("DATA", 354);
            connection.write(message(subject, body, config.mailFrom(), config.recipients()));
            connection.write("\r\n.\r\n");
            connection.expect(250);
            connection.command("QUIT", 221);
        }
    }

    private static String message(String subject, String body, String from, List<String> recipients) {
        String encodedSubject = "=?UTF-8?B?" + base64(subject) + "?=";
        String encodedBody = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return "From: " + from + "\r\n"
                + "To: " + String.join(", ", recipients) + "\r\n"
                + "Subject: " + encodedSubject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + encodedBody;
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class Connection {
        private final BufferedReader reader;
        private final BufferedWriter writer;

        private Connection(Socket socket) throws IOException {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
        }

        private void command(String command, int... expectedCodes) throws IOException {
            write(command + "\r\n");
            expect(expectedCodes);
        }

        private void write(String value) throws IOException {
            writer.write(value);
            writer.flush();
        }

        private void expect(int... expectedCodes) throws IOException {
            String line = reader.readLine();
            if (line == null || line.length() < 3) {
                throw new IOException("Érvénytelen SMTP-válasz: " + line);
            }
            String lastLine = line;
            String codeText = line.substring(0, 3);
            while (lastLine.length() > 3 && lastLine.charAt(3) == '-') {
                lastLine = reader.readLine();
                if (lastLine == null) {
                    throw new IOException("Megszakadt a többsoros SMTP-válasz.");
                }
            }
            int actualCode = Integer.parseInt(codeText);
            for (int expectedCode : expectedCodes) {
                if (actualCode == expectedCode) {
                    return;
                }
            }
            throw new IOException("Váratlan SMTP-válasz: " + line + " ... " + lastLine);
        }
    }
}
