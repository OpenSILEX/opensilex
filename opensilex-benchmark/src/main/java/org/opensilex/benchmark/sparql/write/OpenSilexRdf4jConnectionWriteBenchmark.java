package org.opensilex.benchmark.sparql.write;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.opensilex.benchmark.core.AbstractOpenSilexBenchmark;
import org.opensilex.benchmark.core.OpenSilexBenchmarkRunner;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.Oeso;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@State(Scope.Benchmark)
public class OpenSilexRdf4jConnectionWriteBenchmark extends AbstractOpenSilexBenchmark {

    @Param("1024")
    public int length;
    public static final String LENGTH_JMH_PARAM = "length";

    private Function<DeviceModel, Stream<Statement>> statementBuilder;
    private URI graph;

    @Override
    @Setup
    public void setup() throws Exception {
        super.setup();

        ValueFactory valueFactory = SimpleValueFactory.getInstance();

        IRI brandProperty = valueFactory.createIRI(Oeso.hasBrand.getURI());
        IRI serialNumberProperty = valueFactory.createIRI(Oeso.hasSerialNumber.getURI());
        IRI modelProperty = valueFactory.createIRI(Oeso.hasModel.getURI());
        IRI startUpProperty = valueFactory.createIRI(Oeso.startUp.getURI());
        IRI removalProperty = valueFactory.createIRI(Oeso.removal.getURI());

        graph = sparql.getDefaultGraphURI(DeviceModel.class);
        statementBuilder = model -> {
            IRI uri = valueFactory.createIRI("dev:id/device/" + model.getName());

            Statement typeStmt = valueFactory.createStatement(uri, RDF.TYPE, valueFactory.createLiteral(model.getType().toString()));
            Statement name = valueFactory.createStatement(uri, RDFS.LABEL, valueFactory.createLiteral(model.getName()));
            Statement description = valueFactory.createStatement(uri, RDFS.COMMENT, valueFactory.createLiteral(model.getDescription()));

            Statement modelStmt = valueFactory.createStatement(uri, modelProperty, valueFactory.createLiteral(model.getModel()));
            Statement serialNumber = valueFactory.createStatement(uri, serialNumberProperty, valueFactory.createLiteral(model.getSerialNumber()));
            Statement brand = valueFactory.createStatement(uri, brandProperty, valueFactory.createLiteral(model.getBrand()));

            Statement startUp = valueFactory.createStatement(uri, startUpProperty, valueFactory.createLiteral(model.getStartUp()));
            Statement removal = valueFactory.createStatement(uri, removalProperty, valueFactory.createLiteral(model.getRemoval()));

            return Stream.of(typeStmt, name, description, modelStmt, serialNumber, brand, startUp, removal);
        };
    }

    public List<DeviceModel> getModels() {
        List<DeviceModel> models = new ArrayList<>(length);
        Instant now = Instant.now();
        LocalDate nowDate = LocalDate.now();
        URI type = URI.create(Oeso.SensingDevice.getURI());

        for (int i = 0; i < length; i++) {
            DeviceModel model = new DeviceModel();
            model.setUri(URI.create("benchmark:"+now+"_"+i));
            model.setType(type);
            model.setName("device_bench_name" + now.getEpochSecond() + "_" + i);
            model.setDescription("device_description" + now.getEpochSecond() + "_" + i);

            model.setModel("device_description" + now.getEpochSecond() + "_" + i);
            model.setSerialNumber("device_sn" + now.getEpochSecond() + "_" + i);
            model.setBrand("device_brand" + now.getEpochSecond() + "_" + i);

            model.setStartUp(nowDate);
            model.setRemoval(nowDate.plusDays(8611));
            models.add(model);
        }
        return models;
    }

//    @Benchmark
//    public void testSparqlQueryInsert(Blackhole bh) throws Exception {
//        sparql.create(NodeFactory.createURI(graph.toString()), getModels());
//        LOGGER.info("Insertion [OK] length: {}", length);
//    }

    @Benchmark
    public void testSparqlQueryInsertWithoutUriChecking(Blackhole bh) throws Exception {
//        sparql.create(NodeFactory.createURI(graph.toString()), getModels(),2048,false);
        LOGGER.info("Insertion [OK] length: {}", length);
    }

//    @Benchmark
//    public void testRdf4jConnectionInsert(Blackhole bh) {
//        SPARQLConnection connection = sparql.getConnection();
//        if (connection instanceof RDF4JConnection) {
//            RDF4JConnection rdf4JConnection = (RDF4JConnection) connection;
//            rdf4JConnection.insertAll(getModels().stream(), statementBuilder, graph);
//        }
//        LOGGER.info("Insertion [OK] length: {}", length);
//    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .param(LENGTH_JMH_PARAM, String.valueOf(10000))
                .param(CONFIG_PATH_JMH_PARAM, "/home/renaud/workspace/OpenSilex/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml")
                .build();

        OpenSilexBenchmarkRunner runner = new OpenSilexBenchmarkRunner(OpenSilexRdf4jConnectionWriteBenchmark.class, false, options);
        runner.run();
    }

}
