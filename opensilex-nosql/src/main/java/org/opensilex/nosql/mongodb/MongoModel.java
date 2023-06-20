//******************************************************************************
//                          MongoModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.time.Instant;

/**
 *
 * @author Alice Boizet
 */
public class MongoModel implements ClassURIGenerator<MongoModel> {

    protected URI baseURI;
    protected URI uri;
    protected URI publisher;
    protected Instant publicationDate;
    protected Instant lastUpdateDate;

    public static final String URI_FIELD = "uri";
    public static final String MONGO_ID_FIELD = "_id";


    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getPublisher() {
        return publisher;
    }

    public void setPublisher(URI publisher) {
        this.publisher = publisher;
    }

    public Instant getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Instant publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Instant getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Instant lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
