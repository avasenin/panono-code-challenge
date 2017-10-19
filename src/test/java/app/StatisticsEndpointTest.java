package app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class StatisticsEndpointTest {
    @Autowired
    private MockMvc mockMvc;

    public MockHttpServletRequestBuilder endpoint() {
        return get("/statistics").accept(MediaType.APPLICATION_JSON_UTF8);
    }

    public MvcResult upload(long ts, int count) throws Exception {
        return this.mockMvc.perform(post("/upload")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(String.format("{\"timestamp\":%d,\"count\":%d}", ts, count)))
                .andReturn();
    }

    @Test
    public void statisticsForLastMin() throws Exception {
        long now = Instant.now().getEpochSecond();

        upload(now - 5, 3);
        upload(now - 10, 10);
        upload(now - 7, 2);
        upload(now, 1);

        this.mockMvc.perform(endpoint())
                .andExpect(jsonPath("$.min", is(1)))
                .andExpect(jsonPath("$.max", is(10)))
                .andExpect(jsonPath("$.sum", is(16)))
                .andExpect(jsonPath("$.count", is(4)))
                .andExpect(jsonPath("$.avg", is(4.0)));
    }

    @Test
    public void statisticsOnlyOneSecond() throws Exception {
        long now = Instant.now().getEpochSecond();

        upload(now, 3);
        upload(now, 10);
        upload(now, 2);
        upload(now, 1);

        this.mockMvc.perform(endpoint())
                .andExpect(jsonPath("$.min", is(1)))
                .andExpect(jsonPath("$.max", is(10)))
                .andExpect(jsonPath("$.sum", is(16)))
                .andExpect(jsonPath("$.count", is(4)))
                .andExpect(jsonPath("$.avg", is(4.0)));
    }

    @Test
    public void statisticsWhenDataIsAbsent() throws Exception {
        this.mockMvc.perform(endpoint())
                .andExpect(jsonPath("$.min", is(0)))
                .andExpect(jsonPath("$.max", is(0)))
                .andExpect(jsonPath("$.sum", is(0)))
                .andExpect(jsonPath("$.count", is(0)))
                .andExpect(jsonPath("$.avg", is(0.0)));
    }
}
