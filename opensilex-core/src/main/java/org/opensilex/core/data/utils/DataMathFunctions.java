package org.opensilex.core.data.utils;

import org.opensilex.core.data.api.DataComputedGetDTO;
import org.opensilex.core.data.api.DataSimpleGetDTO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.core.data.utils.LineSimplification.*;

public class DataMathFunctions {

    /**
     * Compute the median per hour for a given data set.
     *
     * @param dataSerie the data set
     * @return the serie of median per hour
     */
    /*
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

            List<Double> data = entry.getValue().stream().map(d->(Double.valueOf(d.getValue().toString()))).collect(Collectors.toList());
            Collections.sort(data);

            double median;
            if (data.size() % 2 == 0)
                median = (data.get(data.size()/2) + data.get(data.size()/2 - 1)) / 2;
            else
                median = data.get(data.size()/2);

            DataComputedGetDTO medianData = new DataComputedGetDTO(entry.getValue().get(0));
            medianData.setValue(median);
            medianData.setDateTime(dateTime);

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
    public static List<DataSimpleGetDTO> computeAveragePerDay(List<DataSimpleGetDTO> dataSerie) {
        List<DataSimpleGetDTO> averagePerHour = new ArrayList<>();

        Map<Long, List<DataSimpleGetDTO>> dataPerHourMap = dataSerie.stream()
                .sorted(Comparator.comparing(DataSimpleGetDTO::getDateTime))
                .collect(Collectors.groupingBy(d->(d.getDateTime().getEpochSecond()/(3600 * 24)),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<Long, List<DataSimpleGetDTO>> entry : dataPerHourMap.entrySet()) {
            Instant dateTime = Instant.ofEpochSecond(entry.getKey()*3600*24).plus(12, ChronoUnit.HOURS);
            double avg = entry.getValue().stream().mapToDouble(d->(Double.valueOf(d.getValue().toString()))).average().orElse(Double.NaN);

            DataSimpleGetDTO averageData = new DataSimpleGetDTO(entry.getValue().get(0));
            averageData.setValue(avg);
            averageData.updateDate(dateTime);

            averagePerHour.add(averageData);
        }

        return averagePerHour;
    }

    public static List<DataSimpleGetDTO> applyRamerDouglasPeucker(List<DataSimpleGetDTO> dataSerie, double epsilon) {

        List<DataSimpleGetDTO> resultData = new ArrayList();
        ramerDouglasPeucker(dataSerie, epsilon, resultData);

        return resultData;
    }

    public static List<DataSimpleGetDTO> applyGaussianSmooth(List<DataSimpleGetDTO> dataSerie, int windowLength) {

        List<DataSimpleGetDTO> resultData = new ArrayList();

        int offset = (int) Math.floor(windowLength/2);

        for (int i = offset; i < (dataSerie.size() - offset); ++i) {
            List<DataSimpleGetDTO> windowList = dataSerie.subList(i - offset, i + offset);

            double average = windowList.stream()
                    .mapToDouble(d -> (Double.valueOf(d.getValue().toString())))
                    .average()
                    .orElse(Double.NaN);

            DataSimpleGetDTO newData = new DataSimpleGetDTO(dataSerie.get(i));
            newData.setValue(average);
            resultData.add(newData);
        }

        return resultData;
    }
}
