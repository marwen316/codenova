package fr.codenova;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ContentSettings {
    static final Map<String,String> DEFAULTS;
    private static final Set<String> OPTIONAL=Set.of("registration","capital","hosting","legalExtra","heroImage","serviceCards");
    static {
        try (var stream=ContentSettings.class.getResourceAsStream("/content-defaults.json")) {
            DEFAULTS=Collections.unmodifiableMap(new ObjectMapper().readValue(stream,new TypeReference<LinkedHashMap<String,String>>(){}));
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    static void validate(Map<String,String> settings) {
        if(settings==null)return; // Keep the existing three-field API compatible.
        for(var entry:settings.entrySet()) {
            String key=entry.getKey(), value=entry.getValue();
            if(!DEFAULTS.containsKey(key)||value==null||(!OPTIONAL.contains(key)&&value.isBlank())||value.length()>(key.equals("serviceCards")?30000:5000))bad();
            if((key.matches("service[1-3]Title")||key.equals("formOtherSubject"))&&value.length()>100)bad();
            if(key.equals("serviceCards")&&!value.isBlank())validateCards(value);
            if(key.startsWith("theme")&&!value.matches("^#[0-9a-fA-F]{6}$"))bad();
            if(key.equals("email")&&!value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))bad();
            if((key.startsWith("logo")||key.equals("heroImage")||key.equals("socialImage"))&&!value.isBlank()&&(!value.startsWith("/")||value.startsWith("//")||value.contains("\\")||value.matches(".*[\\s<>].*")))bad();
        }
    }
    private static void validateCards(String value) {
        try {
            var cards=new ObjectMapper().readTree(value);
            if(!cards.isArray()||cards.size()>12)bad();
            Set<String> ids=new HashSet<>();
            for(var card:cards) {
                if(!card.isObject()||card.size()!=6)bad();
                String[] keys={"id","title","description","tags","icon","link"};
                int[] limits={80,100,1500,150,12,500};
                for(int i=0;i<keys.length;i++) {
                    var field=card.get(keys[i]);
                    if(field==null||!field.isTextual()||field.asText().length()>limits[i])bad();
                }
                if(card.get("id").asText().isBlank()||!ids.add(card.get("id").asText())||card.get("title").asText().isBlank()||card.get("description").asText().isBlank())bad();
                String link=card.get("link").asText();
                if(!link.isEmpty()&&!link.matches("^(#[A-Za-z][\\w-]*|/(?!/)[^\\s\\\\<>]*|https?://[^\\s\\\\<>]+|mailto:[^\\s\\\\<>]+|tel:\\+?[0-9 ()-]+)$"))bad();
            }
        } catch(Exception e) { bad(); }
    }
    private static void bad(){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid site content");}
}
