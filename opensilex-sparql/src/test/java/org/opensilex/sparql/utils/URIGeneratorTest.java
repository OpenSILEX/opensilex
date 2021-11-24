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
import org.opensilex.unit.test.AbstractUnitTest;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author vince
 */
public class URIGeneratorTest extends AbstractUnitTest  {

    public final static String[] EXPECTED_REMOVED_CHARACTERS = {
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
            "\n",
            "\r",
            "\t",
            "\f"
    };

    @Test
    public void testNormalization() throws UnsupportedEncodingException {

        URIGenerator<?> uriGenerator = new DefaultURIGenerator<>();
        for(String character : EXPECTED_REMOVED_CHARACTERS){
            String result = uriGenerator.normalize(character);
            assertEquals(character+" Char should be removed from URI", "", result);
        }

        assertEquals("-",uriGenerator.normalize("=-="));
        assertEquals("a-b",uriGenerator.normalize("a+-b*"));
        assertEquals("_a-b_",uriGenerator.normalize(" a+-b* "));
        assertEquals("_",uriGenerator.normalize(" "));
        assertEquals("__",uriGenerator.normalize("  "));
        assertEquals("_",uriGenerator.normalize(" \n"));
        assertEquals("__",uriGenerator.normalize(" \n "));
        assertEquals("_",uriGenerator.normalize(" \r"));
        assertEquals("__",uriGenerator.normalize(" \r "));
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
