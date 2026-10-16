package fr.codenova;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface MessageRepository extends JpaRepository<ContactMessage,Long> {List<ContactMessage> findAllByOrderByCreatedAtDesc(); List<ContactMessage> findTop10ByEmailStatusAndEmailNextAttemptLessThanEqualOrderByCreatedAtAsc(String status,Instant now); void deleteByCreatedAtBefore(Instant cutoff);}
