//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal.variable;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.quality.QualityModel;
import org.opensilex.core.variable.dal.trait.TraitModel;
import org.opensilex.core.variable.dal.unit.UnitModel;
import org.opensilex.core.variable.dal.entity.EntityModel;
import org.opensilex.core.variable.dal.method.MethodModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;
import java.util.List;


@SPARQLResource(
        ontology = Oeso.class,
        resource = "Variable",
        graph = "variable",
        ignoreValidation = true
)
public class VariableModel extends BaseVariableModel<VariableModel> implements ClassURIGenerator<VariableModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    private String label;
    public static final String LABEL_FIELD = "label";

//    @SPARQLProperty(
//            ontology = RDFS.class,
//            property = "comment"
//    )
//    private String comment;
//    public static final String COMMENT_FIELD = "comment";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasLongName"
    )
    private String longName;
    public static final String LONG_NAME_FIELD_NAME = "longName";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSynonym"
    )
    private String synonym;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasEntity"
    )
    private ClassModel entity;
    public static final String ENTITY_FIELD_NAME = "entity";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasQuality"
    )
    private QualityModel quality;
    public static final String QUALITY_FIELD_NAME = "quality";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTrait"
    )
    private TraitModel trait;
    public static final String TRAIT_FIELD_NAME = "trait";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasMethod"
    )
    private MethodModel method;
    public static final String METHOD_FIELD_NAME = "method";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasUnit"
    )
    private UnitModel unit;
    public static final String UNIT_FIELD_NAME = "unit";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasLowerBound"
    )
    private Double lowerBound;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasUpperBound"
    )
    private Double upperBound;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDimension"
    )
    private String dimension;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

//    public String getComment() {
//        return comment;
//    }
//
//    public void setComment(String comment) {
//        this.comment = comment;
//    }

    public String getLongName() { return longName; }

    public void setLongName(String longName) { this.longName = longName; }

    public ClassModel getEntity() {
        return entity;
    }

    public void setEntity(ClassModel entity) {
        this.entity = entity;
    }

    public QualityModel getQuality() {
        return quality;
    }

    public void setQuality(QualityModel quality) {
        this.quality = quality;
    }

    public TraitModel getTrait() { return trait; }

    public void setTrait(TraitModel trait) { this.trait = trait; }

    public MethodModel getMethod() {
        return method;
    }

    public void setMethod(MethodModel method) {
        this.method = method;
    }

    public UnitModel getUnit() {
        return unit;
    }

    public void setUnit(UnitModel unit) {
        this.unit = unit;
    }

    public Double getLowerBound() { return lowerBound; }

    public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }

    public Double getUpperBound() { return upperBound; }

    public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }

    public String getSynonym() { return synonym; }

    public void setSynonym(String synonym) { this.synonym = synonym; }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    @Override
    public String[] getUriSegments(VariableModel instance) {
        return new String[]{
            "variable",
            instance.getLabel()
        };
    }
}

