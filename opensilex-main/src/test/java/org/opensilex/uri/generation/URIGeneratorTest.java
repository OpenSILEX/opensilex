/*******************************************************************************
 *                         URIGeneratorTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 24/11/2021 17:18
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.uri.generation;

import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 *
 * @author vince
 */
public class URIGeneratorTest {

    public static final  String[] EXPECTED_REMOVED_CHARACTERS = {
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
            Assert.assertEquals(character+" Char should be removed from URI", "", result);
        }

        Assert.assertEquals("-",uriGenerator.normalize("=-="));
        Assert.assertEquals("a-b",uriGenerator.normalize("a+-b*"));
        Assert.assertEquals("_a-b_",uriGenerator.normalize(" a+-b* "));
        Assert.assertEquals("_",uriGenerator.normalize(" "));
        Assert.assertEquals("__",uriGenerator.normalize("  "));
        Assert.assertEquals("_",uriGenerator.normalize(" \n"));
        Assert.assertEquals("__",uriGenerator.normalize(" \n "));
        Assert.assertEquals("_",uriGenerator.normalize(" \r"));
        Assert.assertEquals("__",uriGenerator.normalize(" \r "));
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
