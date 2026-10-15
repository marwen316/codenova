package fr.codenova;
import jakarta.persistence.*;
import java.time.Instant;
@Entity public class MediaAsset {
 @Id @Column(length=36) public String id;
 @Column(nullable=false,length=48,unique=true) public String filename;
 @Column(nullable=false,length=200) public String originalName;
 @Column(nullable=false,length=40) public String contentType;
 @Column(nullable=false) public long sizeBytes;
 @Column(nullable=false) public Instant createdAt;
 public String getUrl(){return "/api/media/"+filename;}
}
