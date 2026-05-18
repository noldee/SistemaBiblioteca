package com.biblioteca.controller;

import com.biblioteca.dto.RegistroUsuarioDto;
import com.biblioteca.service.EmailService;
import com.biblioteca.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final EmailService emailService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registroPage(Model model) {
        model.addAttribute("dto", new RegistroUsuarioDto());
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(
            @Valid @ModelAttribute("dto") RegistroUsuarioDto dto,
            BindingResult result,
            RedirectAttributes flash,
            Model model) {

        if (result.hasErrors()) {
            return "auth/registro";
        }

        try {
            var usuario = usuarioService.registrar(dto);
            emailService.enviarBienvenida(usuario.getEmail(), usuario.getNombre());
            flash.addFlashAttribute("success",
                    "¡Cuenta creada! Ya puedes iniciar sesión.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/registro";
        }
    }
}