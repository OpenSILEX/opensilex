//******************************************************************************
//                          ExperimentDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentDAO {

    protected final SPARQLService sparql;

    public ExperimentDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public ExperimentModel create(ExperimentModel instance) throws Exception {
        checkURIs(instance);
        sparql.create(instance);
        return instance;
    }

    public ExperimentModel update(ExperimentModel instance) throws Exception {
        checkURIs(instance);
        sparql.update(instance);
        return instance;
    }

    public void updateWithVariables(URI xpUri, List<URI> variablesUris) throws Exception {
        if (!sparql.uriExists(ExperimentModel.class, xpUri)) {
            throw new IllegalArgumentException("Unknown experiment " + xpUri);
        }
        sparql.updateObjectRelations(SPARQLDeserializers.nodeURI(xpUri), xpUri, Oeso.measures, variablesUris);
    }

    public void updateWithSensors(URI xpUri, List<URI> sensorsUris) throws Exception {
        if (!sparql.uriExists(ExperimentModel.class, xpUri)) {
            throw new IllegalArgumentException("Unknown experiment " + xpUri);
        }
        sparql.updateSubjectRelations(SPARQLDeserializers.nodeURI(xpUri), sensorsUris, Oeso.participatesIn, xpUri);
    }

    /**
     * check if all URI from uris have the typeResource as {@link RDF#type} into the SPARQL graph
     *
     * @param uris         the {@link List} of {@link URI} to check
     * @param typeResource the {@link Resource} indicating the {@link RDF#type
     */
    protected void checkURIs(List<URI> uris, Resource typeResource) throws URISyntaxException, SPARQLException {

        if (uris == null || uris.isEmpty()) {
            return;
        }
        for (URI uri : uris) {
            if (!sparql.uriExists(new URI(typeResource.getURI()), uri)) {
                throw new IllegalArgumentException("Trying to insert an experiment with an unknown " + typeResource.getLocalName() + " : " + uri);
            }
        }
    }

    /**
     * Check that all URI(s) which refers to a non {@link org.opensilex.sparql.annotations.SPARQLResource}-compliant model exists.
     *
     * @param model the experiment for which we check if all URI(s) exists
     * @throws SPARQLException          if the SPARQL uri checking query fail
     * @throws IllegalArgumentException if the given model contains a unknown URI
     */
    protected void checkURIs(ExperimentModel model) throws SPARQLException, IllegalArgumentException, URISyntaxException {

        // #TODO use a method to test in one query if multiple URI(s) exists and are of a given type, or use SHACL validation

        checkURIs(model.getInfrastructures(), (Oeso.Infrastructure));
        checkURIs(model.getSensors(), (Oeso.SensingDevice));
        checkURIs(model.getVariables(), (Oeso.Variable));
        checkURIs(model.getDevices(), (Oeso.Installation));

        if (model.getSpecies() != null && !sparql.uriExists(new URI(Oeso.Species.getURI()), model.getSpecies())) {
            throw new IllegalArgumentException("Trying to insert an experiment with an unknown species : " + model.getSpecies());
        }

    }

    public void delete(URI xpUri) throws Exception {
        sparql.delete(ExperimentModel.class, xpUri);
    }

    public void delete(List<URI> xpUris) throws Exception {
        sparql.delete(ExperimentModel.class, xpUris);
    }

    public ExperimentModel get(URI xpUri) throws Exception {
        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpUri, null);
        if (xp != null) {
            filterExperimentSensors(xp);
        }
        return xp;
    }

    /**
     * Remove all URI from {@link ExperimentModel#getSensors()} method which don't represents a {@link Oeso#SensingDevice}
     * in the SPARQL Graph
     *
     * @param xp the {@link ExperimentModel} to filter
     */
    protected void filterExperimentSensors(ExperimentModel xp) {
        if (xp.getSensors().isEmpty())
            return;

        // #TODO don't fetch URI which don't represents sensors and delete this method
        xp.getSensors().removeIf(sensor -> {
            try {
                return !sparql.uriExists(new URI(Oeso.SensingDevice.getURI()), sensor);
            } catch (SPARQLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * @param search  a subset of all attributes about an {@link ExperimentModel} search
     * @param orderByList an OrderBy List
     * @param page        the current page
     * @param pageSize    the page size
     * @return the ExperimentModel list
     */
    public ListWithPagination<ExperimentModel> search(ExperimentSearch search, List<OrderBy> orderByList, Integer page, Integer pageSize) throws Exception {

        ListWithPagination<ExperimentModel> xps = sparql.searchWithPagination(
                ExperimentModel.class,
                null,
                (SelectBuilder select) -> {
                    if (search != null) {
                        search.apply(select);
                    }
                },
                orderByList,
                page,
                pageSize
        );
        for (ExperimentModel xp : xps.getList()) {
            filterExperimentSensors(xp);
        }
        return xps;
    }
}
