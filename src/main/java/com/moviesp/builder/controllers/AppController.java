package com.moviesp.builder.controllers;

import com.moviesp.builder.services.IntegratorService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AppController {

    private final IntegratorService integratorService;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/firstData")
    public String testInfo() {
        integratorService.firstData();
        return "Info charged";
    }

}
