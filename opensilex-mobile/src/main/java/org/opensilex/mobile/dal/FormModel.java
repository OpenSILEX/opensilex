package org.opensilex.mobile.dal;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.opensilex.nosql.mongodb.MongoModel;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FormModel extends MongoModel {

    private List<URI> sectionUris;
    private List<URI> children;
    private List<URI> parents;
    private List<URI> availableChildren;
    private String commitAddress;
    private String type;
    private String codeLot;
    private boolean isRoot;
    private Instant creationDate;
    private Instant lastUpdateDate;
    private String offset;

    @BsonCreator
    public FormModel(){}
    public FormModel(String codeLot, String commitAddress, boolean isRoot, Instant creationDate){
        constructorBis(codeLot, commitAddress, isRoot);
        this.creationDate = creationDate;
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
        availableChildren = new ArrayList<>();
    }

    public List<URI> getSectionUris() {return sectionUris;}
    public void setSectionUris(List<URI> sectionUris) {this.sectionUris = sectionUris;}
    public List<URI> getChildren() {return children;}
    public void setChildren(List<URI> children) {this.children = children;}
    public List<URI> getParents() {return parents;}
    public void setParents(List<URI> parents) {this.parents = parents;}
    public void addParent(URI parent){this.parents.add(parent);}
    public void addAvChild(URI child){this.availableChildren.add(child);}
    public List<URI> getAvailableChildren() {return availableChildren;}
    public void setAvailableChildren(List<URI> availableChildren) {this.availableChildren = availableChildren;}
    public String getType() {return type;}
    public void setType(String type) {this.type = type;}
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
    public String[] getUriSegments(MongoModel instance) {
        return new String[]{codeLot, creationDate.toString()};
    }

}
