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
}
