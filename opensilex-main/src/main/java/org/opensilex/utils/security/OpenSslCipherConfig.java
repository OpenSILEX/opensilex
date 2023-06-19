package org.opensilex.utils.security;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

public interface OpenSslCipherConfig extends ServiceConfig {

    @ConfigDescription(
            value = "Cipher algorithm", defaultString = "-aes-256-cbc")
    String cipher();

    @ConfigDescription(
            value = "Message digest/hash function", defaultString = "sha512-256"
    )
    String messageDigest();

    @ConfigDescription(
           value = "Derivation number for pbkdf2 key derivation. Set to -1 if no password derivation was used during encryption", defaultInt = 1000000
    )
    int passwordDerivationNumber();
}
