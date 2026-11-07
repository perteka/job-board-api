package com.jobboard.api.service;

import com.jobboard.api.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PasswordPolicy {

    private static final Pattern LETTER_AND_DIGIT = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "123456", "12345678", "123456789",
            "qwerty", "qwerty123", "letmein", "welcome1", "admin123", "iloveyou",
            "monkey123", "dragon123", "sunshine1", "princess1", "football1", "baseball1"
    );

    public void validate(String password) {
        if (password == null
                || password.length() < 10
                || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Şifre 10-72 karakter arasında olmalı");
        }
        if (!LETTER_AND_DIGIT.matcher(password).matches()) {
            throw new BadRequestException("Şifre en az bir harf ve bir rakam içermeli");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Bu şifre çok yaygın, lütfen farklı bir şifre seçin");
        }
    }
}
