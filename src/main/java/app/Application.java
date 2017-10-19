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

        // if upload doesn't upload anything then return 204 code
        if (req.getCount() <= 0) {
            return ResponseEntity.status(204).build();
        }

        boolean isTooOld = STAT_WINDOW_SECS < (time - req.getTimestamp());

        // client time could be ahead of server time. In general,
        // it's a normal case when we get request from the future
        // but we limit the difference by 1 seconds
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
