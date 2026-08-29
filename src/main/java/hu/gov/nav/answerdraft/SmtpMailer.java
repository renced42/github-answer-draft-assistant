package hu.gov.nav.answerdraft;

import javax.net.ssl.*; import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*;

final class SmtpMailer {
    void send(Config c,EmailComposer.Email mail) {
        try(Socket initial=c.smtpStartTls()?new Socket(c.smtpHost(),c.smtpPort()):SSLSocketFactory.getDefault().createSocket(c.smtpHost(),c.smtpPort())){
            initial.setSoTimeout(30000);Connection connection=new Connection(initial);connection.expect(220);connection.command("EHLO github-answer-draft",250);
            if(c.smtpStartTls()){connection.command("STARTTLS",220);SSLSocketFactory factory=(SSLSocketFactory)SSLSocketFactory.getDefault();SSLSocket tls=(SSLSocket)factory.createSocket(initial,c.smtpHost(),c.smtpPort(),true);tls.startHandshake();connection=new Connection(tls);connection.command("EHLO github-answer-draft",250);}
            connection.command("AUTH LOGIN",334);connection.command(Base64.getEncoder().encodeToString(c.smtpUser().getBytes(StandardCharsets.UTF_8)),334);connection.command(Base64.getEncoder().encodeToString(c.smtpPassword().getBytes(StandardCharsets.UTF_8)),235);
            connection.command("MAIL FROM:<"+c.mailFrom()+">",250);for(String to:c.recipients())connection.command("RCPT TO:<"+to+">",250);connection.command("DATA",354);
            String data="From: "+c.mailFrom()+"\r\nTo: "+String.join(", ",c.recipients())+"\r\nSubject: =?UTF-8?B?"+Base64.getEncoder().encodeToString(mail.subject().getBytes(StandardCharsets.UTF_8))+"?=\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"+mimeBase64(mail.body())+"\r\n.";
            connection.command(data,250);connection.command("QUIT",221);
        }catch(Exception x){throw new IllegalStateException("Az email küldése sikertelen: "+x.getMessage(),x);}
    }
    private String mimeBase64(String s){String b=Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));return String.join("\r\n",b.split("(?<=\\G.{76})"));}
    private static final class Connection {
        private final BufferedReader in;private final BufferedWriter out;
        Connection(Socket s)throws IOException{in=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.US_ASCII));out=new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),StandardCharsets.US_ASCII));}
        void command(String command,int expected)throws IOException{out.write(command+"\r\n");out.flush();expect(expected);}
        void expect(int expected)throws IOException{String line=in.readLine();if(line==null)throw new IOException("Az SMTP-kapcsolat megszakadt.");String last=line;while(line.length()>3&&line.charAt(3)=='-'){line=in.readLine();if(line==null)break;last=line;}if(last==null||!last.startsWith(String.valueOf(expected)))throw new IOException("SMTP válasz: "+last);}
    }
}
