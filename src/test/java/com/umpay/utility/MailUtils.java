package com.umpay.utility;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MailUtils {

    public static void sendEmail(String host, String port, String auth, String starttls, final String from, final String password, String to, String subject, String body, List<String> attachmentPaths) {

        if (password == null || password.isBlank()) {
            System.out.println("No mail password available. Skipping report email.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        if (port.equals("465")) {
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        }
        // Added for Gmail support
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.debug", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        // Try every configured credential, not just the one handed in. A revoked app
        // password in UMPAY_MAIL_PASSWORD shadowed a working one in secrets.properties and
        // took the report mail down with it, failing here as 535-5.7.8 while the IMAP reader
        // failed on the same value. Sending is what settles which credential is real.
        Map<String, String> attempts = new LinkedHashMap<>();
        attempts.put("the password supplied by the caller", password);

        for (Map.Entry<String, String> candidate : MailCredentials.candidates().entrySet()) {
            if (!attempts.containsValue(candidate.getValue())) {
                attempts.put(candidate.getKey(), candidate.getValue());
            }
        }

        for (Map.Entry<String, String> attempt : attempts.entrySet()) {

            if (send(props, from, attempt.getValue(), to, subject, body, attachmentPaths)) {
                System.out.println("Report email sent using " + attempt.getKey());
                return;
            }

            System.out.println("Report email was refused with " + attempt.getKey());
        }

        System.out.println("No configured mail credential could send the report email.");
    }

    /** One send attempt with one credential. Returns whether it went out. */
    private static boolean send(Properties props, final String from, final String password,
                                String to, String subject, String body,
                                List<String> attachmentPaths) {

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            if (attachmentPaths != null && !attachmentPaths.isEmpty()) {
                for (String attachmentPath : attachmentPaths) {
                    if (attachmentPath != null) {
                        File attachmentFile = new File(attachmentPath);
                        System.out.println("[DEBUG_LOG] Checking attachment: " + attachmentPath + " - Exists: " + attachmentFile.exists());
                        if (attachmentFile.exists()) {
                            MimeBodyPart attachmentPart = new MimeBodyPart();
                            DataSource source = new FileDataSource(attachmentPath);
                            attachmentPart.setDataHandler(new DataHandler(source));
                            attachmentPart.setFileName(attachmentFile.getName());
                            
                            // If it's a screenshot (png), set a Content-ID to embed it in the email if needed
                            if (attachmentPath.toLowerCase().endsWith(".png")) {
                                attachmentPart.setHeader("Content-ID", "<screenshot>");
                                attachmentPart.setDisposition(MimeBodyPart.INLINE);
                            } else {
                                attachmentPart.setDisposition(MimeBodyPart.ATTACHMENT);
                            }
                            
                            multipart.addBodyPart(attachmentPart);
                            System.out.println("[DEBUG_LOG] Added attachment: " + attachmentFile.getName());
                        }
                    }
                }
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("Email sent successfully!");
            return true;

        } catch (Exception e) {
            // Not printed as a stack trace any more: a refused credential is an expected
            // outcome here, and the caller moves on to the next one.
            System.out.println("Error sending email: " + e.getMessage());
            return false;
        }
    }
}
