package fr.codenova;
import org.springframework.http.HttpStatus;
final class MediaProblem extends RuntimeException {
 final HttpStatus status;
 MediaProblem(HttpStatus status,String message){super(message);this.status=status;}
}
