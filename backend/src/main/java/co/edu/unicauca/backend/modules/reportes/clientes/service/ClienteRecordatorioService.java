package co.edu.unicauca.backend.modules.reportes.clientes.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio dedicado al envio de recordatorios a clientes.
 */
@Service
public class ClienteRecordatorioService {

    private final JavaMailSender mailSender;

    public ClienteRecordatorioService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarRecordatorio(String emailCliente, String nombreCliente, String mensaje) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(emailCliente);
        mail.setSubject("Recordatorio de visita - Al Toro Gastrobar");
        mail.setText("Hola " + nombreCliente + ",\n\n" + mensaje + "\n\nTe esperamos pronto.\nAl Toro Gastrobar");
        mailSender.send(mail);
    }
}
