package fr.codenova;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional class MediaWorkflowTest {
 static final Path DIRECTORY=createDirectory();
 private static Path createDirectory(){try{return Files.createTempDirectory("codenova-media-test-");}catch(IOException e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("app.media-dir",DIRECTORY::toString);}
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 private byte[] png()throws Exception{var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,8,BufferedImage.TYPE_INT_RGB),"png",output);return output.toByteArray();}
 private org.springframework.test.web.servlet.ResultActions upload(byte[] data,String name)throws Exception{return mvc.perform(multipart("/api/admin/media").file(new MockMultipartFile("file",name,"application/octet-stream",data)).with(user("admin").roles("ADMIN")).with(csrf()));}
 @Test void uploadRequiresAuthenticationAndCsrf()throws Exception{
  mvc.perform(multipart("/api/admin/media").file(new MockMultipartFile("file","x.png","image/png",png())).with(csrf())).andExpect(status().isUnauthorized());
  mvc.perform(multipart("/api/admin/media").file(new MockMultipartFile("file","x.png","image/png",png())).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
 }
 @Test void rasterIsStoredUnderGeneratedNameAndPubliclyServed()throws Exception{
  var body=json.readTree(upload(png(),"../../original.png").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  String filename=body.get("filename").asText(),url=body.get("url").asText();
  assertTrue(filename.matches("[0-9a-f-]{36}\\.png"));assertTrue(Files.isRegularFile(DIRECTORY.resolve(filename)));assertEquals("original.png",body.get("originalName").asText());
  mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("X-Content-Type-Options","nosniff"));
  mvc.perform(get("/api/admin/media").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$[0].url").value(url));
 }
 @Test void simpleSvgIsAcceptedAndActiveContentRejected()throws Exception{
  upload("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'><path d='M0 0h10v10z' fill='#a879ff'/></svg>".getBytes(),"logo.svg").andExpect(status().isCreated());
  for(String svg:java.util.List.of("<svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'/>","<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>","<!DOCTYPE svg [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><svg xmlns='http://www.w3.org/2000/svg'><text>&x;</text></svg>","<svg xmlns='http://www.w3.org/2000/svg'><use href='https://example.com/image.svg'/></svg>"))upload(svg.getBytes(),"unsafe.svg").andExpect(status().isBadRequest());
 }
 @Test void arbitraryFilesAndOversizedImagesAreRejected()throws Exception{
  upload("not an image".getBytes(),"fake.png").andExpect(status().isBadRequest());
  upload(new byte[MediaStore.MAX_BYTES+1],"large.png").andExpect(status().isPayloadTooLarge());
 }
 @Test void imageCannotBeDeletedUntilReplacedAndDefaultsRemainAvailable()throws Exception{
  var body=json.readTree(upload(png(),"logo.png").andReturn().getResponse().getContentAsString());String url=body.get("url").asText(),id=body.get("id").asText();
  String template="{\"headline\":\"Présentation\",\"intro\":\"Introduction\",\"available\":true,\"settings\":{\"logoIcon\":\"%s\"}}";
  mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(template.formatted(url))).andExpect(status().isOk());
  mvc.perform(delete("/api/admin/media/"+id).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
  mvc.perform(put("/api/admin/content").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(template.formatted("/logo.svg"))).andExpect(status().isOk());
  mvc.perform(delete("/api/admin/media/"+id).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(url)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/admin/media/logo.svg").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNotFound());
 }
 @Test void storageQuotaCanBeReclaimedByDeletingUnusedFiles()throws Exception{
  var store=new MediaStore(DIRECTORY.resolve("quota").toString(),1024,1);String first=java.util.UUID.randomUUID()+".png",second=java.util.UUID.randomUUID()+".png";
  store.write(first,png());assertThrows(MediaProblem.class,()->store.write(second,png()));store.delete(first);store.write(second,png());assertTrue(Files.exists(store.path(second)));
 }
 @AfterAll static void cleanup()throws Exception{try(var paths=Files.walk(DIRECTORY)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
}
