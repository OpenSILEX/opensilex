/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.ModuleNotFoundException;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SPARQLModule extends OpenSilexModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLModule.class);

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return SPARQLConfig.class;
    }

    @Override
    public String getConfigId() {
        return "ontologies";
    }

    @Override
    public void startup() throws Exception {
        final String basePrefix;
        SPARQLConfig sparqlConfig = this.getConfig(SPARQLConfig.class);
        if (sparqlConfig != null) {
            basePrefix = sparqlConfig.baseURIAlias() + "-";
        } else {
            basePrefix = "";
        }

        SPARQLService sparql = sparqlConfig.sparql();
        SPARQLClassObjectMapper.forEach((Resource resource, SPARQLClassObjectMapper<?> mapper) -> {
            String resourceNamespace = mapper.getResourceGraphNamespace();
            String resourcePrefix = mapper.getResourceGraphPrefix();
            if (resourceNamespace != null && resourcePrefix != null && !resourcePrefix.isEmpty()) {
                sparql.addPrefix(basePrefix + mapper.getResourceGraphPrefix(), resourceNamespace + "#");
            }
        });
        URIDeserializer.setPrefixes(sparql.getPrefixes());

        
        SPARQLConfig cfg = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        sparql.addPrefix(cfg.baseURIAlias(), cfg.baseURI());

    }

    /**
     * Return configured platform base URI
     *
     * @return plateform URI
     */
    public static URI getPlatformURI() {
        try {
            if (OpenSilex.getInstance() == null) {
                return new URI("http://test.opensilex.org/");
            } else {
                return new URI(OpenSilex.getInstance().getModuleByClass(SPARQLModule.class).getConfig(SPARQLConfig.class).baseURI());
            }
        } catch (ModuleNotFoundException ex) {
            LOGGER.error("Base module not found, can't really happend because it's in the same package", ex);
            return null;
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid Base URI", ex);
            return null;
        }
    }

    public static URI getPlatformDomainGraphURI(String graphSuffix) throws URISyntaxException {
        return new URI(getPlatformURI().toString() + graphSuffix);
    }
}
