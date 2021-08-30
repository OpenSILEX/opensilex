// https://stackabuse.com/guide-to-k-means-clustering-with-java/

package org.opensilex.core.data.clustering;

import java.util.HashMap;
import java.util.LinkedList;

public class Kmeans {

    // Higher precision means earlier termination
    // and higher error
    static final Double PRECISION = 0.0;

    /* K-Means++ implementation, initializes K centroids from data */
    public LinkedList<HashMap<String, Double>> kmeanspp(DataSet data, int K) {
        LinkedList<HashMap<String,Double>> centroids = new LinkedList<>();

        centroids.add(data.randomFromDataSet());

        for (int i = 1; i < K; i++) {
            centroids.add(data.calculateWeighedCentroid());
        }

        return centroids;
    }

    /* K-Means itself, it takes a dataset and a number K and adds class numbers
    * to records in the dataset */
    public void kmeans(DataSet data, int K){
        // Select K initial centroids
        LinkedList<HashMap<String,Double>> centroids = kmeanspp(data, K);

        // Initialize Sum of Squared Errors to max, we'll lower it at each iteration
        Double SSE = Double.MAX_VALUE;

        while (true) {

            // Assign observations to centroids
            LinkedList<DataSet.Record> records = data.getRecords();

            // For each record
            for (DataSet.Record record : records) {
                Double minDist = Double.MAX_VALUE;
                // Find the centroid at a minimum distance from it and add the record to its cluster
                for (int i = 0; i < centroids.size(); i++) {
                    Double dist = DataSet.euclideanDistance(centroids.get(i), record.getRecord());
                    if (dist < minDist) {
                        minDist = dist;
                        record.setClusterNo(i);
                    }
                }
            }

            // Recompute centroids according to new cluster assignments
            centroids = data.recomputeCentroids(K);

            // Exit condition, SSE changed less than PRECISION parameter
            Double newSSE = data.calculateTotalSSE(centroids);
            if (SSE-newSSE <= PRECISION) {
                break;
            }
            SSE = newSSE;
        }
    }
}
