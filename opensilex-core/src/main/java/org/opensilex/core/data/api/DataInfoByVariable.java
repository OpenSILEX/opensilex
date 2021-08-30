//******************************************************************************
//                        DataInfoByVariable.java
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

import java.time.Instant;
import java.util.List;

/**
 *
 * @author Arnaud Charleroy
 */
public class DataInfoByVariable {
    
    List<Instant> dates;
    Instant startDate;
    Instant endDate;

    public List<Instant> getDates() {
        return dates;
    }

    public void setDates(List<Instant> dates) {
        this.dates = dates;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
    
    
    
}
