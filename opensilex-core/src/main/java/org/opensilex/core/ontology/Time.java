package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 * Vocabulary definition from the Time ontology.
 * @see <a href="https://www.w3.org/TR/owl-time/">https://www.w3.org/TR/owl-time/</a>
 * @author Renaud COLIN
 */
public class Time {

    public static final String DOMAIN = "http://www.w3.org/2006/time";

    public static final String PREFIX = "time";

    public static final String NS = DOMAIN + "#";

    /**
     * The namespace of the vocabulary as a string
     *
     * @return namespace as String
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }

    /**
     * Vocabulary namespace
     */
    public static final Resource NAMESPACE = Ontology.resource(NS);

    /**
     * @see <a href="https://www.w3.org/TR/owl-time/#time:Instant">time:Instant</a>
     * OWL Class which define a temporal entity with zero duration.
     */
    public static final Resource Instant = Ontology.resource(NS,"Instant");

    /**
     * @see <a href="https://www.w3.org/TR/owl-time/#time:hasTime">time:hasTime</a>
     * Supports the association of a temporal entity (instant or interval) to any thing.
     */
    public static final Property hasTime = Ontology.property(NS,"hasTime");

    /**
     * @see <a href="https://www.w3.org/TR/owl-time/#time:hasBeginning">time:hasBeginning</a>
     * Beginning of a temporal entity.
     */
    public static final Property hasBeginning = Ontology.property(NS,"hasBeginning");

    /**
     * @see <a href="https://www.w3.org/TR/owl-time/#time:hasEnd">time:hasEnd</a>
     * End of a temporal entity.
     */
    public static final Property hasEnd = Ontology.property(NS,"hasEnd");

    /**
     * @see <a href="https://www.w3.org/TR/owl-time/#time:inXSDDateTimeStamp">time:inXSDDateTimeStamp</a>
     * OWL Property which associate a xsd:dateTimeStamp datatype to an time:Instant
     */
    public static final Property inXSDDateTimeStamp = Ontology.property(NS,"inXSDDateTimeStamp");
}
