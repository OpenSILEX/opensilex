package org.opensilex.nosql.models;

import org.opensilex.nosql.mongodb.MongoModel;

import java.util.UUID;

public class MongoTestModel extends MongoModel {

    protected String string;
    protected int integer;
    protected byte[] bytes;
    protected boolean bool;
    protected MongoTestModel nested;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getInteger() {
        return integer;
    }

    public void setInteger(int integer) {
        this.integer = integer;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public MongoTestModel getNested() {
        return nested;
    }

    public void setNested(MongoTestModel nested) {
        this.nested = nested;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
