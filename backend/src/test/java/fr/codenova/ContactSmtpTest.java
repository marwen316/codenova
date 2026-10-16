package fr.codenova;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactSmtpTest {
    @Test void deliversThroughActualSmtpTransportToLocalTestServer() throws Exception {
        try(var server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(5000);
            var received=CompletableFuture.supplyAsync(()-> {
                try(var socket=server.accept();
                    var input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    var output=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true)) {
                    socket.setSoTimeout(5000);
                    output.print("220 localhost test SMTP\r\n");output.flush();
                    StringBuilder data=new StringBuilder();
                    for(String command;(command=input.readLine())!=null;) {
                        if(command.startsWith("DATA")) {
                            output.print("354 Send message\r\n");output.flush();
                            for(String line;(line=input.readLine())!=null&&!line.equals(".");)data.append(line).append("\r\n");
                            output.print("250 Accepted\r\n");
                        } else if(command.startsWith("QUIT")) {
                            output.print("221 Goodbye\r\n");output.flush();break;
                        } else output.print("250 localhost\r\n");
                        output.flush();
                    }
                    return data.toString();
                } catch(IOException e) {throw new UncheckedIOException(e);}
            });
            var sender=new JavaMailSenderImpl();
            sender.setHost(server.getInetAddress().getHostAddress());sender.setPort(server.getLocalPort());sender.setDefaultEncoding("UTF-8");
            sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout","3000");
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout","3000");
            var repository=mock(MessageRepository.class);
            var contact=new ContactMessage();
            contact.id=42L;contact.name="Visiteur";contact.email="visitor@example.com";contact.company="Entreprise";
            contact.subject="Développement";contact.message="Un projet personnalisé à étudier.";contact.createdAt=Instant.now();
            contact.emailStatus="PENDING";
            when(repository.findTop10ByEmailStatusAndEmailNextAttemptLessThanEqualOrderByCreatedAtAsc(eq("PENDING"),any())).thenReturn(List.of(contact));
            new ContactEmailAlerts(repository,sender,"site@example.com","contact@example.com","https://codenova-solutions.fr/admin").sendPending();
            var mime=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(received.get(5,TimeUnit.SECONDS).getBytes(StandardCharsets.UTF_8)));
            assertEquals("CodeNova — Nouveau message de contact #42",mime.getSubject());
            assertEquals("visitor@example.com",mime.getReplyTo()[0].toString());
            assertTrue(mime.getContent().toString().contains("Un projet personnalisé à étudier."));
            assertEquals("SENT",contact.emailStatus);
            verify(repository).save(contact);
        }
    }
}
