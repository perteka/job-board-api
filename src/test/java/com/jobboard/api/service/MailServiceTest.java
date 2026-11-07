package com.jobboard.api.service;

import com.jobboard.api.entity.enums.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailServiceTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final MailService service = new MailService(sender);

    @Test
    void sendsApplicationReceivedNotification() throws Exception {
        setEnabled(true);
        service.sendApplicationReceived("candidate@example.com", "Candidate", "Java Engineer");
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("candidate@example.com");
        assertThat(captor.getValue().getSubject()).contains("alındı");
        assertThat(captor.getValue().getText()).contains("Java Engineer");
    }

    @Test
    void sendsStatusUpdateNotification() throws Exception {
        setEnabled(true);
        service.sendApplicationStatusChanged("candidate@example.com", "Candidate", "Engineer", ApplicationStatus.REVIEWED);
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("güncellendi");
        assertThat(captor.getValue().getText()).contains("REVIEWED");
    }

    @Test
    void disabledMailDoesNotCallSender() {
        service.sendApplicationReceived("candidate@example.com", "Candidate", "Engineer");
        verifyNoInteractions(sender);
    }

    private void setEnabled(boolean enabled) throws Exception {
        Field field = MailService.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.setBoolean(service, enabled);
    }
}
