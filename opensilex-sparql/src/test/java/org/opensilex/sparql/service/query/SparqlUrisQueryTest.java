package org.opensilex.sparql.service.query;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.server.rest.serialization.uri.UriFormater;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.B;
import org.opensilex.sparql.model.ModelInAnotherGraph;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SparqlUrisQueryTest {

    private static OpenSilexTestEnvironment openSilexTestEnv;

    private static SPARQLService sparql;
    private static Set<URI> uris;
    private static Function<SPARQLResult, SPARQLResourceModel> resultHandler;
    private static List<SparqlUrisQuery<SPARQLResourceModel>> queries;

    @BeforeClass
    public static void beforeClass() throws Exception {

        // Create a new OS env with new classes
        openSilexTestEnv = new OpenSilexTestEnvironment();
        openSilexTestEnv.addTestClasses(List.of(ModelInAnotherGraph.class));
        sparql = openSilexTestEnv.getSparql();

        // Create models of two different classes/graph
        List<B> bList = new ArrayList<>();
        List<ModelInAnotherGraph> anotherList = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            B b = new B();
            b.setDoubleVar(56d);
            b.setFloatVar(41f);
            b.setCharVar('Z');
            b.setShortVar((short) 0);
            b.setUri(URI.create("test:SparqlMultiClassQueryTest/B/" + i));
            bList.add(b);

            var otherModel = new ModelInAnotherGraph();
            otherModel.setUri(URI.create("test:SparqlMultiClassQueryTest/ModelInAnotherGraph/" + i));
            anotherList.add(otherModel);
        }
        sparql.create(B.class, bList);
        sparql.create(ModelInAnotherGraph.class, anotherList);

        // Collect URIs and pre-build query
        uris = Stream.concat(
                        bList.stream().map(SPARQLResourceModel::getUri),
                        anotherList.stream().map(SPARQLResourceModel::getUri)
                ).map(UriFormater::formatURI)
                .collect(Collectors.toSet());

        // Create queries
        var multiClassQuery = new SparqlMultiClassQuery<>(
                sparql,
                List.of(B.class, ModelInAnotherGraph.class),
                uris
        );


        var multiGraphQuery = new SparqlMultiGraphQuery<>(
                sparql,
                uris,
                Map.of(
                        sparql.getForClass(B.class).getRDFType().asNode(), sparql.getDefaultGraph(B.class),
                        sparql.getForClass(ModelInAnotherGraph.class).getRDFType().asNode(), sparql.getDefaultGraph(ModelInAnotherGraph.class)
                )
        );
        var anyGraphQuery = new SparqlMultiGraphQuery<>(
                sparql,
                uris,
                Collections.emptyMap()
        );
        queries = List.of(multiClassQuery, multiGraphQuery, anyGraphQuery);

        resultHandler = result -> {
            SPARQLResourceModel model = new SPARQLResourceModel();
            model.setUri(UriFormater.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
            model.setType(UriFormater.formatURI(result.getStringValue(SPARQLResourceModel.TYPE_FIELD)));
            return model;
        };
    }

    @Test
    public void testResultsAsStream() throws SPARQLException {

        for(var query : queries) {

            List<SPARQLResourceModel> models = query
                    .resultsAsStream(resultHandler)
                    .collect(Collectors.toList());

            Assert.assertEquals(uris.size(), models.size());
            models.forEach(model -> Assert.assertTrue(uris.contains(model.getUri())));
        }
    }

    @Test
    public void testConsumeResults() throws SPARQLException {

        for(var query : queries) {
            List<SPARQLResourceModel> models = query.getResults(resultHandler, "Should never happen here");
            Assert.assertEquals(uris.size(), models.size());
            models.forEach(model -> Assert.assertTrue(uris.contains(model.getUri())));
        }
    }

    @Test
    public void testExistingAsStream() throws SPARQLException {

        for(var query : queries) {

            Stream<URI> existingUris = query.getExistingStream();

            AtomicInteger i = new AtomicInteger(0);
            existingUris.forEach(uri -> {
                Assert.assertTrue(uris.contains(uri));
                i.getAndIncrement();
            });
            Assert.assertEquals(uris.size(), i.get());
        }

    }

    @Test
    public void testCheckUnknowns() throws SPARQLException {

        var unknowns = List.of(
                URI.create("test:unknown_1"),
                URI.create("test:unknown_2")
        );

        SparqlMultiClassQuery<SPARQLResourceModel> failQuery = new SparqlMultiClassQuery<>(
                sparql,
                List.of(B.class, ModelInAnotherGraph.class),
                unknowns
        );
        var exception = Assert.assertThrows(SPARQLInvalidUriListException.class, () -> {
            failQuery.checkUnknowns("testCheckUnknowns");
        });

        Assert.assertTrue(exception.getStrUris().contains(unknowns.get(0).toString()));
        Assert.assertTrue(exception.getStrUris().contains(unknowns.get(1).toString()));

        exception = Assert.assertThrows(SPARQLInvalidUriListException.class, () -> {
            failQuery.getResults((result) -> resultHandler.apply(result));
        });
        Assert.assertTrue(exception.getStrUris().contains(unknowns.get(0).toString()));
        Assert.assertTrue(exception.getStrUris().contains(unknowns.get(1).toString()));

    }

    @Test
    public void testGetUnknownStream() throws SPARQLException {

        for(var query : queries) {
            Stream<URI> unknowns = query.getUnknownStream();
            Assert.assertEquals(0, unknowns.count());
        }
    }

    @AfterClass
    public static void stopOpenSilex() throws Exception {
        openSilexTestEnv.stopOpenSilex();
    }

}