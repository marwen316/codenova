package fr.codenova;
import jakarta.mail.*;
import java.net.*;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.*;
import static org.junit.jupiter.api.Assertions.*;
class MailFailureTest {
    @Test void detectsAuthenticationWithoutExposingServerResponse() {
        assertEquals("AUTHENTICATION_FAILED",MailFailure.reason(new MailAuthenticationException("secret account data",new AuthenticationFailedException("535 secret details"))));
    }
    @Test void followsSmtpNestedAndPerMessageFailures() {
        var smtp=new MessagingException("connection details",new SSLHandshakeException("certificate details"));
        assertEquals("TLS_ERROR",MailFailure.reason(new MailSendException(Map.of("private message",smtp))));
        assertEquals("NETWORK_TIMEOUT",MailFailure.reason(new MessagingException("hidden",new SocketTimeoutException("hidden"))));
        assertEquals("DNS_FAILURE",MailFailure.reason(new UnknownHostException("private hostname")));
        assertEquals("CONNECTION_FAILED",MailFailure.reason(new ConnectException("hidden")));
        assertEquals("SMTP_REJECTED",MailFailure.reason(new SendFailedException("private address")));
        assertEquals("SMTP_ERROR",MailFailure.reason(new RuntimeException("password and message must not be logged")));
    }
}
