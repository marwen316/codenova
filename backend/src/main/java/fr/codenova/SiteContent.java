package fr.codenova;
import jakarta.persistence.*;
@Entity public class SiteContent {
@Id public Long id;
@Column(nullable=false,length=100) public String headline;
@Column(nullable=false,length=500) public String intro;
@Column(nullable=false) public boolean available;
}
