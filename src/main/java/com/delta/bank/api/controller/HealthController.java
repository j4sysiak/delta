package com.delta.bank.api.controller;

import com.delta.bank.api.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "delta-mini-bank");
    }

    @GetMapping("/ready")
    public HealthResponse ready() {
        return new HealthResponse("READY", "delta-mini-bank");
    }

    @GetMapping("/live")
    public HealthResponse live() {
        return new HealthResponse("ALIVE", "delta-mini-bank");
    }
}