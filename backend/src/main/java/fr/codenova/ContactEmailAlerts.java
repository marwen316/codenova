package fr.codenova;

import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Persistent queue for a single backend instance; SMTP delivery is at least once. */
@Component
@ConditionalOnProperty(name="app.contact-alerts.enabled", havingValue="true")
public class ContactEmailAlerts {
    private static final Logger log = LoggerFactory.getLogger(ContactEmailAlerts.class);
    private final MessageRepository messages;
    private final JavaMailSender mail;
    private final String from, to, adminUrl;

    public ContactEmailAlerts(MessageRepository messages, JavaMailSender mail,
            @Value("${app.contact-alerts.from}") String from,
            @Value("${app.contact-alerts.to}") String to,
            @Value("${app.contact-alerts.admin-url}") String adminUrl) {
        this.messages=messages;
        this.mail=mail;
        this.from=address(from);
        this.to=address(to);
        if(!adminUrl.matches("https?://[^\\s<>]+"))throw new IllegalArgumentException("Invalid CONTACT_ALERTS_ADMIN_URL");
        this.adminUrl=adminUrl;
    }
    private static String address(String value) {
        try {
            if(value==null||value.isBlank()||value.contains("\r")||value.contains("\n"))throw new IllegalArgumentException();
            var address=new InternetAddress(value,true);
            address.validate();
            return address.getAddress();
        } catch(Exception e) { throw new IllegalArgumentException("Configure valid CONTACT_ALERTS_FROM and CONTACT_ALERTS_TO before enabling alerts"); }
    }

    @Scheduled(fixedDelayString="${app.contact-alerts.poll-ms:30000}", initialDelayString="${app.contact-alerts.initial-delay-ms:30000}")
    public void sendPending() {
        for(var message:messages.findTop10ByEmailStatusAndEmailNextAttemptLessThanEqualOrderByCreatedAtAsc("PENDING",Instant.now())) {
            message.emailAttempts++;
            try {
                var email=new SimpleMailMessage();
                email.setFrom(from);
                email.setTo(to);
                email.setReplyTo(address(message.email));
                email.setSubject("CodeNova — Nouveau message de contact #"+message.id);
                email.setText("Un nouveau message a été reçu sur CodeNova.\n\n"
                    +"Nom : "+message.name+"\nE-mail : "+message.email+"\nEntreprise : "+message.company
                    +"\nSujet : "+message.subject+"\n\n"+message.message
                    +"\n\nConsulter les messages : "+adminUrl+"\n\nRépondez à cet e-mail pour contacter l’expéditeur.");
                mail.send(email);
                message.emailStatus="SENT";
                message.emailNextAttempt=null;
            } catch(Exception error) {
                message.emailStatus=message.emailAttempts>=5?"FAILED":"PENDING";
                message.emailNextAttempt=message.emailAttempts>=5?null:Instant.now().plusSeconds(60L<<(message.emailAttempts-1));
                // Never log message contents, recipient addresses or SMTP exception details.
                log.warn("Contact email alert failed for message {}, attempt {}; status {}; reason {}",message.id,message.emailAttempts,message.emailStatus,MailFailure.reason(error));
            }
            messages.save(message);
        }
    }
}
