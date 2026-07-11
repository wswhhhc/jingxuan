package com.example.runtime.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "ok", true,
                "service", "runtime-demo-backend"
        );
    }

    @GetMapping("/api/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "message", "hello from runtime demo backend"
        );
    }
}
