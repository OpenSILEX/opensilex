//******************************************************************************
//                        DataClusterReturn.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensilex.core.data.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author alexandre
 */
public class DataClusterReturn {

    public String method;
    public int nbClusters;
    public List<DataGetDTOCluster> data;
    public List<ClusterInfoDTO> clusterInfo;

    public DataClusterReturn(String method, int nbCluster) throws IOException {
        this.method = method;
        this.nbClusters = nbCluster;
    }
    
    public void setNbClusters(int nbClusters) {
        this.nbClusters = nbClusters;
    }
    
    public void setData(List<DataGetDTOCluster> datas) {
        this.clusterInfo = new ArrayList<>();
        for (int i = 0; i < this.nbClusters; i++) {
            ClusterInfoDTO cluster = new ClusterInfoDTO(i + 1);
            this.clusterInfo.add(cluster);
        }
        for (DataGetDTOCluster dataCluster : datas) {
            if (dataCluster.getClusterID() == 0) { // Avoid cluster id n°0
                dataCluster.setClusterID(this.nbClusters);
            }
            int value = (int) dataCluster.getValue();
            this.clusterInfo.get(dataCluster.getClusterID() - 1).updateMinMaxValue(value);
        }
        this.data = datas;
        orderClusterInfo();
        updateClustersById();
    }
    
    private void orderClusterInfo() {
        // Sort by values
        Collections.sort(this.clusterInfo, (ClusterInfoDTO cluster1, ClusterInfoDTO cluster2) -> {
            if (cluster1.getMinValue() > cluster2.getMinValue()) {
                return 1;
            } else if (cluster1.getMinValue() < cluster2.getMinValue()) {
                return -1;
            } else {
                return 0;
            }
        });
        for (int i = 0; i < this.nbClusters; i++) { // Update cluster id n°
            this.clusterInfo.get(i).setId(i + 1);
        }
    }
    
    private void updateClustersById() {
        for (DataGetDTOCluster dataCluster : this.data) {
            for (ClusterInfoDTO cluster : this.clusterInfo) {
                int value = (int) dataCluster.getValue();
                if (cluster.isInRange(value)) {
                    dataCluster.setClusterID(cluster.getId());
                    break;
                }
            }
        }
    }
}
