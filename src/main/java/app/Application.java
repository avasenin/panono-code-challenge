package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@SpringBootApplication
@RestController
public class Application {
    private static Logger LOG = LoggerFactory.getLogger(Application.class);
    private static int STAT_WINDOW_SECS = 60;

    @Bean
    Statistics statistics() {
       return new Statistics(STAT_WINDOW_SECS);
    }

    @Autowired
    private Statistics statistics;


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

        try {
            statistics.bump(req.getTimestamp(), req.getCount());
        } catch (Exception ex) {
            // I assumed that upload operation should not fail
            // if something wrong with statistics. Wrap to try-catch
            // and output exception to the log if it's thrown.
            LOG.error("Failed to report statistics", ex);
        }

        return ResponseEntity.accepted().build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
