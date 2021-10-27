/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.utils.AbstractURIGenerator;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author vmigot
 */
public class ScientificObjectURIGenerator extends AbstractURIGenerator<ScientificObjectModel> {

    private final String soPrefix;
    private static final String SCIENTIFIC_OBJECT_PREFIX = "so-";

    public ScientificObjectURIGenerator(URI prefix, ExperimentModel xp) {
        Objects.requireNonNull(prefix);
        if(xp == null){
            this.soPrefix = prefix.toString();
        }else{
            this.soPrefix = UriBuilder.fromUri(prefix).path(xp.getName()).build().toString();
        }
    }

    public URI generateURI(ScientificObjectModel instance, int retryCount) throws UnsupportedEncodingException, URISyntaxException {
        return super.generateURI(soPrefix, instance, retryCount);
    }

    @Override
    public URI generateURI(String prefix, ScientificObjectModel instance, int retryCount) throws UnsupportedEncodingException, URISyntaxException {
        return generateURI(instance,retryCount);
    }

    @Override
    public String getInstanceURI(ScientificObjectModel instance) throws UnsupportedEncodingException {
        return SCIENTIFIC_OBJECT_PREFIX+instance.getName();
    }


}
