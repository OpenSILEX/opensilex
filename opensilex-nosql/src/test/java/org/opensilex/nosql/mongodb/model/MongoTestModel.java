package org.opensilex.nosql.mongodb.model;

import org.opensilex.nosql.mongodb.MongoModel;

import java.util.List;

public class MongoTestModel extends MongoModel {

    private String name;
    public static final String NAME_FIELD = "name";
    private List<String> tags;

    public static final String TAGS_FIELD = "tags";


    public static final String KEY_FIELD = "key";
    private Integer key;

    private List<Integer> values;
    public static final String VALUES_FIELD = "values";

    private MongoTestModel nested;
    public static final String NESTED_FIELD = "nested";


    private List<MongoTestModel> nestedList;
    public static final String NESTED_LIST_FIELD = "nestedList";


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    public MongoTestModel getNested() {
        return nested;
    }

    public void setNested(MongoTestModel nested) {
        this.nested = nested;
    }

    public List<MongoTestModel> getNestedList() {
        return nestedList;
    }

    public void setNestedList(List<MongoTestModel> nestedList) {
        this.nestedList = nestedList;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
               this.getName()
        };
    }
}
