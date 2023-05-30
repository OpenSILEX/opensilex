package org.opensilex.core.data.utils;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataSimpleGetDTO;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.opensilex.core.data.utils.DataMathFunctions.computeAveragePerDay;
import static org.opensilex.core.data.utils.DataMathFunctions.computeMedianPerHour;

public class DataMathFunctionsTest extends AbstractMongoIntegrationTest {

    private List<DataSimpleGetDTO> dataSerieA, dataSerieB, dataSerieC;

    private static DataSimpleGetDTO createDataSimpleDTO(int id,
                                                        Object value,
                                                        Instant date) {

        DataSimpleGetDTO dto = new DataSimpleGetDTO();

        dto.setUri(URI.create("test:sample_" + id));
        dto.setValue(value);
        dto.setDate(date, "+02:00", true);

        return dto;
    }


    private static List<DataSimpleGetDTO> createDataSerieSample(int sampleSize,
                                                                float startValue,
                                                                float step,
                                                                Instant startDate) {

        List<DataSimpleGetDTO> dataSerie = new ArrayList<>();
        float value = startValue;

        for (int i = 0; i < sampleSize; ++i) {
            dataSerie.add(createDataSimpleDTO(i, value, startDate));
            startDate = startDate.plus(10, ChronoUnit.MINUTES);
            value += step;
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
                Instant.parse("1994-07-04T18:00:00.00Z"));

        // 5, 10, 15, ..., 60
        dataSerieB = createDataSerieSample(
                12,
                5,
                5,
                Instant.parse("1994-07-04T17:50:00.00Z"));

        // 36, 33, 30, ..., 3
        dataSerieC = createDataSerieSample(
                12,
                36,
                -3,
                Instant.parse("1994-07-04T18:10:00.00Z"));
    }


    @Test
    public void testComputeMedianPerHour() {
        List<DataSimpleGetDTO> medianSerieA = computeMedianPerHour(dataSerieA);

        assertNotNull(medianSerieA);
        assertEquals(2, medianSerieA.size());
        assertEquals(Instant.parse("1994-07-04T18:30:00.00Z"), medianSerieA.get(0).getDateTime());
        assertEquals(3.5f, Float.parseFloat(medianSerieA.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T19:30:00.00Z"), medianSerieA.get(1).getDateTime());
        assertEquals(9.5f, Float.parseFloat(medianSerieA.get(1).getValue().toString()));

        List<DataSimpleGetDTO> medianSerieB = computeMedianPerHour(dataSerieB);

        assertNotNull(medianSerieB);
        assertEquals(3, medianSerieB.size());
        assertEquals(Instant.parse("1994-07-04T17:30:00.00Z"), medianSerieB.get(0).getDateTime());
        assertEquals(5f, Float.parseFloat(medianSerieB.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T18:30:00.00Z"), medianSerieB.get(1).getDateTime());
        assertEquals(22.5f, Float.parseFloat(medianSerieB.get(1).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T19:30:00.00Z"), medianSerieB.get(2).getDateTime());
        assertEquals(50f, Float.parseFloat(medianSerieB.get(2).getValue().toString()));
    }

    /*
    @Test
    public void testComputeAveragePerHour() {
        List<DataSimpleGetDTO> dataSample = Stream.of(dataSerieA,dataSerieB, dataSerieC)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DataSimpleGetDTO> averageSerie = computeAveragePerDay(dataSample);

        assertNotNull(averageSerie);
        assertEquals(4, averageSerie.size());
        assertEquals(Instant.parse("1994-07-04T17:30:00.00Z"), averageSerie.get(0).getDateTime());
        assertEquals(5f, Float.parseFloat(averageSerie.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T18:30:00.00Z"), averageSerie.get(1).getDateTime());
        assertEquals(18f, Float.parseFloat(averageSerie.get(1).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T19:30:00.00Z"), averageSerie.get(2).getDateTime());
        assertEquals(22.823f, Float.parseFloat(averageSerie.get(2).getValue().toString()), 0.001f);
        assertEquals(Instant.parse("1994-07-04T20:30:00.00Z"), averageSerie.get(3).getDateTime());
        assertEquals(3f, Float.parseFloat(averageSerie.get(3).getValue().toString()));
    }
    */
}