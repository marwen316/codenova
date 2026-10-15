package fr.codenova;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.*;
import org.springframework.core.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.IOException;

@RestController public class MediaController {
 private final MediaRepository assets;private final MediaStore store;private final ContentRepository content;
 public MediaController(MediaRepository assets,MediaStore store,ContentRepository content){this.assets=assets;this.store=store;this.content=content;}
 @GetMapping("/api/admin/media") public List<MediaAsset> list(){return assets.findAllByOrderByCreatedAtDesc();}
 @PostMapping(value="/api/admin/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @ResponseStatus(HttpStatus.CREATED) public MediaAsset upload(@RequestParam("file") MultipartFile file){
  if(file.getSize()==0||file.getSize()>MediaStore.MAX_BYTES)throw new MediaProblem(HttpStatus.PAYLOAD_TOO_LARGE,"Choisissez une image de 4 Mo maximum.");
  MediaStore.Image image;
  try{image=store.normalize(file.getBytes());}catch(IOException e){throw new MediaProblem(HttpStatus.BAD_REQUEST,"Impossible de lire le fichier.");}
  MediaAsset asset=new MediaAsset();asset.id=UUID.randomUUID().toString();asset.filename=asset.id+"."+image.extension();
  String original=Objects.requireNonNullElse(file.getOriginalFilename(),"image").replace('\\','/');original=original.substring(original.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
  asset.originalName=original.isBlank()?"image":original.substring(0,Math.min(200,original.length()));
  asset.contentType=image.contentType();asset.sizeBytes=image.bytes().length;asset.createdAt=Instant.now();
  store.write(asset.filename,image.bytes());
  try{return assets.saveAndFlush(asset);}catch(RuntimeException e){store.delete(asset.filename);throw new MediaProblem(HttpStatus.SERVICE_UNAVAILABLE,"L’image n’a pas pu être enregistrée. Réessayez.");}
 }
 @DeleteMapping("/api/admin/media/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){
  MediaAsset asset=assets.findById(id).orElseThrow(()->new MediaProblem(HttpStatus.NOT_FOUND,"Image introuvable."));
  if(content.findById(1L).map(c->c.settings.values().stream().anyMatch(value->value.equals(asset.getUrl())||value.startsWith(asset.getUrl()+"?"))).orElse(false))throw new MediaProblem(HttpStatus.CONFLICT,"Cette image est encore utilisée sur le site. Remplacez-la et enregistrez le contenu avant de la supprimer.");
  store.delete(asset.filename);assets.delete(asset);
 }
 @GetMapping("/api/media/{filename}") public ResponseEntity<Resource> image(@PathVariable String filename){
  MediaAsset asset=assets.findByFilename(filename).orElseThrow(()->new MediaProblem(HttpStatus.NOT_FOUND,"Image introuvable."));
  Path path=store.path(asset.filename);if(!Files.isRegularFile(path))throw new MediaProblem(HttpStatus.NOT_FOUND,"Fichier image introuvable.");
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.contentType)).contentLength(asset.sizeBytes)
   .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
   .header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; style-src 'unsafe-inline'; sandbox")
   .header("Content-Disposition","inline; filename=\""+asset.filename+"\"").body(new FileSystemResource(path));
 }
}
@RestControllerAdvice class MediaErrors {
 @ExceptionHandler(MediaProblem.class) ResponseEntity<Map<String,String>> media(MediaProblem error){return ResponseEntity.status(error.status).body(Map.of("message",error.getMessage()));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<Map<String,String>> size(MaxUploadSizeExceededException error){return ResponseEntity.status(413).body(Map.of("message","Choisissez une image de 4 Mo maximum."));}
}
