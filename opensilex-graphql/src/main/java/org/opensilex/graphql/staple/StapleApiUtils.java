/*******************************************************************************
 *                         StapleApiUtils.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 27/09/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.graphql.staple;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.jvnet.hk2.annotations.Service;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;

import javax.inject.Inject;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Valentin Rigolle
 */
@Service
public class StapleApiUtils {
    @Inject
    private OpenSilex openSilex;

    @Inject
    private SPARQLService sparql;

    //region Public methods

    public Model getStapleModel() throws Exception {
        return computeStapleModel();
    }

    public Map<URI, URI> getResourceGraphMap() throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        Map<URI, URI> result = new HashMap<>();
        SPARQLClassObjectMapperIndex index = sparql.getMapperIndex();
        for (Class<?> resourceClass : openSilex.getAnnotatedClasses(SPARQLResource.class)) {
            if (!(SPARQLResourceModel.class.isAssignableFrom(resourceClass))) {
                continue;
            }
            SPARQLClassObjectMapper<?> mapper = index.getForClass(resourceClass);
            if (mapper.getDefaultGraphURI() != null) {
                result.put(URI.create(mapper.getRDFType().getURI()), mapper.getDefaultGraphURI());
            }
        }
        return result;
    }

    //endregion

    //region Staple model computation

    private Model computeSelectedOntologiesModel() throws Exception {
        Model model = ModelFactory.createDefaultModel();

        for (SPARQLExtension module : openSilex.getModulesImplementingInterface(SPARQLExtension.class)) {
            for (OntologyFileDefinition definition : module.getOntologiesFiles()) {
                if (definition.getAddToStaple()) {
                    try (InputStream file = Files.newInputStream(ClassUtils.getFileFromClassArtifact(module.getClass(), definition.getFilePath()).toPath())) {
                        RDFParser.create()
                                .source(file)
                                .lang(definition.getFileType())
                                .build()
                                .parse(model);
                    }
                }
            }
        }

        return model;
    }

    private Set<URI> extractClassUrisFromOntologiesModel(Model selectedOntologiesModel) {
        List<Resource> types = selectedOntologiesModel.listResourcesWithProperty(RDF.type, OWL.Class).toList();
        return types.stream()
                .filter(res -> res.getURI() != null)
                .map(res -> URI.create(res.getURI()))
                .collect(Collectors.toSet());
    }

    private Model computeStapleModel() throws Exception {
        Model selectedOntologiesModel = computeSelectedOntologiesModel();
        Set<URI> rootClassUris = extractClassUrisFromOntologiesModel(selectedOntologiesModel);
        return new StapleModelBuilder(rootClassUris, SPARQLModule.getOntologyStoreInstance()).build();
    }

    //endregion
}
