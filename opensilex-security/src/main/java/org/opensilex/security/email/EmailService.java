//******************************************************************************
//                        EmailService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.security.email;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.mail.internet.InternetAddress;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.security.EmailConfig;
import org.opensilex.service.Service;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.utils.ClassUtils;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.MailerRegularBuilder;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Default Email service implementation for OpenSilex
 * <pre>
 * @see https://github.com/bbottema/simple-java-mail/issues/23 => two factor authentification
 * </pre>
 */
@ServiceDefaultDefinition(config = EmailConfig.class)
public class EmailService extends BaseService implements Service {

    Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    public static Boolean ENABLE;

    private static String HOST;
    private static String USER_ID;
    private static int PORT;
    private static String PASSWORD;
    public static String SENDER;
    public static boolean SIMULATE_SENDING;
    private Mailer mailer;

    public static MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    public EmailService(EmailConfig config) {
        super(config);
        EmailService.HOST = config.smtp().host();
        EmailService.USER_ID = config.smtp().userId();
        EmailService.PORT = config.smtp().port();
        EmailService.PASSWORD = config.smtp().userPassword();
        EmailService.SENDER = config.sender();
        EmailService.ENABLE = config.enable();
        EmailService.SIMULATE_SENDING = config.simulateSending();
    }

    @Override
    public void startup() throws OpenSilexModuleNotFoundException {
        if (EmailService.ENABLE) {
            try {
                mailer = getMailerInstance();

                if (LOGGER.isDebugEnabled()) {
                    System.out.println(mailer.getSession());
                    System.out.println(mailer.getTransportStrategy());
                    System.out.println(mailer.getServerConfig());
                    System.out.println(mailer.getProxyConfig());
                    System.out.println(mailer.getOperationalConfig());
                    System.out.println(mailer.getEmailAddressCriteria());
                    mailer.testConnection();
                    mailer = null;
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public Mailer getMailerInstance() {
        if (mailer == null) {
            mailer = getMailer();
        }
        return mailer;
    }

    /**
     * Get a mailer with a given configuration
     * @return Mailer to send mail with simplemail java api
     */
    private Mailer getMailer() {
        MailerRegularBuilder mailerBuilder = MailerBuilder
                .withSMTPServer(HOST, PORT, USER_ID, PASSWORD)
                .withSessionTimeout(60 * 1000)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withProperty("mail.smtp.host", HOST)
                .withProperty("mail.smtp.user", USER_ID)
                .withProperty("mail.smtp.password", PASSWORD)
                .withProperty("mail.smtp.port", PORT)
                .withProperty("mail.smtp.auth", "true")
                .withProperty("mail.smtp.ssl.trust", "stmp.gmail.com")
                .withProperty("mail.smtp.connectiontimeout", "5000")
                .withProperty("mail.smtp.timeout", "5000")
                .withProperty("mail.smtp.writetimeout", "5000")
                .withProperty("mail.defaultEncoding", "UTF-8");

        switch (PORT) {
            case 465:
                mailerBuilder
                        .withProperty("mail.smtp.socketFactory.port", PORT)
                        .withProperty("mail.smtp.ssl.enable", "true")
                        .withProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                break;
            case 587:
                mailerBuilder
                        .withProperty("mail.smtp.starttls.enable", "true")
                        .withProperty("mail.smtp.starttls.required", "true");
                break;
            default:
                break;
        }
        if (LOGGER.isDebugEnabled() || getOpenSilex().isDebug()) {
            mailerBuilder.withTransportModeLoggingOnly(EmailService.SIMULATE_SENDING);
            mailerBuilder.withDebugLogging(true);
        }
        return mailerBuilder.buildMailer();
    }

    /**
     * Send e-mail with e-mail service configuration
     *
     * @param to list of e-mail address
     * @param from sender e-mail address
     * @param subject mail subject
     * @param emailMessage email message
     * @param isTextHtml is text must be treated as HTML or plain text
     */
    public void sendAnEmail(List<InternetAddress> to, InternetAddress from, String subject, String emailMessage, Boolean isTextHtml) {
        EmailPopulatingBuilder tempEmail = EmailBuilder.startingBlank()
                .toMultipleAddresses(to)
                .from(from)
                .withSubject(subject);
        if (isTextHtml) {
            tempEmail.withHTMLText(emailMessage);
        } else {
            tempEmail.withPlainText(emailMessage);
        }
        Email email = tempEmail.buildEmail();
        getMailerInstance().sendMail(email);
    }

    /**
     * Send e-mail with e-mail service configuration
     *
     * @param to list of e-mail address
     * @param from sender e-mail address
     * @param subject mail subject
     * @param templateName provide mustache template message to prepare an
     * e-mail
     * @param templateOptions A map of (String,Objet) that with replace template
     * values. e.g: Hello {{name}} with map("name","opensilex) == Hello
     * opensilex
     * @param isTextHtml is text must be treated as HTML or plain text
     * @throws java.io.IOException
     */
    public void sendAnEmail(List<InternetAddress> to, InternetAddress from, String subject, String templateName, Map<String, Object> templateOptions, Boolean isTextHtml) throws IOException {
        Reader targetReader = new InputStreamReader(new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), "email/" + templateName)));
        Mustache mustache = mustacheFactory.compile(targetReader, templateName);
        StringWriter writer = new StringWriter();
        mustache.execute(writer, templateOptions).flush();
        String htmlText = writer.toString();
        EmailPopulatingBuilder tempEmail = EmailBuilder.startingBlank()
                .toMultipleAddresses(to)
                .from(from)
                .withSubject(subject);
        if (isTextHtml) {
            tempEmail.withHTMLText(htmlText);
        } else {
            tempEmail.withPlainText(htmlText);
        }
        Email email = tempEmail.buildEmail();
        getMailerInstance().sendMail(email);
    }
}
