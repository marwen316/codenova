package fr.codenova;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface MessageRepository extends JpaRepository<ContactMessage,Long> {List<ContactMessage> findAllByOrderByCreatedAtDesc(); void deleteByCreatedAtBefore(Instant cutoff);}
