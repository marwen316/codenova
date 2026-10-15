package fr.codenova;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service public class MediaStore {
 static final int MAX_BYTES=4*1024*1024;
 private final Path directory;
 private final long maxBytes;
 private final int maxFiles;
 public record Image(byte[] bytes,String extension,String contentType){}
 public MediaStore(@Value("${app.media-dir:.local/media}") String location,
                   @Value("${app.media-max-bytes:52428800}") long maxBytes,
                   @Value("${app.media-max-files:50}") int maxFiles){
  this.directory=Path.of(location).toAbsolutePath().normalize();this.maxBytes=maxBytes;this.maxFiles=maxFiles;
 }
 public Path path(String filename){
  if(!filename.matches("[0-9a-f-]{36}\\.(png|jpg|svg)"))throw new MediaProblem(HttpStatus.NOT_FOUND,"Image introuvable.");
  return directory.resolve(filename);
 }
 public Image normalize(byte[] data){
  if(data.length==0||data.length>MAX_BYTES)throw invalid("L’image doit faire entre 1 octet et 4 Mo.");
  String prefix=new String(data,0,Math.min(data.length,512),java.nio.charset.StandardCharsets.UTF_8).stripLeading();
  if(prefix.startsWith("<")||prefix.startsWith("\ufeff<"))return svg(data);
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))){
   var readers=ImageIO.getImageReaders(input);
   if(!readers.hasNext())throw invalid("Format non reconnu. Utilisez PNG, JPEG ou SVG.");
   var reader=readers.next();
   try{
    String format=reader.getFormatName().toLowerCase(Locale.ROOT);
    if(!Set.of("png","jpeg","jpg").contains(format))throw invalid("Formats acceptés : PNG, JPEG et SVG.");
    reader.setInput(input,true,true);
    int width=reader.getWidth(0),height=reader.getHeight(0);
    if(width<1||height<1||(long)width*height>16_000_000L)throw invalid("L’image ne doit pas dépasser 16 millions de pixels.");
    var pixels=reader.read(0);var output=new ByteArrayOutputStream();
    boolean jpeg=!format.equals("png");
    if(!ImageIO.write(pixels,jpeg?"jpeg":"png",output))throw invalid("Impossible de traiter l’image.");
    byte[] normalized=output.toByteArray();
    if(normalized.length>MAX_BYTES)throw invalid("L’image traitée dépasse 4 Mo. Réduisez sa taille.");
    return new Image(normalized,jpeg?"jpg":"png",jpeg?"image/jpeg":"image/png");
   }finally{reader.dispose();}
  }catch(MediaProblem e){throw e;}catch(Exception e){throw invalid("Fichier image illisible ou endommagé.");}
 }
 private Image svg(byte[] bytes){
  try{
   var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
   factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
   factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
   factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
   factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
   factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
   factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth","40");
   factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
   var builder=factory.newDocumentBuilder();builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
   var document=builder.parse(new ByteArrayInputStream(bytes));
   if(!document.getDocumentElement().getLocalName().equals("svg"))throw invalid("Le fichier doit contenir un SVG valide.");
   inspect(document.getDocumentElement(),0,new int[]{0});
   var transformerFactory=TransformerFactory.newInstance();transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
   transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
   var transformer=transformerFactory.newTransformer();transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
   var output=new ByteArrayOutputStream();transformer.transform(new DOMSource(document.getDocumentElement()),new StreamResult(output));
   if(output.size()>MAX_BYTES)throw invalid("Le SVG traité dépasse 4 Mo.");
   return new Image(output.toByteArray(),"svg","image/svg+xml");
  }catch(MediaProblem e){throw e;}catch(Exception e){throw invalid("SVG non pris en charge. Exportez une version simple, sans script ni ressource externe.");}
 }
 private void inspect(Element element,int depth,int[] count){
  if(depth>40||++count[0]>10000)throw invalid("SVG trop complexe.");
  if(!"http://www.w3.org/2000/svg".equals(element.getNamespaceURI()))throw invalid("Espace de noms SVG non valide.");
  var elements=Set.of("svg","g","path","rect","circle","ellipse","line","polyline","polygon","text","tspan","defs","linearGradient","radialGradient","stop","clipPath","mask","title","desc","use","symbol");
  var attributes=Set.of("id","viewBox","width","height","x","y","x1","y1","x2","y2","cx","cy","r","rx","ry","d","points","fill","fill-opacity","fill-rule","stroke","stroke-width","stroke-linecap","stroke-linejoin","stroke-miterlimit","stroke-dasharray","stroke-dashoffset","stroke-opacity","opacity","transform","gradientUnits","gradientTransform","offset","stop-color","stop-opacity","clip-path","clip-rule","mask","maskUnits","maskContentUnits","font-family","font-size","font-weight","font-style","text-anchor","dominant-baseline","letter-spacing","dx","dy","preserveAspectRatio","role","aria-labelledby","aria-label","href","version","xmlns","xmlns:xlink","xlink:href");
  if(!elements.contains(element.getLocalName()))throw invalid("SVG contenant un élément non autorisé : "+element.getLocalName()+".");
  var attrs=element.getAttributes();
  for(int i=0;i<attrs.getLength();i++){
   var attr=attrs.item(i);String name=attr.getNodeName(),value=attr.getNodeValue();
   if(!attributes.contains(name))throw invalid("SVG contenant un attribut non autorisé : "+name+".");
   if(name.equals("xmlns")||name.equals("xmlns:xlink"))continue;
   if(name.equals("href")||name.equals("xlink:href")){if(!value.matches("#[A-Za-z_][A-Za-z0-9_.:-]*"))throw invalid("Les liens SVG externes ne sont pas acceptés.");}
   String lower=value.toLowerCase(Locale.ROOT);
   if(lower.contains("url")&&!value.matches("url\\(#[A-Za-z_][A-Za-z0-9_.:-]*\\)"))throw invalid("Les ressources SVG externes ne sont pas acceptées.");
   if(lower.contains("javascript:")||lower.contains("data:")||lower.contains("expression("))throw invalid("SVG non autorisé.");
  }
  var nodes=element.getChildNodes();for(int i=nodes.getLength()-1;i>=0;i--){
   Node child=nodes.item(i);
   if(child instanceof Element nested)inspect(nested,depth+1,count);
   else if(child.getNodeType()!=Node.TEXT_NODE&&child.getNodeType()!=Node.CDATA_SECTION_NODE)element.removeChild(child);
  }
 }
 public synchronized void write(String filename,byte[] bytes){
  Path temporary=null;
  try{
   Files.createDirectories(directory);
   try(var files=Files.list(directory)){
    var assets=files.filter(Files::isRegularFile).toList();long used=0;for(var asset:assets)used+=Files.size(asset);
    if(assets.size()>=maxFiles||used+bytes.length>maxBytes)throw new MediaProblem(HttpStatus.CONFLICT,"La médiathèque est pleine (50 fichiers ou 50 Mo). Supprimez des images inutilisées.");
   }
   temporary=Files.createTempFile(directory,"upload-",".tmp");Files.write(temporary,bytes);
   Files.move(temporary,path(filename),StandardCopyOption.ATOMIC_MOVE);temporary=null;
  }catch(MediaProblem e){throw e;}catch(IOException e){throw new MediaProblem(HttpStatus.SERVICE_UNAVAILABLE,"Stockage indisponible. Vérifiez l’espace disque et les permissions du volume d’images.");}
  finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){}}
 }
 public void delete(String filename){try{Files.deleteIfExists(path(filename));}catch(IOException e){throw new MediaProblem(HttpStatus.SERVICE_UNAVAILABLE,"Suppression impossible sur le stockage.");}}
 private static MediaProblem invalid(String message){return new MediaProblem(HttpStatus.BAD_REQUEST,message);}
}
