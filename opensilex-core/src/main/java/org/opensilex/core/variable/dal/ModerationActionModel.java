package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.time.LocalDate;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "ModerationAction",
        graph = VariableModel.GRAPH
)
public class ModerationActionModel {
    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "dateAccepted"
    )
    private LocalDate dateAccepted;

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

    public LocalDate getDateAccepted() {
        return dateAccepted;
    }

    public void setDateAccepted(LocalDate dateAccepted) {
        this.dateAccepted = dateAccepted;
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
