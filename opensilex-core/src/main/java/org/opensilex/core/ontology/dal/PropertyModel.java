/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author vince
 */
public interface PropertyModel{

    URI getUri();

    void setUri(URI uri);

    URI getType();

    void setType(URI uri);

     SPARQLLabel getTypeLabel();

     void setTypeLabel(SPARQLLabel typeLabel);

    public String getName();

    void setName(String name);

    public void setLabel(SPARQLLabel label);

    public SPARQLLabel getLabel();

    public void setComment(SPARQLLabel comment);

    public SPARQLLabel getComment();

    public ClassModel getDomain();

    public void setDomain(ClassModel domain);

    public URI getTypeRestriction();

    public void setTypeRestriction(URI typeRestriction);

    default void fromModel(PropertyModel copy) {

        setUri(copy.getUri());
        setName(copy.getName());

        setType(copy.getType());
        if (copy.getTypeLabel() != null) {
            setTypeLabel(new SPARQLLabel(copy.getTypeLabel()));
        }

        if (copy.getLabel() != null) {
            setLabel(new SPARQLLabel(copy.getLabel()));
        }
        if (copy.getComment() != null) {
            setComment(new SPARQLLabel(copy.getComment()));
        }

        setTypeRestriction(copy.getTypeRestriction());
        if(copy.getDomain() != null){
            setDomain(new ClassModel(copy.getDomain(),false));
        }
    }

}
