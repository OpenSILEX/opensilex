package org.opensilex.core.data.utils;

import org.opensilex.core.data.api.DataSimpleGetDTO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DataMathFunctions {

    /**
     * Compute the median per hour for a given data set.
     *
     * @param dataSerie the data set
     * @return the serie of median per hour
     */
    public static List<DataSimpleGetDTO> computeMedianPerHour(List<DataSimpleGetDTO> dataSerie) {
        List<DataSimpleGetDTO> mediansPerHour = new ArrayList<>();

        Map<Long, List<DataSimpleGetDTO>> dataPerHourMap = dataSerie.stream()
                .sorted(Comparator.comparing(DataSimpleGetDTO::getDateTime))
                .collect(Collectors.groupingBy(d->(d.getDateTime().getEpochSecond()/3600),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<Long, List<DataSimpleGetDTO>> entry : dataPerHourMap.entrySet()) {
            Instant dateTime = Instant.ofEpochSecond(entry.getKey()*3600).plus(30, ChronoUnit.MINUTES);

            List<Double> data = entry.getValue().stream().map(d->(Double.valueOf(d.getValue().toString()))).collect(Collectors.toList());
            Collections.sort(data);

            double median;
            if (data.size() % 2 == 0)
                median = (data.get(data.size()/2) + data.get(data.size()/2 - 1)) / 2;
            else
                median = data.get(data.size()/2);

            DataSimpleGetDTO medianData = new DataSimpleGetDTO(entry.getValue().get(0));
            medianData.setValue(median);
            medianData.updateDate(dateTime);

            mediansPerHour.add(medianData);
        }

        return mediansPerHour;
    }

    /**
     * Compute the average per hour for a given data set.
     *
     * @param dataSerie the data set
     * @return a
     */
    public static List<DataSimpleGetDTO> computeAveragePerHour(List<DataSimpleGetDTO> dataSerie) {
        List<DataSimpleGetDTO> averagePerHour = new ArrayList<>();

        Map<Long, List<DataSimpleGetDTO>> dataPerHourMap = dataSerie.stream()
                .sorted(Comparator.comparing(DataSimpleGetDTO::getDateTime))
                .collect(Collectors.groupingBy(d->(d.getDateTime().getEpochSecond()/3600),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<Long, List<DataSimpleGetDTO>> entry : dataPerHourMap.entrySet()) {
            Instant dateTime = Instant.ofEpochSecond(entry.getKey()*3600).plus(30, ChronoUnit.MINUTES);
            double avg = entry.getValue().stream().mapToDouble(d->(Double.valueOf(d.getValue().toString()))).average().orElse(Double.NaN);

            DataSimpleGetDTO averageData = new DataSimpleGetDTO(entry.getValue().get(0));
            averageData.setValue(avg);
            averageData.updateDate(dateTime);

            averagePerHour.add(averageData);
        }

        return averagePerHour;
    }
}
