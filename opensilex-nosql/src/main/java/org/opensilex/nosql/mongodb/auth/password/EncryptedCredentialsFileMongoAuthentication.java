package org.opensilex.nosql.mongodb.auth.password;

import com.mongodb.MongoCredential;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.utils.ProcessUtils;
import org.opensilex.utils.security.SecretReadUtils;
import org.opensilex.utils.security.OpenSslCipherConfig;
import org.opensilex.utils.unix.UnixFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A specialization of {@link CredentialsFileMongoAuthentication} which can read credentials from an
 * encrypted credentials file. <br>
 * This implementation read a passphrase/key in order to decrypt a credentials file encrypted with an OpenSSL cipher.
 * This key must have been used in to encrypt the initial plain credentials file
 *
 * @author rcolin
 * @see <a href="https://www.openssl.org/docs/manmaster/man1/openssl-enc.html">OpenSSL encryption</a>
 */
@ServiceDefaultDefinition(config = EncryptedCredentialsFileMongoConfig.class)
public class EncryptedCredentialsFileMongoAuthentication extends CredentialsFileMongoAuthentication {

    public EncryptedCredentialsFileMongoAuthentication(EncryptedCredentialsFileMongoConfig config) throws IOException, SecurityException {
        super(config);

        if (StringUtils.isEmpty(config.credentialEncryptionKeyPath())) {
            throw new IllegalArgumentException("Null or empty credentials decryption key");
        }

        // Ensure that credentials key file is only readable by OpenSILEX
        File credentialKeyFile = new File(config.credentialEncryptionKeyPath());
        UnixFileUtils.checkFilePermissionsAndOwnership(credentialKeyFile, "Mongo credentials key file", true, true);
    }

    @Override
    public EncryptedCredentialsFileMongoConfig getConfig() {
        return (EncryptedCredentialsFileMongoConfig) super.getConfig();
    }

    @Override
    public MongoCredential readCredentials() throws SecurityException, IOException {

        Process openSslProcess = null;
        InputStream processStdOut = null;
        try {

            OpenSslCipherConfig cipherConfig = getConfig().cipherConfig();
            if(LOGGER.isInfoEnabled()){
                LOGGER.info("[OPENSSL_DECRYPTION_PROCESS] cipher: {}, messageDigest: {}, keyDerivationNb: {} [START]",
                        cipherConfig.cipher(), cipherConfig.messageDigest(), cipherConfig.passwordDerivationNumber()
                );
            }

            // Prepare and run a process which run the OpenSSL executable
            openSslProcess = new ProcessBuilder()
                    .command(buildOpenSslArgs())
                    .start();

            ProcessUtils.checkErrorFromProcess(openSslProcess);

            // read standard output ((it contains the decrypted credentials)) of the decryption process
            processStdOut = openSslProcess.getInputStream();

            // provide an InputStream (from a file which contains the secrets) and read
            Charset charset = Charset.forName(getConfig().credentialConfigEncoding());
            char[] password = SecretReadUtils.readSecret(openSslProcess::getInputStream, charset);

            return buildCredentialsAndCleanPassword(password, charset);

        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        } finally {
            // Ensure that the Process will always be destroyed and
            if (openSslProcess != null && openSslProcess.isAlive()) {
                openSslProcess.destroy();
            }
            if (processStdOut != null) {
                processStdOut.close();
            }
        }
    }


    private List<String> buildOpenSslArgs() {

        EncryptedCredentialsFileMongoConfig config = getConfig();
        OpenSslCipherConfig sslCipherConfig = config.cipherConfig();
        Objects.requireNonNull(sslCipherConfig, "Null OpenSSL configuration provided");

        List<String> opensslDecryptArgs = new ArrayList<>();
        opensslDecryptArgs.add("openssl");
        opensslDecryptArgs.add("enc");

        // cipher
        if (StringUtils.isEmpty(sslCipherConfig.cipher())) {
            throw new IllegalArgumentException("Null or empty OpenSSL cipher");
        }
        opensslDecryptArgs.add("-d");
        opensslDecryptArgs.add(sslCipherConfig.cipher());

        // hash/message digest
        if (StringUtils.isEmpty(sslCipherConfig.messageDigest())) {
            throw new IllegalArgumentException("Null OpenSSL messageDigest");
        }
        opensslDecryptArgs.add("-md");
        opensslDecryptArgs.add(sslCipherConfig.messageDigest());

        // input passphrase/secret key
        opensslDecryptArgs.add("--pass");
        opensslDecryptArgs.add("file:" + config.credentialEncryptionKeyPath());

        // Salt (prevent dictionary-bases brute force attacks)
        opensslDecryptArgs.add("-salt");

        // password derivation with pbkdf2
        if (sslCipherConfig.passwordDerivationNumber() > 0) {
            opensslDecryptArgs.add("-pbkdf2");
            opensslDecryptArgs.add("-iter");
            opensslDecryptArgs.add(Integer.toString(sslCipherConfig.passwordDerivationNumber()));
        }

        // encrypted properties file
        opensslDecryptArgs.add("-in");
        opensslDecryptArgs.add(config.credentialConfigPath());

        return opensslDecryptArgs;
    }
}
