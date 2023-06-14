package org.opensilex.core.data.utils;

import org.opensilex.core.data.api.DataComputedGetDTO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class DataMathFunctions {

    /**
     * Compute the median per hour for a given data set.
     *
     * @param dataSerie the data set
     * @return the serie of median per hour
     */
    public static List<DataComputedGetDTO> computeMedianPerHour(List<DataComputedGetDTO> dataSerie) {
        List<DataComputedGetDTO> mediansPerHour = new ArrayList<>();

        Map<Long, List<DataComputedGetDTO>> dataPerHourMap = dataSerie.stream()
                .sorted(Comparator.comparing(DataComputedGetDTO::getDateTime))
                .collect(Collectors.groupingBy(d->(d.getDateTime().getEpochSecond()/3600),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<Long, List<DataComputedGetDTO>> entry : dataPerHourMap.entrySet()) {
            Instant dateTime = Instant.ofEpochSecond(entry.getKey()*3600).plus(30, ChronoUnit.MINUTES);

            int size = entry.getValue().size();
            // Stream of sorted data values
            DoubleStream sortedValues = entry.getValue().stream()
                    .mapToDouble(d -> ((Number) d.getValue()).doubleValue())
                    .sorted();
            // Depending on the odd/even size, either a stream of the only or both middle values
            DoubleStream middleValues = (size % 2 == 0)
                    ? sortedValues.skip(size / 2 - 1).limit(2)
                    : sortedValues.skip(size / 2).limit(1);
            // Average of the middle value(s) gives the median
            double median = middleValues.average()
                    .orElse(Double.NaN);

            DataComputedGetDTO medianData = new DataComputedGetDTO();
            medianData.setValue(median);
            medianData.setDateTime(dateTime);

            mediansPerHour.add(medianData);
        }

        return mediansPerHour;
    }

    /**
     * Compute the mean per day for a given data set.
     *
     * @param dataSerie the data set
     * @return a
     */
    public static List<DataComputedGetDTO> computeAveragePerDay(List<DataComputedGetDTO> dataSerie) {
        List<DataComputedGetDTO> averagePerHour = new ArrayList<>();

        Map<Long, List<DataComputedGetDTO>> dataPerHourMap = dataSerie.stream()
                .sorted(Comparator.comparing(DataComputedGetDTO::getDateTime))
                .collect(Collectors.groupingBy(d->(d.getDateTime().getEpochSecond()/(3600 * 24)),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<Long, List<DataComputedGetDTO>> entry : dataPerHourMap.entrySet()) {
            Instant dateTime = Instant.ofEpochSecond(entry.getKey()*3600*24).plus(12, ChronoUnit.HOURS);
            double avg = entry.getValue().stream()
                    .mapToDouble(d -> ((Number) d.getValue()).doubleValue())
                    .average()
                    .orElse(Double.NaN);

            DataComputedGetDTO averageData = new DataComputedGetDTO();
            averageData.setValue(avg);
            averageData.setDateTime(dateTime);

            averagePerHour.add(averageData);
        }

        return averagePerHour;
    }

}
