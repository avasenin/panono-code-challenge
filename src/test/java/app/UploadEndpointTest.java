package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UploadEndpointTest {
    @Autowired
    private MockMvc mockMvc;

    public static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonContent = mapper.writeValueAsString(obj);
            return jsonContent;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MockHttpServletRequestBuilder endpoint() {
        return post("/upload").contentType(MediaType.APPLICATION_JSON_UTF8);
    }

    @Test
    public void getRequestShouldFail() throws Exception {
        this.mockMvc.perform(get("/upload"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void postValidBatch() throws Exception {
        UploadRequest requestBody = new UploadRequest(Instant.now().getEpochSecond(), 2);
        MockHttpServletRequestBuilder req = endpoint()
                .content(asJsonString(requestBody));

        this.mockMvc.perform(req)
                .andExpect(status().isAccepted());
    }

    @Test
    public void postExpiredBatch() throws Exception {
        UploadRequest requestBody = new UploadRequest(Instant.now().getEpochSecond() - 100, 2);
        MockHttpServletRequestBuilder req = endpoint()
                .content(asJsonString(requestBody));

        this.mockMvc.perform(req)
                .andExpect(status().is(204));
    }

    @Test
    public void postBatchFromFuture() throws Exception {
        UploadRequest requestBody = new UploadRequest(Instant.now().getEpochSecond() + 3, 2);
        MockHttpServletRequestBuilder req = endpoint()
                .content(asJsonString(requestBody));

        this.mockMvc.perform(req)
                .andExpect(status().is(204));
    }

    @Test
    public void postZeroPanoramas() throws Exception {
        UploadRequest requestBody = new UploadRequest(Instant.now().getEpochSecond() - 10, 0);
        MockHttpServletRequestBuilder req = endpoint()
                .content(asJsonString(requestBody));

        this.mockMvc.perform(req)
                .andExpect(status().is(204));
    }
}
