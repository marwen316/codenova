package fr.codenova;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ServiceCardsTest {
    private String card(String link) {return "{\"id\":\"test\",\"title\":\"Audit\",\"description\":\"Conseil informatique\",\"tags\":\"IT\",\"icon\":\"◇\",\"link\":\""+link+"\"}";}
    @Test void acceptsEmptyAndConfiguredCards() {
        for(String link:new String[]{"", "#contact", "/conseil", "https://example.com", "mailto:contact@example.com", "tel:+33758628689"})
            assertDoesNotThrow(()->ContentSettings.validate(Map.of("serviceCards","["+card(link)+"]")));
        assertDoesNotThrow(()->ContentSettings.validate(Map.of("serviceCards","[]")));
    }
    @Test void rejectsUnsafeLinksAndMalformedCollections() {
        for(String link:new String[]{"javascript:alert(1)","//example.com","data:text/html,test"})
            assertThrows(Exception.class,()->ContentSettings.validate(Map.of("serviceCards","["+card(link)+"]")));
        for(String value:new String[]{"{}","[{}]","["+card("")+","+card("")+"]","["+String.join(",",java.util.Collections.nCopies(13,card("")))+"]"})
            assertThrows(Exception.class,()->ContentSettings.validate(Map.of("serviceCards",value)));
    }
}
