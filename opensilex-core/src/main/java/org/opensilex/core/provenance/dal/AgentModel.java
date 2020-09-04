/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.provenance.dal;

import java.util.Map;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

/**
 *
 * @author boizetal
 */
@PersistenceCapable(table="provenance")
public class AgentModel {
    @Persistent(defaultFetchGroup="true")
    Map sensingDevice;
    @Persistent(defaultFetchGroup="true")
    Map operator;

    public Map getSensingDevice() {
        return sensingDevice;
    }

    public void setSensingDevice(Map sensingDevice) {
        this.sensingDevice = sensingDevice;
    }

    public Map getOperator() {
        return operator;
    }

    public void setOperator(Map operator) {
        this.operator = operator;
    }
    
}
