package fr.codenova;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class WorkflowTest {
@Autowired MockMvc mvc;
@org.springframework.beans.factory.annotation.Value("${ADMIN_USERNAME}") String username;
@org.springframework.beans.factory.annotation.Value("${ADMIN_PASSWORD}") String password;
@Test void administratorCanLogin()throws Exception{mvc.perform(post("/api/login").with(csrf()).param("username",username).param("password",password)).andExpect(status().isNoContent());}
@Test void wrongPasswordIsRejected()throws Exception{mvc.perform(post("/api/login").with(csrf()).param("username",username).param("password","incorrect")).andExpect(status().isUnauthorized());}
@Test void anonymousCannotReadMessages()throws Exception{mvc.perform(get("/api/admin/messages")).andExpect(status().isUnauthorized());}
@Test void contactRequiresConsent()throws Exception{mvc.perform(post("/api/contact").contentType("application/json").content("{\"name\":\"Test\",\"email\":\"test@example.com\",\"subject\":\"Conseil\",\"message\":\"Un projet de test\",\"consent\":false}")).andExpect(status().isBadRequest());}
@Test void contactIsStoredAndManaged()throws Exception{
mvc.perform(post("/api/contact").contentType("application/json").content("{\"name\":\"Test CodeNova\",\"email\":\"test@example.com\",\"subject\":\"Conseil\",\"message\":\"Un projet de test complet\",\"consent\":true}")).andExpect(status().isCreated());
mvc.perform(get("/api/admin/messages").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("test@example.com"));}
@Test void contentUpdatesRequireCsrf()throws Exception{mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"headline\":\"Bonjour\",\"intro\":\"Une introduction\",\"available\":true}")).andExpect(status().isForbidden());}
@Test void adminCanEditPublicContent()throws Exception{mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"headline\":\"Une ambition nouvelle\",\"intro\":\"Une introduction\",\"available\":false}")).andExpect(status().isOk());mvc.perform(get("/api/content")).andExpect(jsonPath("$.headline").value("Une ambition nouvelle"));}

@Autowired com.fasterxml.jackson.databind.ObjectMapper json;
private String payload(java.util.Map<String,String> settings)throws Exception{return json.writeValueAsString(java.util.Map.of("headline","Présentation conservée","intro","Une introduction enregistrée","available",true,"settings",settings));}
@Test void editorialSettingsPersistAndPartialUpdatesPreserveOtherFields()throws Exception{
 mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(payload(java.util.Map.of("companyName","Entreprise test","email","contact@example.com","service1Title","Audit personnalisé")))).andExpect(status().isOk());
 mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(payload(java.util.Map.of("phone","+33 1 23 45 67 89")))).andExpect(status().isOk());
 mvc.perform(get("/api/content")).andExpect(jsonPath("$.settings.companyName").value("Entreprise test")).andExpect(jsonPath("$.settings.email").value("contact@example.com")).andExpect(jsonPath("$.settings.service1Title").value("Audit personnalisé")).andExpect(jsonPath("$.settings.phone").value("+33 1 23 45 67 89")).andExpect(jsonPath("$.settings.city").value("Sartrouville"));
}
@Test void invalidSettingsAreRejectedWithoutChangingContent()throws Exception{
 for(var invalid:java.util.List.of(java.util.Map.of("email","not-an-email"),java.util.Map.of("logoIcon","javascript:alert(1)"),java.util.Map.of("logoFull","//example.com/logo.svg"),java.util.Map.of("unknownField","value"),java.util.Map.of("companyName",""))){
  mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(payload(invalid))).andExpect(status().isBadRequest());
 }
 mvc.perform(get("/api/content")).andExpect(jsonPath("$.settings.companyName").value("CodeNova"));
}
@Test void anonymousCannotUpdateEditorialSettings()throws Exception{
 mvc.perform(put("/api/admin/content").with(csrf()).contentType("application/json").content(payload(java.util.Map.of("companyName","Forbidden")))).andExpect(status().isUnauthorized());
}

@Test void themeColorsPersistWithOtherEditorialSettings()throws Exception{
 mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(payload(java.util.Map.of("themePrimary","#ff8800","themeBackground","#f1f5f9")))).andExpect(status().isOk());
 mvc.perform(get("/api/content")).andExpect(jsonPath("$.settings.themePrimary").value("#ff8800")).andExpect(jsonPath("$.settings.themeBackground").value("#f1f5f9")).andExpect(jsonPath("$.settings.companyName").value("CodeNova"));
}
@Test void malformedThemeColorsAreRejected()throws Exception{
 for(String value:java.util.List.of("red","#fff","#GG0000","#000000; color:red","url(https://example.com)")){
  mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(payload(java.util.Map.of("themePrimary",value)))).andExpect(status().isBadRequest());
 }
}
}
