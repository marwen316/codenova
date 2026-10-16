package fr.codenova;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import java.net.*;
import java.security.cert.CertificateException;
import java.util.*;
import javax.net.ssl.SSLException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

/** Log only an allowlisted category; exception text may include personal data or secrets. */
final class MailFailure {
    static String reason(Throwable error) {
        var pending=new ArrayDeque<Throwable>();
        var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        pending.add(error);
        while(!pending.isEmpty()&&seen.size()<64) {
            var current=pending.removeFirst();
            if(!seen.add(current))continue;
            if(current.getCause()!=null)pending.add(current.getCause());
            if(current instanceof MessagingException mail&&mail.getNextException()!=null)pending.add(mail.getNextException());
            if(current instanceof MailSendException mail)for(var nested:mail.getMessageExceptions())pending.add(nested);
        }
        if(seen.stream().anyMatch(e->e instanceof MailAuthenticationException||e instanceof AuthenticationFailedException))return "AUTHENTICATION_FAILED";
        if(seen.stream().anyMatch(e->e instanceof SSLException||e instanceof CertificateException))return "TLS_ERROR";
        if(seen.stream().anyMatch(e->e instanceof UnknownHostException))return "DNS_FAILURE";
        if(seen.stream().anyMatch(e->e instanceof SocketTimeoutException))return "NETWORK_TIMEOUT";
        if(seen.stream().anyMatch(e->e instanceof ConnectException))return "CONNECTION_FAILED";
        if(seen.stream().anyMatch(e->e instanceof SendFailedException))return "SMTP_REJECTED";
        if(seen.stream().anyMatch(e->e instanceof IllegalArgumentException))return "INVALID_ADDRESS_OR_CONFIGURATION";
        return "SMTP_ERROR";
    }
}
