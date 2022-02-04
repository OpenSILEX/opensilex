package org.opensilex.mobile.dal;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FormModel extends MongoModel {

    private List<URI> sectionUris;
    private List<String> children;
    private List<String> parents;
    private String commitAddress;
    private URI type;
    private String codeLot;
    private boolean isRoot;
    private Instant creationDate;

    private Instant lastUpdateDate;
    private String offset;


    /*@BsonCreator
    public FormModel(){}*/
    public FormModel(){};
    public FormModel(String codeLot, String commitAddress, boolean isRoot, Instant creationDate){
        constructorBis(codeLot, commitAddress, isRoot);
        this.creationDate = creationDate;
        this.lastUpdateDate = creationDate;

    }
    public FormModel(String codeLot, String commitAddress, boolean isRoot){
        constructorBis(codeLot, commitAddress, isRoot);
    }
    private void constructorBis(String codeLot, String commitAddress, boolean isRoot){
        this.codeLot = codeLot;
        this.commitAddress = commitAddress;
        this.isRoot = isRoot;
        sectionUris = new ArrayList<>();
        children = new ArrayList<>();
        parents = new ArrayList<>();
    }

    public List<URI> getSectionUris() {return sectionUris;}
    public void setSectionUris(List<URI> sectionUris) {this.sectionUris = sectionUris;}
    public List<String> getChildren() {return children;}
    public void setChildren(List<String> children) {this.children = children;}
    public List<String> getParents() {return parents;}
    public void setParents(List<String> parents) {this.parents = parents;}
    public void addParent(String parent){
        boolean allreadyExists = false;
        for(String s : this.parents){
            allreadyExists = s.equals(parent);
        }
        if(!allreadyExists){
            this.parents.add(parent);
        }
    }
    public void addChild(String child){
        boolean allreadyExists = false;
        for(String s : this.children){
            allreadyExists = s.equals(child);
        }
        if(!allreadyExists){
            this.children.add(child);
        }
    }
    public URI getType() {return type;}
    public void setType(URI type) {this.type = type;}
    public void setCommitAddress(String commitAddress) {
        this.commitAddress = commitAddress;
    }
    public String getCommitAddress() {
        return commitAddress;
    }
    public String getCodeLot() {return codeLot;}
    public void setCodeLot(String codeLot) {this.codeLot = codeLot;}
    public boolean isRoot() {return isRoot;}
    public void setRoot(boolean root) {isRoot = root;}
    public void setLastUpdateDate(Instant lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }
    public Instant getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }
    public Instant getLastUpdateDate() {
        return lastUpdateDate;
    }

    public String getOffset() {
        return offset;
    }
    public void setOffset(String offset) {
        this.offset = offset;
    }
    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{codeLot, creationDate.toString()};

    }

}
