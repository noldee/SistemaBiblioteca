package com.biblioteca.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biblioteca.service.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailService emailService;

    @GetMapping("/test-email")
    public String test() {

        emailService.enviarBienvenida(
                "1434909@senati.pe",
                "Walter"
        );

        return "EMAIL ENVIADO";
    }
}