package fr.codenova;
import jakarta.persistence.*;
import java.time.Instant;
@Entity public class ContactMessage {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
@Column(nullable=false,length=120) public String name;
@Column(nullable=false,length=254) public String email;
@Column(nullable=false,length=160) public String company;
@Column(nullable=false,length=100) public String subject;
@Column(nullable=false,length=5000) public String message;
@Column(nullable=false) public Instant createdAt;
@Column(nullable=false) public boolean read;
@Column(nullable=false,length=16) public String emailStatus="NONE";
@Column(nullable=false) public int emailAttempts;
public Instant emailNextAttempt;
}
