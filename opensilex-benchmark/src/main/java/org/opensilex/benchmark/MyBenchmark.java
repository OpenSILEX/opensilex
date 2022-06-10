package org.opensilex.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.opensilex.benchmark.core.AbstractOpenSilexBenchmark;
import org.opensilex.benchmark.core.OpenSilexBenchmarkRunner;

@State(Scope.Benchmark)
public class MyBenchmark extends AbstractOpenSilexBenchmark {

    @Param("default_opensilex")
    String name;
    public static final String NAME_PARAM = "name";

    @Benchmark
    public void testMethod(Blackhole bh) {
        System.out.println("Hello world : " + name);
    }

    public static void main(String[] args) throws RunnerException {

        Options options = new OptionsBuilder()
                .param(NAME_PARAM, "opensilex")
                .param(CONFIG_PATH_JMH_PARAM,"/home/renaud/workspace/OpenSilex/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml")
                .build();

        OpenSilexBenchmarkRunner runner = new OpenSilexBenchmarkRunner(MyBenchmark.class, true, options);
        runner.run();
    }

}
