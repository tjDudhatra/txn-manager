package org.tdudhatra.n26.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.tdudhatra.n26.model.Statistics;
import org.tdudhatra.n26.model.Transaction;
import org.tdudhatra.n26.service.StorageService;

import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

@RestController
public class N26Controller {

    @Autowired
    private StorageService storageService;

    @PostMapping(path = "transactions")
    public void createTransaction(@RequestBody Transaction transaction, HttpServletResponse response) {
        if (Instant.ofEpochMilli(transaction.getTimestamp()).isBefore(Instant.now().minusSeconds(60))) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }
        boolean result = storageService.saveTransaction(transaction);
        int statusCode = result ? HttpStatus.CREATED.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();
        response.setStatus(statusCode);
    }

    @GetMapping(path = "statistics")
    public Statistics getStatistics() {
        return storageService.getStatistics();
    }

    @PutMapping(path = "clear-storage")
    public void clearStorage() {
        storageService.clearStorage();
    }

}
