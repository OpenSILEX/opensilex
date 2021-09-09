package org.opensilex.core.ontology.dal.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

public class CaffeineOntologyCache extends AbstractOntologyCache {

    private final static long EXPIRE_AFTER_WRITE_DURATION = 24;
    private final static TimeUnit EXPIRE_AFTER_WRITE_TIME_UNIT = TimeUnit.HOURS;

    private Cache<URI,ClassModel> classesCache;

    protected CaffeineOntologyCache(SPARQLService sparql) throws URISyntaxException, SPARQLException {
        super(sparql);
    }

    protected void buildCache(){
        classesCache = Caffeine.newBuilder()
                .expireAfterWrite(EXPIRE_AFTER_WRITE_DURATION, EXPIRE_AFTER_WRITE_TIME_UNIT)
                .build();
    }

    private static CaffeineOntologyCache INSTANCE;

    /**
     * @apiNote use synchronized keyword in order to ensure thread-safety
     * @param sparql the {@link SPARQLService} used to access {@link OntologyDAO} if no entry was found into cache
     * @return OntologyApiCache shared instance
     */
    public synchronized static CaffeineOntologyCache getInstance(SPARQLService sparql) throws URISyntaxException, SPARQLException {
        if(INSTANCE == null){
            INSTANCE = new CaffeineOntologyCache(sparql);
        }
        return INSTANCE;
    }


    @Override
    public void invalidate() {
        classesCache.invalidateAll();
    }


    @Override
    protected void addClassToCache(URI key, ClassModel classModel) throws SPARQLException{
        classesCache.put(key,classModel);
    }

    @Override
    protected ClassModel getClassFromCache(URI classUri) {
        return classesCache.getIfPresent(classUri);
    }

    @Override
    protected void removeClassFromCache(URI classUri) {
        classesCache.invalidate(classUri);
    }

}
