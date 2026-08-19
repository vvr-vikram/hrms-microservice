package com.hrms.employee.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees/config")
public class ConfigController {

    @Value("${app.environment:default}")
    private String environment;

    @GetMapping("/env")
    public String getEnvironment() {
        return environment;
    }
}
