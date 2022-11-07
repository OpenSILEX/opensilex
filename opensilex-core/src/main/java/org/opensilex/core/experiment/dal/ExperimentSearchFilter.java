package org.opensilex.core.experiment.dal;

import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SparqlSearchFilter;

import java.net.URI;
import java.util.List;

/**
 * Object which group all filters (can be null/empty) which can apply for a {@link ExperimentModel} search
 * @author bmaussang
 */
public class ExperimentSearchFilter extends SparqlSearchFilter {

    private Integer year;
    private String name;
    private List<URI> species;
    private List<URI> factorCategories;
    private Boolean isEnded;
    private List<URI> projects;
    private Boolean isPublic;
    private List<URI> facilities;
    private UserModel user;

    public ExperimentSearchFilter() {
        super();
    }

    public Integer getYear() {
        return year;
    }

    public ExperimentSearchFilter setYear(Integer year) {
        this.year = year;
        return this;
    }

    public String getName() {
        return name;
    }

    public ExperimentSearchFilter setName(String name) {
        this.name = name;
        return this;
    }

    public List<URI> getSpecies() {
        return species;
    }

    public ExperimentSearchFilter setSpecies(List<URI> species) {
        this.species = species;
        return this;
    }

    public List<URI> getFactorCategories() {
        return factorCategories;
    }

    public ExperimentSearchFilter setFactorCategories(List<URI> factorCategories) {
        this.factorCategories = factorCategories;
        return this;
    }

    public Boolean isEnded() {
        return isEnded;
    }

    public ExperimentSearchFilter setEnded(Boolean ended) {
        isEnded = ended;
        return this;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public ExperimentSearchFilter setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    public Boolean isPublic() {
        return isPublic;
    }

    public ExperimentSearchFilter setPublic(Boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public ExperimentSearchFilter setFacilities(List<URI> facilities) {
        this.facilities = facilities;
        return this;
    }

    public UserModel getUser() {
        return user;
    }

    public ExperimentSearchFilter setUser(UserModel user) {
        this.user = user;
        return this;
    }

}
