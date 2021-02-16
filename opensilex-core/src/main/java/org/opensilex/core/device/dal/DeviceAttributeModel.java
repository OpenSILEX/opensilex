/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.dal;

import java.net.URI;
import java.util.Map;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 *
 * @author sammy
 */
public class DeviceAttributeModel extends MongoModel {
      
    Map<String, String> attribute;

    public Map<String, String> getAttribute() {
        return attribute;
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }

}
