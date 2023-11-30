package org.opensilex.nosql.mongodb.model;

import org.opensilex.nosql.mongodb.MongoModel;

import java.time.OffsetDateTime;
import java.util.List;

public class MongoTestModel extends MongoModel {

    private String string;
    private List<String> stringList;
    private Integer integer;
    private List<Integer> integerList;

    private MongoTestModel nestedMongoTestModel;
    private List<MongoTestModel> nestedMongoTestModelList;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }


    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public List<Integer> getIntegerList() {
        return integerList;
    }

    public void setIntegerList(List<Integer> integerList) {
        this.integerList = integerList;
    }

    public MongoTestModel getNestedMongoTestModel() {
        return nestedMongoTestModel;
    }

    public void setNestedMongoTestModel(MongoTestModel nestedMongoTestModel) {
        this.nestedMongoTestModel = nestedMongoTestModel;
    }

    public List<MongoTestModel> getNestedMongoTestModelList() {
        return nestedMongoTestModelList;
    }

    public void setNestedMongoTestModelList(List<MongoTestModel> nestedMongoTestModelList) {
        this.nestedMongoTestModelList = nestedMongoTestModelList;
    }
}
