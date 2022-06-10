package org.opensilex.benchmark.core;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OpenSilexBenchmarkRunner {

    private final Options options;

    public OpenSilexBenchmarkRunner(Class<?> benchmarkClass, boolean debug, Options newOptions) {

        Objects.requireNonNull(benchmarkClass);

        OptionsBuilder optionsBuilder = new OptionsBuilder();

        optionsBuilder.timeout(TimeValue.minutes(30))
                .timeUnit(TimeUnit.MILLISECONDS)
                .mode(Mode.AverageTime)
                .verbosity(VerboseMode.NORMAL)
                .warmupIterations(2)
                .warmupTime(TimeValue.milliseconds(10))
                .measurementIterations(5)
                .measurementTime(TimeValue.milliseconds(10))
                .forks(debug ? 0 : 1)
                .resultFormat(ResultFormatType.JSON);

        if (newOptions != null) {
            optionsBuilder.parent(newOptions);
        }

        this.options = optionsBuilder.include(benchmarkClass.getName())  // include benchmark class
                .build();
    }

    public void run() throws RunnerException {
        new Runner(options).run();
    }
}
