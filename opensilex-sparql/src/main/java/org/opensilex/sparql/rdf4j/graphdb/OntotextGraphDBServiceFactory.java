package org.opensilex.sparql.rdf4j.graphdb;

import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;
import org.opensilex.utils.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Specialization of {@link RDF4JServiceFactory} which allow to install OpenSILEX on a Ontotext GraphDB triplestore.
 * The specificity of this factory resides in repository creation file template, which differs from rdf4j repository creation.
 * The default RDF4JServiceFactory doesn't work, since the repository template accepted by GraphDB is different
 *
 *</p>
 *
 * @see OntotextGraphDBConfig
 * @see com.ontotext.trree.free.GraphDBRepositoryConfig
 * @see com.ontotext.trree.monitorRepository.MonitorRepositoryConfig
 * @see com.ontotext.graphdb.fedx.GraphDBFedXRepositoryConfig
 *
 * @author rcolin
 */
@ServiceDefaultDefinition(config = OntotextGraphDBConfig.class)
public class OntotextGraphDBServiceFactory extends RDF4JServiceFactory {

    public OntotextGraphDBServiceFactory(OntotextGraphDBConfig config) {
        super(config);
    }

    @Override
    protected File getRepositoryCreationTemplateFile() throws IOException {
        return ClassUtils.getFileFromClassArtifact(getClass(), "repository/ontotext_graphdb_repository_template.ttl");
    }

    @Override
    public OntotextGraphDBConfig getImplementedConfig()  {
        return (OntotextGraphDBConfig) getConfig();
    }

    @Override
    protected Map<String, String> getCustomRepositorySettings() {
        OntotextGraphDBConfig graphDBConfig = getImplementedConfig();

        Map<String,String> settings = new HashMap<>();
        settings.put(OntotextGraphDBConfig.REPOSITORY_TYPE_TEMPLATE_PARAMETER, graphDBConfig.repositoryType());
        settings.put(OntotextGraphDBConfig.SAIL_TYPE_TEMPLATE_PARAMETER, graphDBConfig.sailType());
        settings.put(OntotextGraphDBConfig.QUERY_TIMEOUT_TEMPLATE_PARAMETER, String.valueOf(graphDBConfig.queryTimeout()));
        settings.put(OntotextGraphDBConfig.QUERY_LIMIT_RESULTS_TEMPLATE_PARAMETER, String.valueOf(graphDBConfig.queryLimitResult()));

        return settings;
    }
}
