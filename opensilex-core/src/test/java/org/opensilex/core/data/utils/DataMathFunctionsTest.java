package org.opensilex.core.data.utils;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.data.dal.DataComputedModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.*;
import static org.opensilex.core.data.utils.DataMathFunctions.computeAveragePerDay;
import static org.opensilex.core.data.utils.DataMathFunctions.computeMedianPerHour;

public class DataMathFunctionsTest {

    private List<DataComputedModel> dataSerieA, dataSerieB, dataSerieC;

    private static DataComputedModel createDataComputedModel(int id,
                                                             Object value,
                                                             Instant date) {

        DataComputedModel dto = new DataComputedModel();

        dto.setValue(value);
        dto.setDate(date);

        return dto;
    }


    private static List<DataComputedModel> createDataSerieSample(int sampleSize,
                                                                 float startValue,
                                                                 float step,
                                                                 Instant startDate) {

        List<DataComputedModel> dataSerie = new ArrayList<>();
        float value = startValue;

        for (int i = 0; i < sampleSize; ++i) {
            dataSerie.add(createDataComputedModel(i, startValue + step * i, startDate.plus(10L * i, ChronoUnit.MINUTES)));
        }


        return dataSerie;
    }

    @Before
    public void beforeTest() throws Exception {
        // 1, 2, 3, ..., 12
        dataSerieA = createDataSerieSample(
                12,
                1,
                1,
                Instant.parse("1994-07-04T23:00:00.00Z"));

        // 5, 10, 15, ..., 60
        dataSerieB = createDataSerieSample(
                12,
                5,
                5,
                Instant.parse("1994-07-04T22:50:00.00Z"));

        // 36, 33, 30, ..., 3
        dataSerieC = createDataSerieSample(
                12,
                36,
                -3,
                Instant.parse("1994-07-04T23:10:00.00Z"));
    }

    @Test
    public void testComputeMedianPerHour() {
        List<DataComputedModel> medianSerieA = computeMedianPerHour(dataSerieA);

        assertNotNull(medianSerieA);
        assertEquals(2, medianSerieA.size());
        assertEquals(Instant.parse("1994-07-04T23:30:00.00Z"), medianSerieA.get(0).getDate());
        assertEquals(3.5f, Float.parseFloat(medianSerieA.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-05T00:30:00.00Z"), medianSerieA.get(1).getDate());
        assertEquals(9.5f, Float.parseFloat(medianSerieA.get(1).getValue().toString()));

        List<DataComputedModel> medianSerieB = computeMedianPerHour(dataSerieB);

        assertNotNull(medianSerieB);
        assertEquals(3, medianSerieB.size());
        assertEquals(Instant.parse("1994-07-04T22:30:00.00Z"), medianSerieB.get(0).getDate());
        assertEquals(5f, Float.parseFloat(medianSerieB.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T23:30:00.00Z"), medianSerieB.get(1).getDate());
        assertEquals(22.5f, Float.parseFloat(medianSerieB.get(1).getValue().toString()));
        assertEquals(Instant.parse("1994-07-05T00:30:00.00Z"), medianSerieB.get(2).getDate());
        assertEquals(50f, Float.parseFloat(medianSerieB.get(2).getValue().toString()));
    }


    @Test
    public void testComputeAveragePerDay() {
        List<DataComputedModel> dataSample = Stream.of(dataSerieA, dataSerieB, dataSerieC)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DataComputedModel> averageSerie = computeAveragePerDay(dataSample);
        BigDecimal value;

        assertNotNull(averageSerie);
        assertEquals(2, averageSerie.size());
        assertEquals(Instant.parse("1994-07-04T12:00:00.00Z"), averageSerie.get(0).getDate());
        assertTrue(Math.abs(((Number) averageSerie.get(0).getValue()).doubleValue() - 17.28d) < 0.01);
        assertEquals(Instant.parse("1994-07-05T12:00:00.00Z"), averageSerie.get(1).getDate());
        assertTrue(Math.abs(((Number) averageSerie.get(1).getValue()).doubleValue() - 21.72d) < 0.01);
    }
}