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
public interface PropertyModel {

    public URI getUri();

    public void setUri(URI uri);

    public String getName();

    public void setLabel(SPARQLLabel label);

    public SPARQLLabel getLabel();

    public void setComment(SPARQLLabel comment);

    public SPARQLLabel getComment();

    public URI getDomain();

    public void setDomain(URI domain);

    public URI getRange();

    public void setRange(URI range);

    public URI getTypeRestriction();

    public void setTypeRestriction(URI typeRestriction);

}
