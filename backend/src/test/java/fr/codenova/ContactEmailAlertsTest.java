package fr.codenova;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.contact-alerts.enabled=true","app.contact-alerts.from=site@example.com","app.contact-alerts.to=contact@example.com","app.contact-alerts.initial-delay-ms=86400000"})
@AutoConfigureMockMvc @Transactional
class ContactEmailAlertsTest {
    @Autowired MockMvc mvc;
    @Autowired MessageRepository messages;
    @Autowired ContactEmailAlerts alerts;
    @MockitoBean JavaMailSender mail;
    private ContactMessage submit() throws Exception {
        mvc.perform(post("/api/contact").contentType("application/json").content("{\"name\":\"Email alert test\",\"email\":\"visitor@example.com\",\"company\":\"Entreprise\",\"subject\":\"Audit\",\"message\":\"Un projet à discuter ensemble\",\"consent\":true}"))
            .andExpect(status().isCreated());
        return messages.findAllByOrderByCreatedAtDesc().stream().filter(m->m.name.equals("Email alert test")).findFirst().orElseThrow();
    }
    @Test void savedMessageIsQueuedThenDeliveredOnceWithReplyTo() throws Exception {
        var message=submit();
        assertEquals("PENDING",message.emailStatus);
        verifyNoInteractions(mail);
        alerts.sendPending();
        var sent=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(sent.capture());
        assertEquals("site@example.com",sent.getValue().getFrom());
        assertArrayEquals(new String[]{"contact@example.com"},sent.getValue().getTo());
        assertEquals("visitor@example.com",sent.getValue().getReplyTo());
        assertTrue(sent.getValue().getText().contains("Un projet à discuter ensemble"));
        assertTrue(sent.getValue().getText().contains("https://codenova-solutions.fr/admin"));
        assertEquals("SENT",messages.findById(message.id).orElseThrow().emailStatus);
        alerts.sendPending();
        verifyNoMoreInteractions(mail);
    }
    @Test void smtpFailureKeepsContactAndRetriesUntilFiveAttempts() throws Exception {
        doThrow(new MailSendException("SMTP unavailable")).when(mail).send(any(SimpleMailMessage.class));
        var message=submit();
        alerts.sendPending();
        var stored=messages.findById(message.id).orElseThrow();
        assertEquals("PENDING",stored.emailStatus);
        assertEquals(1,stored.emailAttempts);
        assertTrue(stored.emailNextAttempt.isAfter(Instant.now()));
        alerts.sendPending();
        verify(mail,times(1)).send(any(SimpleMailMessage.class));
        stored.emailAttempts=4;
        stored.emailNextAttempt=Instant.now().minusSeconds(1);
        messages.save(stored);
        alerts.sendPending();
        assertEquals("FAILED",stored.emailStatus);
        assertEquals(5,stored.emailAttempts);
        assertEquals("Un projet à discuter ensemble",stored.message);
    }
    @Test void honeypotAndInvalidFormNeverQueueOrSend() throws Exception {
        long count=messages.count();
        mvc.perform(post("/api/contact").contentType("application/json").content("{\"name\":\"Bot\",\"email\":\"bot@example.com\",\"subject\":\"Test\",\"message\":\"Message automatique\",\"consent\":true,\"website\":\"bot\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/contact").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        assertEquals(count,messages.count());
        alerts.sendPending();
        verifyNoInteractions(mail);
    }
    @Test void existingMessagesAreNotRetrospectivelyEmailed() throws Exception {
        var message=submit();
        message.emailStatus="NONE";
        messages.save(message);
        alerts.sendPending();
        verifyNoInteractions(mail);
    }
}
