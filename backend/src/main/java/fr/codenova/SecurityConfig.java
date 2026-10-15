package fr.codenova;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
@Configuration public class SecurityConfig {
@Bean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
@Bean UserDetailsService users(@Value("${ADMIN_USERNAME}") String username,@Value("${ADMIN_PASSWORD}") String password){if(password.length()<16)throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 16 characters");return new org.springframework.security.provisioning.InMemoryUserDetailsManager(User.withUsername(username).password(new BCryptPasswordEncoder().encode(password)).roles("ADMIN").build());}
@Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
return http.authorizeHttpRequests(a->a.requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/contact","/api/content","/api/csrf","/error").permitAll().anyRequest().denyAll())
.csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()).ignoringRequestMatchers("/api/contact"))
.formLogin(f->f.loginProcessingUrl("/api/login").successHandler((q,r,a)->r.setStatus(204)).failureHandler((q,r,e)->r.setStatus(401)).permitAll())
.logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((q,r,a)->r.setStatus(204)))
.exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->r.sendError(401))).build();
}}
