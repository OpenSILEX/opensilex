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

import javax.mail.internet.InternetAddress;

/**
 *
 * Default Email service implementation for OpenSilex
 * <pre>
 * @see <a href="https://github.com/bbottema/simple-java-mail/issues/23">=> two factor authentification</a>
 * </pre>
 */
@ServiceDefaultDefinition(config = EmailConfig.class)
public class EmailService extends BaseService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final boolean enable;

    private final String host;
    private final String userId;
    private final int port;
    private final String password;
    public final String sender;
    public final boolean simulateSending;
    private Mailer mailer;

    public static final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    public EmailService(EmailConfig config) {
        super(config);
        host = config.smtp().host();
        userId = config.smtp().userId();
        port = config.smtp().port();
        password = config.smtp().userPassword();
        sender = config.sender();
        enable = config.enable();
        simulateSending = config.simulateSending();
    }

    @Override
    public void startup() throws OpenSilexModuleNotFoundException {
        if (enable) {
            mailer = getMailerInstance();

            LOGGER.debug("session : {}", mailer.getSession());
            LOGGER.debug("transportStrategy: {}", mailer.getTransportStrategy());
            LOGGER.debug("serverConfig: {}", mailer.getServerConfig());
            LOGGER.debug("proxyConfig: {}", mailer.getProxyConfig());
            LOGGER.debug("operationalConfig: {}", mailer.getOperationalConfig());
            mailer.testConnection();
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
        MailerRegularBuilder<?> mailerBuilder = MailerBuilder
                .withSMTPServer(host, port, userId, password)
                .withSessionTimeout(60 * 1000)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withProperty("mail.smtp.host", host)
                .withProperty("mail.smtp.user", userId)
                .withProperty("mail.smtp.password", password)
                .withProperty("mail.smtp.port", port)
                .withProperty("mail.smtp.auth", "true")
                .withProperty("mail.smtp.ssl.trust", "stmp.gmail.com")
                .withProperty("mail.smtp.connectiontimeout", "5000")
                .withProperty("mail.smtp.timeout", "5000")
                .withProperty("mail.smtp.writetimeout", "5000")
                .withProperty("mail.defaultEncoding", "UTF-8");

        switch (port) {
            case 465:
                mailerBuilder
                        .withProperty("mail.smtp.socketFactory.port", port)
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
            mailerBuilder.withTransportModeLoggingOnly(simulateSending);
            mailerBuilder.withDebugLogging(true);
        }
        return mailerBuilder.buildMailer();
    }

    /**
     * Send e-mail with e-mail service configuration
     *
     * @param to list of e-mail address
     * @param subject mail subject
     * @param templateName provide mustache template message to prepare an
     * e-mail
     * @param templateOptions A map of (String,Objet) that with replace template
     * values. e.g: Hello {{name}} with map("name","opensilex) == Hello
     * opensilex
     * @param isTextHtml is text must be treated as HTML or plain text
     */
    public void sendAnEmail(List<InternetAddress> to, String subject, String templateName, Map<String, Object> templateOptions, boolean isTextHtml) throws IOException {

        Reader targetReader = new InputStreamReader(new FileInputStream(ClassUtils.getFileFromClassArtifact(getClass(), "email/" + templateName)));
        Mustache mustache = mustacheFactory.compile(targetReader, templateName);
        StringWriter writer = new StringWriter();
        mustache.execute(writer, templateOptions).flush();
        String htmlText = writer.toString();

        EmailPopulatingBuilder tempEmail = EmailBuilder.startingBlank()
                .toMultipleAddresses(to)
                .from(sender)
                .withSubject(subject);

        if (isTextHtml) {
            tempEmail.withHTMLText(htmlText);
        } else {
            tempEmail.withPlainText(htmlText);
        }
        Email email = tempEmail.buildEmail();
        getMailerInstance().sendMail(email);
    }

    public boolean isEnable() {
        return enable;
    }
}
