package org.opensilex.core.data.utils;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.data.api.DataComputedGetDTO;

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

    private List<DataComputedGetDTO> dataSerieA, dataSerieB, dataSerieC;

    private static DataComputedGetDTO createDataSimpleDTO(int id,
                                                          Object value,
                                                          Instant date) {

        DataComputedGetDTO dto = new DataComputedGetDTO();

        dto.setValue(value);
        dto.setDateTime(date);

        return dto;
    }


    private static List<DataComputedGetDTO> createDataSerieSample(int sampleSize,
                                                                  float startValue,
                                                                  float step,
                                                                  Instant startDate) {

        List<DataComputedGetDTO> dataSerie = new ArrayList<>();
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
        List<DataComputedGetDTO> medianSerieA = computeMedianPerHour(dataSerieA);

        assertNotNull(medianSerieA);
        assertEquals(2, medianSerieA.size());
        assertEquals(Instant.parse("1994-07-04T23:30:00.00Z"), medianSerieA.get(0).getDateTime());
        assertEquals(3.5f, Float.parseFloat(medianSerieA.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-05T00:30:00.00Z"), medianSerieA.get(1).getDateTime());
        assertEquals(9.5f, Float.parseFloat(medianSerieA.get(1).getValue().toString()));

        List<DataComputedGetDTO> medianSerieB = computeMedianPerHour(dataSerieB);

        assertNotNull(medianSerieB);
        assertEquals(3, medianSerieB.size());
        assertEquals(Instant.parse("1994-07-04T22:30:00.00Z"), medianSerieB.get(0).getDateTime());
        assertEquals(5f, Float.parseFloat(medianSerieB.get(0).getValue().toString()));
        assertEquals(Instant.parse("1994-07-04T23:30:00.00Z"), medianSerieB.get(1).getDateTime());
        assertEquals(22.5f, Float.parseFloat(medianSerieB.get(1).getValue().toString()));
        assertEquals(Instant.parse("1994-07-05T00:30:00.00Z"), medianSerieB.get(2).getDateTime());
        assertEquals(50f, Float.parseFloat(medianSerieB.get(2).getValue().toString()));
    }


    @Test
    public void testComputeAveragePerDay() {
        List<DataComputedGetDTO> dataSample = Stream.of(dataSerieA, dataSerieB, dataSerieC)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DataComputedGetDTO> averageSerie = computeAveragePerDay(dataSample);
        BigDecimal value;

        assertNotNull(averageSerie);
        assertEquals(2, averageSerie.size());
        assertEquals(Instant.parse("1994-07-04T12:00:00.00Z"), averageSerie.get(0).getDateTime());
        value = new BigDecimal(Float.parseFloat(averageSerie.get(0).getValue().toString()))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(17.28f, value.floatValue());
        assertEquals(Instant.parse("1994-07-05T12:00:00.00Z"), averageSerie.get(1).getDateTime());
        value = new BigDecimal(Float.parseFloat(averageSerie.get(1).getValue().toString()))
                .setScale(2, RoundingMode.HALF_UP);;
        assertEquals(21.72f, value.floatValue());
    }
}