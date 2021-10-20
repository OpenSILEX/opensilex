package org.opensilex.core.ontology.dal.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Ontology caching implementation based on Caffeine {@link Cache}
 */
public class CaffeineOntologyCache extends AbstractOntologyCache {

    private static final long EXPIRE_AFTER_WRITE_DURATION = 24;
    private static final TimeUnit EXPIRE_AFTER_WRITE_TIME_UNIT = TimeUnit.HOURS;

    private Cache<URI,ClassEntry> classesCache;

    protected CaffeineOntologyCache(SPARQLService sparql) throws OntologyCacheException {
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
    public static synchronized CaffeineOntologyCache getInstance(SPARQLService sparql) throws OntologyCacheException {
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
    protected ClassEntry getEntry(URI classUri) {
        return classesCache.getIfPresent(classUri);
    }

    @Override
    protected void addClassToCache(URI key, ClassEntry entry){
        classesCache.put(key,entry);
    }


    @Override
    protected void removeClassFromCache(URI classUri) {
        classesCache.invalidate(classUri);
    }

    @Override
    public long length() {
        return classesCache.estimatedSize();
    }
}
