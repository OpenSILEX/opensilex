/*
 *  *************************************************************************************
 *  SecretReadUtilsTest.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.utils.security;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opensilex.utils.ThrowingFunction;
import org.opensilex.utils.functionnal.ThrowingSupplier;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SecretReadUtilsTest {

    static final List<String> SECRETS = Arrays.asList(
            "h,[_-N;\"orSx-&^Ev`.D7lt\"b9A+SvRTI|7JKp<.\"%G$'u^I_J4<6'\\3}maMqsh9",
            "M\\W$J7..J603s9X-|:bS'=,Zg[d8ZeeE7|T*@,HQ:pF[_mD6{;>X~tY?\\,TbK`Z+\n",
            "opensilex",
            RandomStringUtils.randomAlphabetic(128),
            RandomStringUtils.randomAlphanumeric(128),
            RandomStringUtils.randomAlphanumeric(4096),
            RandomStringUtils.randomAlphanumeric(65536),
            RandomStringUtils.randomAlphanumeric(65536 * 8),
            RandomStringUtils.randomAscii(256),
            "openslèx",
            "@pensilàx"
    );

    static final List<Charset> CHARSETS = Arrays.asList(
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16,
            StandardCharsets.UTF_16BE,
            StandardCharsets.UTF_16LE,
            StandardCharsets.ISO_8859_1
    );

    @Test
    public void readSecret() throws IOException {

        for (String inputSecret : SECRETS) {
            for (Charset charset : CHARSETS) {
                char[] outSecret = SecretReadUtils.readSecret(() -> new ByteArrayInputStream(inputSecret.getBytes(charset)), charset);
                Assert.assertArrayEquals("" + inputSecret + "," + charset, outSecret, inputSecret.toCharArray());
            }
        }
    }
    @Test()
    @Ignore("The right of creating a tmp file is not guaranteed in every test execution environnement/host")
    public void readSecretFromFile() throws IOException {

        Path tmpFile = Files.createTempFile("opensilex-tests", "readSecretFromFile.tests");

        try {
            for (String inputSecret : SECRETS) {
                for (Charset charset : CHARSETS) {
                    Files.write(tmpFile, inputSecret.getBytes(charset), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                    char[] outSecret = SecretReadUtils.readSecret(() -> Files.newInputStream(tmpFile), charset);
                    Assert.assertArrayEquals("" + inputSecret + "," + charset, outSecret, inputSecret.toCharArray());
                }
            }

        } finally {
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
                Assert.assertFalse(Files.exists(tmpFile));
            }
        }
    }
    @Test()
    @Ignore("The right of creating a tmp file is not guaranteed in every test execution environnement/host")
    public void readSecretFromFileWithProcess() throws IOException {
        Path tmpFile = Files.createTempFile("opensilex-tests", "readSecretFromFileWithProcess.tests");

        try {
            for (String inputSecret : SECRETS) {
                for (Charset charset : CHARSETS) {
                    Files.write(tmpFile, inputSecret.getBytes(charset), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                    Process process = new ProcessBuilder().command("cat", tmpFile.toString()).start();
                    char[] outSecret = SecretReadUtils.readSecret(process::getInputStream, charset);

                    Assert.assertArrayEquals("" + inputSecret + "," + charset, outSecret, inputSecret.toCharArray());
                    process.destroy();
                }
            }

        } finally {
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
                Assert.assertFalse(Files.exists(tmpFile));
            }
        }
    }

}