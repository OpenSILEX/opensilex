package org.opensilex.core.data.clustering;

import java.net.URI;

public class ClusterDataSet {
    public URI uri;
    public Double value;
    public Double secondValue = 0.0;

    public ClusterDataSet(URI uri, int value) {
        this.uri = uri;
        this.value = Double.valueOf(value);
    }

    public ClusterDataSet(URI uri, Double value) {
        this.uri = uri;
        this.value = value;
    }
    
    public ClusterDataSet(URI uri, Object value) {
        this.uri = uri;
        this.value = Double.valueOf(value.toString());
    }

}