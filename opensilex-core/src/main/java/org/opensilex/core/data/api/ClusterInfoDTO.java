//******************************************************************************
//                        ClusterInfoDTO.java
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;

/**
 * @author alexandre
 */

public class ClusterInfoDTO {

    private int id;
    private int minValue = Integer.MAX_VALUE;
    private int maxValue = Integer.MIN_VALUE;

    public ClusterInfoDTO(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    @JsonProperty("min_value")
    public int getMinValue() {
        return this.minValue;
    }

    @JsonProperty("max_value")
    public int getMaxValue() {
        return this.maxValue;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public boolean isInRange(int value) {
        return value >= this.minValue && value <= this.maxValue;
    }
    
    public boolean updateMinMaxValue(int value) {
        if (setIfMinValue(value)) {
            return true;
        } else return setIfMaxValue(value);
    }

    public boolean setIfMinValue(int value) {
        if (!isMinValueIsLessThanValue(value)) {
            this.minValue = value;
            if (this.maxValue == Integer.MIN_VALUE) // Handle if there is only one value entered
                this.maxValue = value;
            return true;
        } else {
            return false;
        }
    }

    public boolean setIfMaxValue(int value) {
        if (!isMaxValueIsGreaterThanValue(value)) {
            this.maxValue = value;
            if (this.minValue == Integer.MAX_VALUE) // Handle if there is only one value entered
                this.minValue = value;
            return true;
        } else {
            return false;
        }
    }

    private boolean isMinValueIsLessThanValue(int value) {
        return this.minValue < value;
    }

    private boolean isMaxValueIsGreaterThanValue(int value) {
        return this.maxValue > value;
    }

}
