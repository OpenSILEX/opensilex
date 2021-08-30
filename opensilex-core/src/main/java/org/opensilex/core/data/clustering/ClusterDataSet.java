//******************************************************************************
//                        ClusterDataSet.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensilex.core.data.clustering;

import java.net.URI;

/**
 * @author alexandre
 */

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