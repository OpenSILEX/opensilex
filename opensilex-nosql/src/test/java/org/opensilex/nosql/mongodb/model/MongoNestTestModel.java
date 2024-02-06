package org.opensilex.nosql.mongodb.model;

import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;

public class MongoNestTestModel extends MongoModel {

    private Document settings;

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }
}
