package com.jobboard.api.service;

import com.jobboard.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsCompliantPassword() {
        assertThatCode(() -> policy.validate("Str0ngPassw0rd")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> policy.validate("Abc12345"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertThatThrownBy(() -> policy.validate("OnlyLettersHere"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPasswordWithoutLetter() {
        assertThatThrownBy(() -> policy.validate("1234567890"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsCommonPassword() {
        assertThatThrownBy(() -> policy.validate("Password123"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> policy.validate(null))
                .isInstanceOf(BadRequestException.class);
    }
}
