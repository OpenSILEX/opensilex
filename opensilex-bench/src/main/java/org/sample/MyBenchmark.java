
package org.sample;

import org.apache.jena.sparql.algebra.Op;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.*;
import org.opensilex.core.CoreModule;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.rest.RestModule;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.OpenSilex;


import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyBenchmark {

    @Benchmark
    public URI testMethod() throws Exception {

        OpenSilex.setup(new HashMap<>() {{
            put(OpenSilex.DEBUG_ARG_KEY, "false");
        }});

        OpenSilex.registerModule(RestModule.class);
        OpenSilex.registerModule(CoreModule.class);

        RDF4JInMemoryServiceFactory factory = new RDF4JInMemoryServiceFactory();
        SPARQLService sparql = factory.provide();

        ExperimentDAO dao = new ExperimentDAO(sparql);
        ExperimentModel model = new ExperimentModel();
        model.setLabel("label");
        model.setCampaign(2020);
        model.setStartDate(LocalDate.now());
        model.setComment("comment");
        model.setKeywords(List.of("opensilex","climate change","irrigation"));
        dao.create(model);
        return model.getUri();
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
            .include(MyBenchmark.class.getSimpleName())
            .forks(0)
            .measurementTime(TimeValue.milliseconds(1))
            .measurementIterations(5)
            .warmupTime(TimeValue.milliseconds(1))
            .warmupIterations(2)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.MILLISECONDS)
            .resultFormat(ResultFormatType.JSON)
            .verbosity(VerboseMode.NORMAL)
//            .output("opensilex_bench.json")
            .build();
        new Runner(opt).run();

    }

}
