//******************************************************************************
//                          SectionModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.dal;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * @author Maximilian Hart
 */
public class SectionModel  extends MongoModel {  
    
    private String commitAddress;
    private List<Map> sectionData;
    private String name;
    private Instant creationDate;
    private Instant lastUpdateDate;
    private String offset;

    public List<Map> getSectionData() {
        return sectionData;
    }
    public void setSectionData(List<Map> sectionData) {
        this.sectionData = sectionData;
    }
    public void setCommitAddress(String commitAddress) {
        this.commitAddress = commitAddress;
    }
    public String getCommitAddress() {
        return commitAddress;
    }
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
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String[] getUriSegments(MongoModel instance) {
        return new String[]{
            name, creationDate.toString()
        };
    }
}
