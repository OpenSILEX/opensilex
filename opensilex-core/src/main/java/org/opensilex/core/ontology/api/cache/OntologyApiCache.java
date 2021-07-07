package org.opensilex.core.ontology.api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author rcolin
 * A simple cache for storing classes and properties.
 * This implementation is based on Caffeine {@link Cache}.
 *
 * The expire after write duration is 30 min.
 */
public class OntologyApiCache {

    private static OntologyApiCache INSTANCE;

    /**
     * @apiNote use synchronized keyword in order to ensure thread-safety
     * @param sparql the {@link SPARQLService} used to access {@link OntologyDAO} if no entry was found into cache
     * @return OntologyApiCache shared instance
     */
    public synchronized static OntologyApiCache getInstance(SPARQLService sparql){
        if(INSTANCE == null){
            INSTANCE = new OntologyApiCache(sparql);
        }
        return INSTANCE;
    }

    /**
     * OntologyDao used if no entry was found into cache
     */
    private final OntologyDAO ontologyDAO;

    private static final Cache<URI, List<ResourceTreeDTO>> subClassesCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();


    private static final Cache<URI, List<ResourceTreeDTO>> propertiesCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private OntologyApiCache(SPARQLService sparql) {
        this.ontologyDAO = new OntologyDAO(sparql);
    }

    protected List<ResourceTreeDTO> getSubClassesOfFromDao(UserModel user, URI classUri, String stringPattern) throws Exception {
        SPARQLTreeListModel<?> tree = ontologyDAO.searchSubClasses(
                classUri,
                ClassModel.class,
                stringPattern,
                user,
                false,
                null
        );

        return ResourceTreeDTO.fromResourceTree(tree);
    }

    protected List<ResourceTreeDTO> populateSubClassesCache(UserModel user, URI key) throws Exception {
       return getSubClassesOfFromDao(user,key,null);
    }

    public List<ResourceTreeDTO> searchSubClassesOf(UserModel user, URI parentClass, String stringPattern, boolean ignoreRootClasses) throws Exception {

        List<ResourceTreeDTO> treeDto;
        AtomicReference<Exception> e = new AtomicReference<>();

        URI formattedClassUri = SPARQLDeserializers.formatURI(parentClass);

        if(!StringUtils.isEmpty(stringPattern)){
            treeDto = getSubClassesOfFromDao(user,formattedClassUri,stringPattern);
        }else{
            // try to get classes from cache if no string pattern.

            treeDto = subClassesCache.get(formattedClassUri,(key) -> {
                try {
                    return populateSubClassesCache(user,key);
                } catch (Exception exception) {
                    e.set(exception);
                    return null;
                }
            });
        }

        if(e.get() != null){
            throw e.get();
        }

        // handle root classes ignore manually, it allow to store the same results in cache whether ignoreRootClasses is true or false
        if(ignoreRootClasses && !CollectionUtils.isEmpty(treeDto)){
            treeDto = treeDto.get(0).getChildren();
        }

        return treeDto;
    }

    /**
     * Invalidate all classes stored by cache.
     * @apiNote all properties are also invalidated
     */
    public void invalidateClasses(){
        subClassesCache.invalidateAll();
        invalidateProperties();
    }


    protected List<ResourceTreeDTO> populatePropertiesCache(UserModel user, URI domain) throws Exception {

        SPARQLTreeListModel dataPropertyTree = ontologyDAO.searchDataProperties(domain, user);
        SPARQLTreeListModel objectPropertyTree = ontologyDAO.searchObjectProperties(domain, user);
        return ResourceTreeDTO.fromResourceTree(dataPropertyTree, objectPropertyTree);
    }

    public List<ResourceTreeDTO> getProperties(UserModel user, URI domain) throws Exception {

        URI formattedDomain = SPARQLDeserializers.formatURI(domain);
        AtomicReference<Exception> e = new AtomicReference<>();

        List<ResourceTreeDTO> treeDTO = propertiesCache.get(formattedDomain,(key) -> {
            try {
                return populatePropertiesCache(user,key);
            } catch (Exception exception) {
                e.set(exception);
                return null;
            }
        });

        if(e.get() != null){
            throw e.get();
        }

        return treeDTO;
    }

    /**
     * Invalidate all properties stored by cache
     */
    public void invalidateProperties() {
        propertiesCache.invalidateAll();
    }
}
