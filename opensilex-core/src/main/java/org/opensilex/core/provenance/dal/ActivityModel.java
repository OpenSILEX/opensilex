//******************************************************************************
//                          ActivityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import javax.jdo.annotations.PersistenceCapable;

/**
 *
 * @author boizetal
 */
@PersistenceCapable(table = "provenance")
public class ActivityModel {
    String rdftype;    
    String startedAtTime;
    String endedAtTime;

    public String getRdftype() {
        return rdftype;
    }

    public void setRdftype(String rdftype) {
        this.rdftype = rdftype;
    }

    public String getStartedAtTime() {
        return startedAtTime;
    }

    public void setStartedAtTime(String startedAtTime) {
        this.startedAtTime = startedAtTime;
    }

    public String getEndedAtTime() {
        return endedAtTime;
    }

    public void setEndedAtTime(String endedAtTime) {
        this.endedAtTime = endedAtTime;
    }
    
    
    
}
