package fr.codenova;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MediaRepository extends JpaRepository<MediaAsset,String> {
 List<MediaAsset> findAllByOrderByCreatedAtDesc();
 Optional<MediaAsset> findByFilename(String filename);
}
