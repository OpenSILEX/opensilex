package org.opensilex.core.ontology.dal.cache;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;

/**
 * @author rcolin
 * An no-cache implementation which only relies on {@link OntologyDAO}
 */
public class NoOntologyCacheImpl implements OntologyCache {

    protected final SPARQLService sparql;
    protected final OntologyDAO ontologyDAO;

    private NoOntologyCacheImpl(SPARQLService sparql) {
        this.sparql = sparql;
        this.ontologyDAO = new OntologyDAO(sparql);
    }

    private static NoOntologyCacheImpl INSTANCE;

    /**
     * @apiNote use synchronized keyword in order to ensure thread-safety
     * @param sparql the {@link SPARQLService} used to access {@link OntologyDAO} if no entry was found into cache
     * @return NoOntologyCacheImpl shared instance
     */
    public static synchronized  NoOntologyCacheImpl getInstance(SPARQLService sparql) {
        if(INSTANCE == null){
            INSTANCE = new NoOntologyCacheImpl(sparql);
        }
        return INSTANCE;
    }

    @Override
    public SPARQLTreeListModel<ClassModel> getSubClassesOf(URI classUri, String stringPattern, String lang, boolean ignoreRootClasses) throws OntologyCacheException {
        try{
            return ontologyDAO.searchSubClasses(classUri,ClassModel.class,stringPattern,lang,ignoreRootClasses,null);
        }catch (Exception e){
            throw new OntologyCacheException(e);
        }
    }

    @Override
    public ClassModel getClassModel(URI classUri, String lang) throws OntologyCacheException {
        return this.getClassModel(classUri,null,lang);
    }

    @Override
    public ClassModel getClassModel(URI classUri, URI parentClassUri, String lang) throws OntologyCacheException {
        try{
            return ontologyDAO.getClassModel(classUri,parentClassUri,lang);
        }catch (Exception e){
            throw new OntologyCacheException(e);
        }
    }

    @Override
    public void addClass(ClassModel classModel) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void updateClass(ClassModel classModel) throws OntologyCacheException {
        // no-cache impl
    }

    @Override

    public void removeClass(URI classUris) {
        // no-cache impl
    }

    @Override
    public void invalidate() {
        // no-cache impl
    }

    @Override
    public void populate(List<URI> classUris) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public SPARQLTreeListModel<DatatypePropertyModel> searchDataProperties(URI domain, String lang) throws OntologyCacheException {
        try {
            return ontologyDAO.searchDataProperties(domain,null, lang);
        } catch (Exception e) {
            throw new OntologyCacheException(e);
        }
    }

    @Override
    public void createDataProperty(DatatypePropertyModel property) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void updateDataProperty(DatatypePropertyModel property) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void deleteDataProperty(URI propertyURI, URI domain) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public SPARQLTreeListModel<ObjectPropertyModel> searchObjectProperties(URI domain, String lang) throws OntologyCacheException {
        try {
            return ontologyDAO.searchObjectProperties(domain, null,lang);
        } catch (Exception e) {
            throw new OntologyCacheException(e);
        }
    }

    @Override
    public void createObjectProperty(ObjectPropertyModel property) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void updateObjectProperty(ObjectPropertyModel property) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void deleteObjectProperty(URI propertyURI, URI domain) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void addRestriction(OwlRestrictionModel restriction) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void updateRestriction(OwlRestrictionModel restriction) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public void deleteRestriction(URI restrictionUri, URI domain) throws OntologyCacheException {
        // no-cache impl
    }

    @Override
    public DatatypePropertyModel getTopDatatypePropertyModel() throws OntologyCacheException {
        try {
            return sparql.loadByURI(DatatypePropertyModel.class,ontologyDAO.getTopDataPropertyUri(), OpenSilex.DEFAULT_LANGUAGE,null);
        }catch (Exception e){
            throw new OntologyCacheException(e);
        }
    }

    @Override
    public ObjectPropertyModel getTopObjectPropertyModel() throws OntologyCacheException {
        try {
            return sparql.loadByURI(ObjectPropertyModel.class,ontologyDAO.getTopObjectPropertyUri(), OpenSilex.DEFAULT_LANGUAGE,null);
        }catch (Exception e){
            throw new OntologyCacheException(e);
        }

    }
}
