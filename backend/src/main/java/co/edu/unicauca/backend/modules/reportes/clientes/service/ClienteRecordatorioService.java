package co.edu.unicauca.backend.modules.reportes.clientes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio dedicado al envio de recordatorios a clientes.
 */
@Service
public class ClienteRecordatorioService {

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public ClienteRecordatorioService(JavaMailSender mailSender,
                                      @Value("${spring.mail.from}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    public boolean enviarRecordatorio(String emailCliente, String nombreCliente, String mensaje) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailFrom);
            mail.setTo(emailCliente);
            mail.setSubject("Recordatorio de visita - Al Toro Gastrobar");
            mail.setText("Hola " + nombreCliente + ",\n\n" + mensaje + "\n\nTe esperamos pronto.\nAl Toro Gastrobar");
            mailSender.send(mail);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
