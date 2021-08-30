// https://stackabuse.com/guide-to-k-means-clustering-with-java/

package org.opensilex.core.data.clustering;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.List;
import org.opensilex.core.data.api.DataGetDTOCluster;

public class DataSet {

    static class Record{
        public URI uri;
        public HashMap<String, Double> record;
        public Integer clusterNo = 0;

        public Record(HashMap<String, Double> record){
            this.record = record;
        }

        public void setClusterNo(Integer clusterNo) {
            this.clusterNo = clusterNo;
        }

        public HashMap<String, Double> getRecord() {
            return record;
        }
        
        public void setURI(URI uri) {
            this.uri = uri;
        }
        
        public void setURI(String uri) throws URISyntaxException {
            this.uri = new URI(uri);
        }
        
        public URI getURI() {
            return uri;
        }
    }

    private final LinkedList<Record> records = new LinkedList<>();
    private final LinkedList<Integer> indicesOfCentroids = new LinkedList<>();
    private final HashMap<String, Double> minimums = new HashMap<>();
    private final HashMap<String, Double> maximums = new HashMap<>();
    private static final Random random = new Random();

    public DataSet(List<ClusterDataSet> dataList) throws IOException {
        for (ClusterDataSet data : dataList) {
            HashMap<String, Double> record = new HashMap<>();

            String name = "Value";

            record.put(name, data.value);
            updateMin(name, data.value);
            updateMax(name, data.value);

            record.put("SecondValue", data.secondValue);
            updateMin("SecondValue", data.secondValue);
            updateMax("SecondValue", data.secondValue);
            
            Record recordClass = new Record(record);
            recordClass.setURI(data.uri);
            
            records.add(recordClass);
        }
    }
    
    public List<DataGetDTOCluster> getResults(List<DataGetDTOCluster> clusterList) {
        for (Record record : records) {
            for (DataGetDTOCluster data : clusterList) {
                if (record.getURI() == data.getUri()) {
                    data.setClusterID(record.clusterNo);
                }
            }
        }
        return clusterList;
    }
    
    public void showResults() {
        for (Record record : records) {
            System.out.println("Cluster n°" + String.valueOf(record.clusterNo));
            System.out.println(record.getURI().toString() + ": " + String.valueOf(record.getRecord().get("Value")));
        }
    }

    private void updateMin(String name, Double val){
        if (minimums.containsKey(name)){
            if (val < minimums.get(name)) {
                minimums.put(name, val);
            }
        } else {
            minimums.put(name, val);
        }
    }

    private void updateMax(String name, Double val){
        if (maximums.containsKey(name)) {
            if (val > maximums.get(name)) {
                maximums.put(name, val);
            }
        } else {
            maximums.put(name, val);
        }
    }

    public Double meanOfAttr(String attrName, LinkedList<Integer> indices){
        Double sum = 0.0;
        for (int i : indices) {
            if (i < records.size()) {
                sum += records.get(i).getRecord().get(attrName);
            }
        }
        return sum / indices.size();
    }

    public HashMap<String, Double> calculateCentroid(int clusterNo){
        HashMap<String, Double> centroid = new HashMap<>();

        LinkedList<Integer> recsInCluster = new LinkedList<>();
        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            if (record.clusterNo == clusterNo) {
                recsInCluster.add(i);
            }
        }

        centroid.put("Value", meanOfAttr("Value", recsInCluster));
        centroid.put("SecondValue", meanOfAttr("SecondValue", recsInCluster));
        return centroid;
    }

    public LinkedList<HashMap<String,Double>> recomputeCentroids(int K){
        LinkedList<HashMap<String,Double>> centroids = new LinkedList<>();
        for (int i = 0; i < K; i++) {
            centroids.add(calculateCentroid(i));
        }
        return centroids;
    }

    public HashMap<String, Double> randomDataPoint(){
        HashMap<String, Double> res = new HashMap<>();

        Double min = minimums.get("Value");
        Double max = maximums.get("Value");
        res.put("Value", min + (max-min) * random.nextDouble());

        min = minimums.get("SecondValue");
        max = maximums.get("SecondValue");
        res.put("SecondValue", min + (max-min) * random.nextDouble());

        return res;
    }

    public HashMap<String, Double> randomFromDataSet(){
        int index = random.nextInt(records.size());
        return records.get(index).getRecord();
    }

    public static Double euclideanDistance(HashMap<String, Double> a, HashMap<String, Double> b){
        if (!a.keySet().equals(b.keySet())) {
            return Double.POSITIVE_INFINITY;
        }

        double sum = 0.0;

        for (String attrName : a.keySet()) {
            sum += Math.pow(a.get(attrName) - b.get(attrName), 2);
        }

        return Math.sqrt(sum);
    }

    public Double calculateClusterSSE(HashMap<String, Double> centroid, int clusterNo){
        double SSE = 0.0;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).clusterNo == clusterNo){
                SSE += Math.pow(euclideanDistance(centroid, records.get(i).getRecord()), 2);
            }
        }
        return SSE;
    }

    public Double calculateTotalSSE(LinkedList<HashMap<String,Double>> centroids){
        Double SSE = 0.0;
        for (int i = 0; i < centroids.size(); i++) {
            SSE += calculateClusterSSE(centroids.get(i), i);
        }
        return SSE;
    }

    public HashMap<String,Double> calculateWeighedCentroid(){
        double sum = 0.0;

        for (int i = 0; i < records.size(); i++) {
            if (!indicesOfCentroids.contains(i)) {
                double minDist = Double.MAX_VALUE;
                for (int ind : indicesOfCentroids) {
                    double dist = euclideanDistance(records.get(i).getRecord(), records.get(ind).getRecord());
                    if (dist<minDist)
                        minDist = dist;
                }
                if (indicesOfCentroids.isEmpty())
                    sum = 0.0;
                sum += minDist;
            }
        }

        double threshold = sum * random.nextDouble();

        for (int i = 0; i < records.size(); i++) {
            if (!indicesOfCentroids.contains(i)){
                double minDist = Double.MAX_VALUE;
                for (int ind : indicesOfCentroids) {
                    double dist = euclideanDistance(records.get(i).getRecord(), records.get(ind).getRecord());
                    if (dist<minDist)
                        minDist = dist;
                }
                sum += minDist;

                if (sum > threshold) {
                    indicesOfCentroids.add(i);
                    return records.get(i).getRecord();
                }
            }
        }

        return new HashMap<>();
    }

    public LinkedList<Record> getRecords() {
        return records;
    }

    public Double getMin(String attrName){
        return minimums.get(attrName);
    }

    public Double getMax(String attrName){
        return maximums.get(attrName);
    }
}
