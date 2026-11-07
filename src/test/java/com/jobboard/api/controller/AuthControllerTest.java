package com.jobboard.api.controller;

import com.jobboard.api.service.AuthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthControllerTest {

    @Test
    void logoutWithoutPrincipal_returnsUnauthorizedWithoutCallingService() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        var response = controller.logout(null);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(authService);
    }
}
