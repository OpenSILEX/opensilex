package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 * Vocabulary definition from the SOSA Extension ontology.
 * @see <a href="https://www.w3.org/TR/vocab-ssn-ext/">https://www.w3.org/TR/vocab-ssn-ext/</a>
 *
 */
public class SOSA {

    public static final String DOMAIN = "http://www.w3.org/ns/sosa/";
    public static final String PREFIX = "sosa";
    public static final String NS = DOMAIN;

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
     * @see <a href="https://www.w3.org/TR/vocab-ssn-ext/#sosa:ObservationCollection">sosa:ObservationCollection</a>
     * Collection of one or more observations, whose members share a common value for one or more property
     */
    public static final Resource ObservationCollection = Ontology.resource(NS,"ObservationCollection");/**

     /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn/#SOSAObservableProperty">sosa:ObservableProperty</a>
     * An observable quality (property, characteristic) of a FeatureOfInterest.
     */
    public static final Resource ObservableProperty = Ontology.resource(NS,"ObservableProperty");/**

     /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn/#SOSAObservation">sosa:Observation</a>
     *  Act of carrying out an (Observation) Procedure to estimate or calculate a value of a property of a FeatureOfInterest. Links to a Sensor to describe what made the Observation and how; links to an ObservableProperty to describe what the result is an estimate of, and to a FeatureOfInterest to detail what that property was associated with.
     */
    public static final Resource Observation = Ontology.resource(NS,"Observation");/**

     /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn/#SOSAFeatureOfInterest">sosa:FeatureOfInterest</a>
     *  The thing whose property is being estimated or calculated in the course of an Observation to arrive at a Result, or whose property is being manipulated by an Actuator, or which is being sampled or transformed in an act of Sampling.
     */
    public static final Resource FeatureOfInterest = Ontology.resource(NS,"FeatureOfInterest");/**

     /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn/#SOSAobservedProperty">sosa:observedProperty</a>
     * Relation linking an Observation to the property that was observed. The ObservableProperty should be a property of the FeatureOfInterest (linked by hasFeatureOfInterest) of this Observation.
     */
    public static final Property observedProperty = Ontology.property(NS,"observedProperty");

    /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn-ext/#sosa:hasMember">sosa:hasMember</a>
     * Link to a member of a collection of observations that share the same value for one or more of the characteristic properties
     */
    public static final Property hasMember = Ontology.property(NS,"hasMember");

    /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn/#SOSAhasFeatureOfInterest">sosa:hasFeatureOfInterest</a>
     *  A relation between an Observation and the entity whose quality was observed, or between an Actuation and the entity whose property was modified, or between an act of Sampling and the entity that was sampled.
     */
    public static final Property hasFeatureOfInterest = Ontology.property(NS,"hasFeatureOfInterest");

    /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn-ext/#sosa:hasUltimateFeatureOfInterest">sosa:hasUltimateFeatureOfInterest</a>
     *  link to the ultimate feature of interest of an observation or act of sampling. This is useful when the proximate feature of interest is a sample of the ultimate feature of interest, directly or trasntitively.
     */
    public static final Property hasUltimateFeatureOfInterest = Ontology.property(NS,"hasUltimateFeatureOfInterest");

    /**
     * @see <a href="https://www.w3.org/TR/vocab-ssn/#SOSAisFeatureOfInterestOf">sosa:isFeatureOfInterestOf</a>
     *  A relation between a FeatureOfInterest and an Observation about it or an Actuation acting on it, or an act of Sampling that sampled it.
     */
    public static final Property isFeatureOfInterestOf = Ontology.property(NS,"isFeatureOfInterestOf");
}
