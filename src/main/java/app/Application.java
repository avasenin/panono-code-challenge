package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@SpringBootApplication
@RestController
public class Application {

    private static long STAT_WINDOW_SECS = 60;

    @RequestMapping(value = "/upload", consumes = {"application/json"}, method = RequestMethod.POST)
    public ResponseEntity<Void> batchUpload(@RequestBody UploadRequest req) {
        long time = Instant.now().getEpochSecond();
        boolean isTooOld = STAT_WINDOW_SECS < (time - req.getTimestamp());
        boolean isFromFuture = 1 < (req.getTimestamp() - time);
        if (isTooOld || isFromFuture) {
            return ResponseEntity.status(204).build();
        }
        return ResponseEntity.accepted().build();
    }

    @RequestMapping("/")
    public ResponseEntity<String> greeting() {
        return ResponseEntity.ok("Hello World");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
