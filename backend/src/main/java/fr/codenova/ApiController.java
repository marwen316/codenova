package fr.codenova;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api") public class ApiController {
private final MessageRepository messages; private final ContentRepository content;
private final Map<String,Deque<Instant>> attempts=new HashMap<>();
public ApiController(MessageRepository m,ContentRepository c){messages=m;content=c;}
public record Contact(@NotBlank @Size(max=120) String name,@NotBlank @Email @Size(max=254) String email,@Size(max=160) String company,@NotBlank @Size(max=100) String subject,@NotBlank @Size(min=10,max=5000) String message,@AssertTrue boolean consent,@Size(max=500) String website){}
public record Content(@NotBlank @Size(max=100) String headline,@NotBlank @Size(max=500) String intro,boolean available,@Size(max=128) Map<@NotBlank String,@NotNull @Size(max=30000) String> settings){}
@GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
@GetMapping("/content") public SiteContent content(){SiteContent c=content.findById(1L).orElseThrow();Map<String,String> merged=new LinkedHashMap<>(ContentSettings.DEFAULTS);merged.putAll(c.settings);c.settings=merged;return c;}
@PostMapping("/contact") @ResponseStatus(HttpStatus.CREATED) public void contact(@Valid @RequestBody Contact input,HttpServletRequest request){
if(input.website()!=null&&!input.website().isBlank())return;
limit(request.getRemoteAddr());
ContactMessage m=new ContactMessage();m.name=input.name().strip();m.email=input.email().strip();m.company=input.company()==null?"":input.company().strip();m.subject=input.subject();m.message=input.message().strip();m.createdAt=Instant.now();messages.save(m);
}
private synchronized void limit(String ip){Instant cutoff=Instant.now().minusSeconds(3600);attempts.values().forEach(q->{while(!q.isEmpty()&&q.peekFirst().isBefore(cutoff))q.removeFirst();});attempts.entrySet().removeIf(e->e.getValue().isEmpty());if(attempts.size()>10000&&!attempts.containsKey(ip))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);Deque<Instant> q=attempts.computeIfAbsent(ip,k->new ArrayDeque<>());if(q.size()>=5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);q.addLast(Instant.now());}
@GetMapping("/admin/messages") public List<ContactMessage> list(){return messages.findAllByOrderByCreatedAtDesc();}
@PatchMapping("/admin/messages/{id}/read") public void read(@PathVariable Long id){ContactMessage m=messages.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));m.read=true;messages.save(m);}
@DeleteMapping("/admin/messages/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){messages.deleteById(id);}
@PutMapping("/admin/content") @Transactional public SiteContent update(@Valid @RequestBody Content input){
ContentSettings.validate(input.settings());
SiteContent c=content();c.headline=input.headline();c.intro=input.intro();c.available=input.available();
if(input.settings()!=null)c.settings.putAll(input.settings());
return content.save(c);
}
@Scheduled(cron="0 0 3 * * *") @Transactional public void purge(){messages.deleteByCreatedAtBefore(Instant.now().minus(Duration.ofDays(365)));}
}
