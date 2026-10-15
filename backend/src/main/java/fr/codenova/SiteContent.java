package fr.codenova;
import jakarta.persistence.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity public class SiteContent {
@Id public Long id;
@Column(nullable=false,length=100) public String headline;
@Column(nullable=false,length=500) public String intro;
@Column(nullable=false) public boolean available;
@JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") public Map<String,String> settings=new LinkedHashMap<>();
}
