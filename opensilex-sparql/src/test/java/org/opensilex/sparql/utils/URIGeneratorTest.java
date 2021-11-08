/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.unit.test.AbstractUnitTest;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author vince
 */
public class URIGeneratorTest extends AbstractUnitTest  {

    public final static String[] NORMALIZED_CHARACTERS = {
            "$",
            "&",
            "~",
            "\"",
            "'",
            "{",
            "}",
            "(",
            ")",
            "[",
            "]",
            "|",
            "`",
            "\\",
            "^",
            "@",
            "=",
            "+",
            "%",
            "*",
            "!",
            ":",
            "/",
            ";",
            ".",
            ",",
            "?",
    };

    @Test
    public void testNormalization() throws UnsupportedEncodingException {

        for(String character : NORMALIZED_CHARACTERS){
            String result = URIGenerator.normalize(character);
            assertEquals(character+" Char should be removed from URI", "", result);
        }
    }

    @Test
    public void testUriBuilding(){

        String baseUri = "http://opensilex.org";
        String baseUriWithSlash = baseUri+"/";

        String expectedUri =  "http://opensilex.org/id/param";

        URI uri = UriBuilder.fromUri(baseUri).path("id").path("param").build();
        Assert.assertEquals(uri.toString(),expectedUri);

        URI uri2 = UriBuilder.fromUri(baseUri).path("id/").path("param").build();
        Assert.assertEquals(uri2.toString(),expectedUri);

        URI uri3 = UriBuilder.fromUri(baseUriWithSlash).path("id").path("param").build();
        Assert.assertEquals(uri3.toString(),expectedUri);

        URI uri4 = UriBuilder.fromUri(baseUriWithSlash).path("id/").path("param").build();
        Assert.assertEquals(uri4.toString(),expectedUri);
    }
}
