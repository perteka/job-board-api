package com.jobboard.api.service;

import com.jobboard.api.entity.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Async("mailTaskExecutor")
    public void sendApplicationReceived(String to, String candidateName, String jobTitle) {
        if (!enabled) {
            return;
        }
        send(
                to,
                "Başvurunuz alındı",
                "Merhaba " + candidateName + ",\n\n"
                        + "\"" + jobTitle + "\" ilanına başvurunuz alındı. "
                        + "Durum değişikliklerini hesabınızdan takip edebilirsiniz.\n\n"
                        + "İyi günler."
        );
    }

    @Async("mailTaskExecutor")
    public void sendApplicationStatusChanged(
            String to,
            String candidateName,
            String jobTitle,
            ApplicationStatus status
    ) {
        if (!enabled) {
            return;
        }
        send(
                to,
                "Başvuru durumunuz güncellendi",
                "Merhaba " + candidateName + ",\n\n"
                        + "\"" + jobTitle + "\" ilanına yaptığınız başvurunun durumu "
                        + status.name() + " olarak güncellendi.\n\n"
                        + "İyi günler."
        );
    }

    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String to, String candidateName, String verificationUrl) {
        if (!enabled) {
            return;
        }
        send(
                to,
                "E-posta adresinizi doğrulayın",
                "Merhaba " + candidateName + ",\n\n"
                        + "Hesabınızı etkinleştirmek için aşağıdaki bağlantıya tıklayın:\n"
                        + verificationUrl + "\n\n"
                        + "Bağlantı sınırlı süre geçerlidir. Bu kaydı siz yapmadıysanız bu e-postayı yok sayabilirsiniz.\n\n"
                        + "İyi günler."
        );
    }

    @Async("mailTaskExecutor")
    public void sendPasswordResetEmail(String to, String candidateName, String resetUrl) {
        if (!enabled) {
            return;
        }
        send(
                to,
                "Şifre sıfırlama",
                "Merhaba " + candidateName + ",\n\n"
                        + "Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın:\n"
                        + resetUrl + "\n\n"
                        + "Bağlantı kısa süre geçerlidir. Bu talebi siz yapmadıysanız bu e-postayı yok sayabilirsiniz.\n\n"
                        + "İyi günler."
        );
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("E-posta gönderilemedi: {}", to, exception);
        }
    }
}
