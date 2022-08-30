package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.time.LocalDate;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "ModerationAction",
        graph = VariableModel.GRAPH
)
public class ModerationActionModel extends SPARQLResourceModel {
    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "date"
    )
    private LocalDate date;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasModerator"
    )
    private UserModel moderator;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasModerationActionType"
    )
    private String moderationActionType;

    public static final String MODERATION_ACTION_TYPE_FIELD_NAME = "moderationActionType";

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public UserModel getModerator() {
        return moderator;
    }

    public void setModerator(UserModel moderator) {
        this.moderator = moderator;
    }

    public String getModerationActionType() {
        return moderationActionType;
    }

    public void setModerationActionType(String moderationActionType) {
        this.moderationActionType = moderationActionType;
    }

}
