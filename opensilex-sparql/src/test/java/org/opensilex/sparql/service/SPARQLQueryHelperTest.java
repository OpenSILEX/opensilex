//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.sparql.service;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_GreaterThanOrEqual;
import org.apache.jena.sparql.expr.E_LessThanOrEqual;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.Expr;
import org.junit.Test;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import javax.mail.internet.InternetAddress;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.Assert.*;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 * @author Renaud COLIN
 */
public class SPARQLQueryHelperTest extends AbstractUnitTest {

    @Test
    public void testDateRange() throws Exception {

        String startDateVar = "start";
        String endDateVar = "end";
        String startDateStr = "2020-01-06";
        String endDateStr = "2020-02-05";

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // test with null start and en<d date
        Expr dateRange = SPARQLQueryHelper.dateRange(startDateVar, null, endDateVar, null);
        assertNull(dateRange);

        // test with null end date
        dateRange = SPARQLQueryHelper.dateRange(startDateVar, startDate, endDateVar, null);
        assertNotNull(dateRange);
        assertTrue(dateRange instanceof E_GreaterThanOrEqual);
        assertEquals(dateRange.toString(), "(>= ?start \"2020-01-06\"^^<http://www.w3.org/2001/XMLSchema#date>)");

        // test with null start date
        dateRange = SPARQLQueryHelper.dateRange(startDateVar, null, endDateVar, endDate);
        assertNotNull(dateRange);
        assertTrue(dateRange instanceof E_LessThanOrEqual);
        assertEquals(dateRange.toString(), "(<= ?end \"2020-02-05\"^^<http://www.w3.org/2001/XMLSchema#date>)");

        // test with two non null date
        dateRange = SPARQLQueryHelper.dateRange(startDateVar, startDate, endDateVar, endDate);
        assertNotNull(dateRange);
        assertTrue(dateRange instanceof E_LogicalAnd);
        assertEquals(dateRange.toString(), "(&& (>= ?start \"2020-01-06\"^^<http://www.w3.org/2001/XMLSchema#date>) (<= ?end \"2020-02-05\"^^<http://www.w3.org/2001/XMLSchema#date>))");

        E_LogicalAnd and = (E_LogicalAnd) dateRange;

        Expr startDatePart = and.getArg1();
        Expr endDatePart = and.getArg2();
        assertTrue(startDatePart instanceof E_GreaterThanOrEqual);
        assertTrue(endDatePart instanceof E_LessThanOrEqual);
    }

    @Test
    public void testWhereValuesFailWithEmptyParams() throws Exception {

        SelectBuilder select = new SelectBuilder();
        SPARQLQueryHelper.addWhereValues(select, "", Collections.emptyList());

        Map<Var, List<Node>> whereValuesMap = select.getWhereValuesMap();
        assertTrue(whereValuesMap.isEmpty());

        select = new SelectBuilder();
        SPARQLQueryHelper.addWhereValues(select, "varName", Collections.emptyList());

        whereValuesMap = select.getWhereValuesMap();
        assertTrue(whereValuesMap.isEmpty());
    }

    protected void testWhereValues(SelectBuilder select, Node var, List<?> objs) throws Exception {

        SPARQLQueryHelper.addWhereValues(select, var.getName(), objs);
        Map<Var, List<Node>> whereValuesMap = select.getWhereValuesMap();

        // test that the VALUES claude has been created
        assertTrue(whereValuesMap.containsKey(var));
        List<Node> nodeList = whereValuesMap.get(var);

        // test that each Object from objs is present in nodeList
        for (Object obj : objs) {
            Node node = SPARQLDeserializers.getForClass(obj.getClass()).getNodeFromString(obj.toString());
            assertTrue(nodeList.contains(node));
        }
    }

    @Test
    public void testWhereValueWithMultiplesVariables() throws Exception {

        SelectBuilder select = new SelectBuilder();

        Map<String, List<?>> valuesMap = new HashMap<>();
        valuesMap.put("var", Arrays.asList("v1", "v2"));
        valuesMap.put("var2", Collections.singletonList("v3"));

        SPARQLQueryHelper.addWhereValues(select, valuesMap);
        select.buildString();
    }

    @Test
    public void testWhereValuesWithURI() throws Exception {

        List<URI> uris = Arrays.asList(
                new URI("http://opensilex.org/set/experiments/xp"),
                new URI("http://opensilex.org/set/experiments/xp1"),
                new URI("rdf:type")
        );
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("experiment"), uris);
    }

    @Test
    public void testWhereValuesWithStr() throws Exception {
        List<String> strings = Arrays.asList("Agrophen", "Diaphen", "Phenoarch");
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("name"), strings);
    }

    @Test
    public void testWhereValuesWitBooleanDataTypes() throws Exception {
        List<Boolean> booleans = Arrays.asList(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasBoolean"), booleans);
    }

    @Test
    public void testWhereValuesWithNumberDataTypes() throws Exception {

        List<Integer> integers = Arrays.asList(58, 147, 63);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasInt"), integers);

        List<Long> longs = Arrays.asList(584888648L, 1474864884L, 63165333458648L);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasLong"), longs);

        List<Short> shorts = Arrays.asList((short) 5, (short) 47, (short) 32);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasShort"), shorts);

        List<Double> doubles = Arrays.asList(1234.5, 3.14, 890.6, 448614.9744, 8e6);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasDouble"), doubles);

        List<Float> floats = Arrays.asList(1234.5f, 3.14f, 890.6e14f, 448614.9744f, 8e16f);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasFloat"), floats);

        List<BigInteger> bigIntegers = Arrays.asList(
                new BigInteger("49888881998416616565888888888448748751114797"),
                new BigInteger("2434718947848647864635429020081766404878459000"),
                new BigInteger("2434718947848647864635429020081766404878459000").multiply(new BigInteger("78948647446"))
        );
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasBigInt"), bigIntegers);

    }

    @Test
    public void testWhereValuesWithEmail() throws Exception {

        List<InternetAddress> addresses = Arrays.asList(
                InternetAddress.parse("abc@abc.com, admin@opensilex.org, opensilex@inrae.fr")
        );
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasMail"), addresses);
    }

    @Test
    public void testWhereValuesWithByte() throws Exception {

        List<Byte> byteList = Arrays.asList((byte) 24, (byte) 96, (byte) -124, (byte) 127, (byte) -128, (byte) 5);
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasByte"), byteList);

        String str = "some str which will be converted to byte[] 4 44978df";
        List<Byte> byteListFromStr = new ArrayList<Byte>(str.getBytes().length);
        for (byte b : str.getBytes()) {
            byteListFromStr.add(b);
        }
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasByte"), byteListFromStr);
    }

    @Test
    public void testWhereValuesWithChar() throws Exception {

        String str = "some str which will be converted to byte[] 4 44978df";
        List<Character> chars = new ArrayList<>(str.length());
        for (int i = 0; i < str.length(); i++) {
            chars.add(str.charAt(i));
        }
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasChar"), chars);
    }

    @Test
    public void testWhereValuesWithDateTypes() throws Exception {

        List<LocalDate> dates = Arrays.asList(
                LocalDate.now(),
                LocalDate.now().plusDays(50),
                LocalDate.now().minusMonths(4)
        );
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasDate"), dates);

        List<OffsetDateTime> dataTimes = Arrays.asList(
                OffsetDateTime.parse("2011-12-03T10:15:30+01:00"),
                OffsetDateTime.now(),
                OffsetDateTime.of(2020, 4, 28, 14, 32, 58, 0, ZoneOffset.ofHours(1))
        );
        testWhereValues(new SelectBuilder(), NodeFactory.createVariable("hasDateTime"), dataTimes);
    }
}
