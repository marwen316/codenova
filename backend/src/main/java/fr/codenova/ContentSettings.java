package fr.codenova;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ContentSettings {
    static final Map<String,String> DEFAULTS;
    private static final Set<String> OPTIONAL=Set.of("registration","capital","hosting","legalExtra");
    static {
        try (var stream=ContentSettings.class.getResourceAsStream("/content-defaults.json")) {
            DEFAULTS=Collections.unmodifiableMap(new ObjectMapper().readValue(stream,new TypeReference<LinkedHashMap<String,String>>(){}));
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    static void validate(Map<String,String> settings) {
        if(settings==null)return; // Keep the existing three-field API compatible.
        for(var entry:settings.entrySet()) {
            String key=entry.getKey(), value=entry.getValue();
            if(!DEFAULTS.containsKey(key)||value==null||(!OPTIONAL.contains(key)&&value.isBlank())||value.length()>5000)bad();
            if((key.matches("service[1-3]Title")||key.equals("formOtherSubject"))&&value.length()>100)bad();
            if(key.equals("email")&&!value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))bad();
            if(key.startsWith("logo")&&(!value.startsWith("/")||value.startsWith("//")||value.contains("\\")||value.matches(".*[\\s<>].*")))bad();
        }
    }
    private static void bad(){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid site content");}
}
