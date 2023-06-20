package org.opensilex.core.mongoService;

import org.opensilex.nosql.mongodb.MongoModel;

/**
 * @author Hamza Ikiou
 */
public class MongoModelTest extends MongoModel {

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
