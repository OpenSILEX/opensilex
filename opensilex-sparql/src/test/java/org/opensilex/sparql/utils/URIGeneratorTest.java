/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.utils;

import java.io.UnsupportedEncodingException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opensilex.sparql.utils.URIGenerator;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vince
 */
public class URIGeneratorTest extends AbstractUnitTest  {

    @Test
    public void testNormalization() throws UnsupportedEncodingException {

        String result = URIGenerator.normalize("$");
        assertEquals("$ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("&");
        assertEquals("& Char should be removed from URI", "", result);
        result = URIGenerator.normalize("~");
        assertEquals("~ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("\"");
        assertEquals("\" Char should be removed from URI", "", result);
        result = URIGenerator.normalize("'");
        assertEquals("' Char should be removed from URI", "", result);
        result = URIGenerator.normalize("{");
        assertEquals("{ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("}");
        assertEquals("} Char should be removed from URI", "", result);
        result = URIGenerator.normalize("(");
        assertEquals("( Char should be removed from URI", "", result);
        result = URIGenerator.normalize(")");
        assertEquals(") Char should be removed from URI", "", result);
        result = URIGenerator.normalize("[");
        assertEquals("[ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("]");
        assertEquals("] Char should be removed from URI", "", result);
        result = URIGenerator.normalize("|");
        assertEquals("| Char should be removed from URI", "", result);
        result = URIGenerator.normalize("`");
        assertEquals("` Char should be removed from URI", "", result);
        result = URIGenerator.normalize("\\");
        assertEquals("\\ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("^");
        assertEquals("^ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("@");
        assertEquals("@ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("=");
        assertEquals("= Char should be removed from URI", "", result);
        result = URIGenerator.normalize("+");
        assertEquals("+ Char should be removed from URI", "", result);
        result = URIGenerator.normalize("%");
        assertEquals("% Char should be removed from URI", "", result);
        result = URIGenerator.normalize("*");
        assertEquals("* Char should be removed from URI", "", result);
        result = URIGenerator.normalize("!");
        assertEquals("! Char should be removed from URI", "", result);
        result = URIGenerator.normalize(":");
        assertEquals(": Char should be removed from URI", "", result);
        result = URIGenerator.normalize("/");
        assertEquals("/ Char should be removed from URI", "", result);
        result = URIGenerator.normalize(";");
        assertEquals("; Char should be removed from URI", "", result);
        result = URIGenerator.normalize(".");
        assertEquals(". Char should be removed from URI", "", result);
        result = URIGenerator.normalize(",");
        assertEquals(", Char should be removed from URI", "", result);
        result = URIGenerator.normalize("?");
        assertEquals("? Char should be removed from URI", "", result);

    }
}
