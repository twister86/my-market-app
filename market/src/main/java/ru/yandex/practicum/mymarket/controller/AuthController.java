package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null) model.addAttribute("error", "Неверный логин или пароль");
        if (logout != null) model.addAttribute("message", "Вы вышли из системы");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public Mono<String> register(ServerWebExchange exchange, Model model) {
        return exchange.getFormData().flatMap(form -> {
            String username = form.getFirst("username");
            String password = form.getFirst("password");

            if (username == null || username.isBlank() ||
                    password == null || password.isBlank()) {
                model.addAttribute("error", "Логин и пароль не могут быть пустыми");
                return Mono.just("register");
            }

            return userRepository.findByUsername(username)
                    .flatMap(existing -> {
                        model.addAttribute("error", "Пользователь уже существует");
                        return Mono.just("register");
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        User user = User.builder()
                                .username(username)
                                .password(passwordEncoder.encode(password))
                                .role("ROLE_USER")
                                .build();
                        return userRepository.save(user)
                                .thenReturn("redirect:/login?registered=true");
                    }));
        });
    }
}
