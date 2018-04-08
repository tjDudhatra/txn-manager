package org.tdudhatra.n26.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.tdudhatra.n26.Application;
import org.tdudhatra.n26.model.Statistics;
import org.tdudhatra.n26.model.Transaction;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.tdudhatra.n26.helper.Helper.assertStatistics;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class N26ControllerIT {

    @Autowired
    WebApplicationContext applicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        mockMvc.perform(put("/clear-storage"));
    }

    @Test
    public void test_createTransaction_happyCase() throws Exception {
        MockHttpServletResponse response = addTransaction(100, Instant.now().minusSeconds(20));
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
    }

    @Test
    public void test_createTransaction_oldTransaction() throws Exception {
        MockHttpServletResponse response = addTransaction(100, Instant.now().minusSeconds(61));
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
    }

    @Test
    public void test_createTransaction_amountIsZero() throws Exception {
        MockHttpServletResponse response = addTransaction(0, Instant.now().minusSeconds(30));
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
    }

    @Test
    public void test_createTransaction_sameTime() throws Exception {
        Instant now = Instant.now();
        addTransaction(50, now.minusMillis(30));
        addTransaction(60, now.minusMillis(30));
        addTransaction(40, now.minusMillis(30));

        Statistics statistics = getStatistics();
        assertStatistics(statistics, 150, 60, 40, 3);
    }

    @Test
    public void test_createTransaction_manyTxnDiffAndSameTime() throws Exception {
        Instant now = Instant.now();
        addTransaction(50, now.minusMillis(30));
        addTransaction(60, now.minusMillis(30));
        addTransaction(40, now.minusMillis(40));
        addTransaction(40, now.minusMillis(50));

        Statistics statistics = getStatistics();
        assertStatistics(statistics, 190, 60, 40, 4);
    }

    @Test
    public void test_getStatistics_happyCase() throws Exception {
        addTransaction(100, Instant.now().minusSeconds(30));
        Statistics statistics = getStatistics();
        assertStatistics(statistics, 100, 100, 100, 1);
    }

    @Test
    public void test_getStatistics_whenNoTransaction() throws Exception {
        Statistics statistics = getStatistics();
        assertStatistics(statistics, 0, 0, 0, 0);
    }

    private MockHttpServletResponse addTransaction(double amount, Instant time) {
        try {
            String transactionStr = objectMapper.writeValueAsString(new Transaction(amount, time.toEpochMilli()));
            return mockMvc.perform(post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(transactionStr))
                    .andReturn()
                    .getResponse();
        } catch (Exception e) {
            return null;
        }
    }

    private Statistics getStatistics() {
        try {
            String responseStr = mockMvc.perform(get("/statistics"))
                    .andReturn()
                    .getResponse().getContentAsString();
            return objectMapper.readValue(responseStr, Statistics.class);
        } catch (Exception e) {
            return null;
        }
    }

}
