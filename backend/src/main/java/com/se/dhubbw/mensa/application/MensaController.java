package com.se.dhubbw.mensa.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class MensaController {
    @GetMapping("/mensa")
    public String testEndpoint() {
        return "success";
    }
}
