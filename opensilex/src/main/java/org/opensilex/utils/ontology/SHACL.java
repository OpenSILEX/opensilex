/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.ontology;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 *
 * @author vincent
 */
public class SHACL {

    public static final String ONTOLOGY_FILE_URI = "https://www.w3.org/ns/shacl.ttl";
    public static final Lang ONTOLOGY_FILE_FORMAT = RDFLanguages.TURTLE;

    public static final String PREFIX = "sh";
    public static final String BASE_URI = "http://www.w3.org/ns/shacl";
    public static final String NAMESPACE = BASE_URI + "#";
    public static final Node GRAPH = NodeFactory.createURI(NAMESPACE + "/");

    // W3C Shapes Constraint Language (SHACL) Vocabulary
    // Version from 2017-07-20
    // Shapes vocabulary -----------------------------------------------------------
    public static final Resource Shape = Ontology.resource(NAMESPACE, "Shape");
    public static final Resource NodeShape = Ontology.resource(NAMESPACE, "NodeShape");
    public static final Resource PropertyShape = Ontology.resource(NAMESPACE, "PropertyShape");

    public static final Property deactivated = Ontology.property(NAMESPACE, "deactivated");
    public static final Property targetClass  = Ontology.property(NAMESPACE, "targetClass ");
    public static final Property targetNode  = Ontology.property(NAMESPACE, "targetNode ");
    public static final Property targetObjectsOf = Ontology.property(NAMESPACE, "targetObjectsOf");
    public static final Property targetSubjectsOf = Ontology.property(NAMESPACE, "targetSubjectsOf");
    public static final Property message = Ontology.property(NAMESPACE, "message");
    public static final Property severity = Ontology.property(NAMESPACE, "severity");

    // Node kind vocabulary --------------------------------------------------------
    public static final Resource NodeKind = Ontology.resource(NAMESPACE, "NodeKind");
    public static final Resource BlankNode = Ontology.resource(NAMESPACE, "BlankNode");
    public static final Resource BlankNodeOrIRI = Ontology.resource(NAMESPACE, "BlankNodeOrIRI");
    public static final Resource BlankNodeOrLiteral = Ontology.resource(NAMESPACE, "BlankNodeOrLiteral");
    public static final Resource IRI = Ontology.resource(NAMESPACE, "IRI");
    public static final Resource IRIOrLiteral = Ontology.resource(NAMESPACE, "IRIOrLiteral");
    public static final Resource Literal = Ontology.resource(NAMESPACE, "Literal");

    // Results vocabulary ----------------------------------------------------------
    public static final Resource ValidationReport = Ontology.resource(NAMESPACE, "ValidationReport");

    public static final Property conforms = Ontology.property(NAMESPACE, "conforms");
    public static final Property result = Ontology.property(NAMESPACE, "result");
    public static final Property shapesGraphWellFormed = Ontology.property(NAMESPACE, "shapesGraphWellFormed");

    public static final Resource AbstractResult = Ontology.resource(NAMESPACE, "AbstractResult");
    public static final Resource ValidationResult = Ontology.resource(NAMESPACE, "ValidationResult");
    public static final Resource Severity = Ontology.resource(NAMESPACE, "Severity");
    public static final Resource Info = Ontology.resource(NAMESPACE, "Info");
    public static final Resource Violation = Ontology.resource(NAMESPACE, "Violation");
    public static final Resource Warning = Ontology.resource(NAMESPACE, "Warning");

    public static final Property detail = Ontology.property(NAMESPACE, "detail");
    public static final Property focusNode = Ontology.property(NAMESPACE, "focusNode");
    public static final Property resultMessage = Ontology.property(NAMESPACE, "resultMessage");
    public static final Property resultPath = Ontology.property(NAMESPACE, "resultPath");
    public static final Property resultSeverity = Ontology.property(NAMESPACE, "resultSeverity");
    public static final Property sourceConstraint = Ontology.property(NAMESPACE, "sourceConstraint");
    public static final Property sourceShape = Ontology.property(NAMESPACE, "sourceShape");
    public static final Property sourceConstraintComponent = Ontology.property(NAMESPACE, "sourceConstraintComponent");
    public static final Property value = Ontology.property(NAMESPACE, "value");

    // Graph properties ------------------------------------------------------------
    public static final Property shapesGraph = Ontology.property(NAMESPACE, "shapesGraph");
    public static final Property suggestedShapesGraph = Ontology.property(NAMESPACE, "suggestedShapesGraph");
    public static final Property entailment = Ontology.property(NAMESPACE, "entailment");

    // Path vocabulary -------------------------------------------------------------
    public static final Property path = Ontology.property(NAMESPACE, "path");
    public static final Property inversePath = Ontology.property(NAMESPACE, "inversePath");
    public static final Property alternativePath = Ontology.property(NAMESPACE, "alternativePath");
    public static final Property zeroOrMorePath = Ontology.property(NAMESPACE, "zeroOrMorePath");
    public static final Property oneOrMorePath = Ontology.property(NAMESPACE, "oneOrMorePath");
    public static final Property zeroOrOnePath = Ontology.property(NAMESPACE, "zeroOrOnePath");

    // Parameters metamodel --------------------------------------------------------
    public static final Resource Parameterizable = Ontology.resource(NAMESPACE, "Parameterizable");

    public static final Property parameter = Ontology.property(NAMESPACE, "parameter");
    public static final Property labelTemplate = Ontology.property(NAMESPACE, "labelTemplate");

    public static final Resource Parameter = Ontology.resource(NAMESPACE, "Parameter");

    public static final Property optional = Ontology.property(NAMESPACE, "optional");

    // Constraint components metamodel ---------------------------------------------
    public static final Resource ConstraintComponent = Ontology.resource(NAMESPACE, "ConstraintComponent");

    public static final Property validator = Ontology.property(NAMESPACE, "validator");
    public static final Property nodeValidator = Ontology.property(NAMESPACE, "nodeValidator");
    public static final Property propertyValidator = Ontology.property(NAMESPACE, "propertyValidator");

    public static final Resource Validator = Ontology.resource(NAMESPACE, "Validator");
    public static final Resource SPARQLAskValidator = Ontology.resource(NAMESPACE, "SPARQLAskValidator");
    public static final Resource SPARQLSelectValidator = Ontology.resource(NAMESPACE, "SPARQLSelectValidator");

    // Library of Core Constraint Components and their properties ------------------
    public static final Resource AndConstraintComponent = Ontology.resource(NAMESPACE, "AndConstraintComponent");
    public static final Resource AndConstraintComponentAnd = Ontology.resource(NAMESPACE, "AndConstraintComponent-and");

    public static final Property and = Ontology.property(NAMESPACE, "and");

    public static final Resource ClassConstraintComponent  = Ontology.resource(NAMESPACE, "ClassConstraintComponent ");
    public static final Resource ClassConstraintComponentClass = Ontology.resource(NAMESPACE, "ClassConstraintComponent-class");

    public static final Property classProperty = Ontology.property(NAMESPACE, "class");

    public static final Resource ClosedConstraintComponent = Ontology.resource(NAMESPACE, "ClosedConstraintComponent");
    public static final Resource ClosedConstraintComponentClosed = Ontology.resource(NAMESPACE, "ClosedConstraintComponent-closed");
    public static final Resource ClosedConstraintComponentIgnoredProperties = Ontology.resource(NAMESPACE, "ClosedConstraintComponent-ignoredProperties");

    public static final Property closed = Ontology.property(NAMESPACE, "closed");
    public static final Property ignoredProperties = Ontology.property(NAMESPACE, "ignoredProperties");

    public static final Resource DatatypeConstraintComponent = Ontology.resource(NAMESPACE, "DatatypeConstraintComponent");
    public static final Resource DatatypeConstraintComponentDatatype = Ontology.resource(NAMESPACE, "DatatypeConstraintComponent-datatype");

    public static final Property datatype = Ontology.property(NAMESPACE, "datatype");

    public static final Resource DisjointConstraintComponent = Ontology.resource(NAMESPACE, "DisjointConstraintComponent");
    public static final Resource DisjointConstraintComponentDisjoint = Ontology.resource(NAMESPACE, "DisjointConstraintComponent-disjoint");

    public static final Property disjoint = Ontology.property(NAMESPACE, "disjoint");

    public static final Resource EqualsConstraintComponent = Ontology.resource(NAMESPACE, "EqualsConstraintComponent");
    public static final Resource EqualsConstraintComponentEquals = Ontology.resource(NAMESPACE, "EqualsConstraintComponent-equals");

    public static final Property equals = Ontology.property(NAMESPACE, "equals");

    public static final Resource HasValueConstraintComponent = Ontology.resource(NAMESPACE, "HasValueConstraintComponent");
    public static final Resource HasValueConstraintComponentHasValue = Ontology.resource(NAMESPACE, "HasValueConstraintComponent-hasValue");

    public static final Property hasValue = Ontology.property(NAMESPACE, "hasValue");

    public static final Resource InConstraintComponent = Ontology.resource(NAMESPACE, "InConstraintComponent");
    public static final Resource InConstraintComponentIn = Ontology.resource(NAMESPACE, "InConstraintComponent-in");

    public static final Property in = Ontology.property(NAMESPACE, "in");

    public static final Resource LanguageInConstraintComponent = Ontology.resource(NAMESPACE, "LanguageInConstraintComponent");
    public static final Resource LanguageInConstraintComponentLanguageIn = Ontology.resource(NAMESPACE, "LanguageInConstraintComponent-languageIn");

    public static final Property languageIn = Ontology.property(NAMESPACE, "languageIn");

    public static final Resource LessThanConstraintComponent = Ontology.resource(NAMESPACE, "LessThanConstraintComponent");
    public static final Resource LessThanConstraintComponentLessThan = Ontology.resource(NAMESPACE, "LessThanConstraintComponent-lessThan");

    public static final Property lessThan = Ontology.property(NAMESPACE, "lessThan");

    public static final Resource LessThanOrEqualsConstraintComponent = Ontology.resource(NAMESPACE, "LessThanOrEqualsConstraintComponent");
    public static final Resource LessThanOrEqualsConstraintComponentLessThanOrEquals = Ontology.resource(NAMESPACE, "LessThanOrEqualsConstraintComponent-lessThanOrEquals");

    public static final Property lessThanOrEquals = Ontology.property(NAMESPACE, "lessThanOrEquals");

    public static final Resource MaxCountConstraintComponent = Ontology.resource(NAMESPACE, "MaxCountConstraintComponent");
    public static final Resource MaxCountConstraintComponentMaxCount = Ontology.resource(NAMESPACE, "MaxCountConstraintComponent-maxCount");

    public static final Property maxCount = Ontology.property(NAMESPACE, "maxCount");

    public static final Resource MaxExclusiveConstraintComponent = Ontology.resource(NAMESPACE, "MaxExclusiveConstraintComponent");
    public static final Resource MaxExclusiveConstraintComponentMaxExclusive = Ontology.resource(NAMESPACE, "MaxExclusiveConstraintComponent-maxExclusive");

    public static final Property maxExclusive = Ontology.property(NAMESPACE, "maxExclusive");

    public static final Resource MaxInclusiveConstraintComponent = Ontology.resource(NAMESPACE, "MaxInclusiveConstraintComponent");
    public static final Resource MaxInclusiveConstraintComponentMaxInclusive = Ontology.resource(NAMESPACE, "MaxInclusiveConstraintComponent-maxInclusive");

    public static final Property maxInclusive = Ontology.property(NAMESPACE, "maxInclusive");

    public static final Resource MaxLengthConstraintComponent = Ontology.resource(NAMESPACE, "MaxLengthConstraintComponent");
    public static final Resource MaxLengthConstraintComponentMaxLength = Ontology.resource(NAMESPACE, "MaxLengthConstraintComponent-maxLength");

    public static final Property maxLength = Ontology.property(NAMESPACE, "maxLength");

    public static final Resource MinCountConstraintComponent = Ontology.resource(NAMESPACE, "MinCountConstraintComponent");
    public static final Resource MinCountConstraintComponentMinCount = Ontology.resource(NAMESPACE, "MinCountConstraintComponent-minCount");

    public static final Property minCount = Ontology.property(NAMESPACE, "minCount");

    public static final Resource MinExclusiveConstraintComponent = Ontology.resource(NAMESPACE, "MinExclusiveConstraintComponent");
    public static final Resource MinExclusiveConstraintComponentMinExclusive = Ontology.resource(NAMESPACE, "MinExclusiveConstraintComponent-minExclusive");

    public static final Property minExclusive = Ontology.property(NAMESPACE, "minExclusive");

    public static final Resource MinInclusiveConstraintComponent = Ontology.resource(NAMESPACE, "MinInclusiveConstraintComponent");
    public static final Resource MinInclusiveConstraintComponentMinInclusive = Ontology.resource(NAMESPACE, "MinInclusiveConstraintComponent-minInclusive");

    public static final Property minInclusive = Ontology.property(NAMESPACE, "minInclusive");

    public static final Resource MinLengthConstraintComponent = Ontology.resource(NAMESPACE, "MinLengthConstraintComponent");
    public static final Resource MinLengthConstraintComponentMinLength = Ontology.resource(NAMESPACE, "MinLengthConstraintComponent-minLength");

    public static final Property minLength = Ontology.property(NAMESPACE, "minLength");

    public static final Resource NodeConstraintComponent = Ontology.resource(NAMESPACE, "NodeConstraintComponent");
    public static final Resource NodeConstraintComponentNode = Ontology.resource(NAMESPACE, "NodeConstraintComponent-node");

    public static final Property node = Ontology.property(NAMESPACE, "node");

    public static final Resource NodeKindConstraintComponent = Ontology.resource(NAMESPACE, "NodeKindConstraintComponent");
    public static final Resource NodeKindConstraintComponentNodeKind = Ontology.resource(NAMESPACE, "NodeKindConstraintComponent-nodeKind");

    public static final Property nodeKind = Ontology.property(NAMESPACE, "nodeKind");

    public static final Resource NotConstraintComponent = Ontology.resource(NAMESPACE, "NotConstraintComponent");
    public static final Resource NotConstraintComponentNot = Ontology.resource(NAMESPACE, "NotConstraintComponent-not");

    public static final Property not = Ontology.property(NAMESPACE, "not");

    public static final Resource OrConstraintComponent = Ontology.resource(NAMESPACE, "OrConstraintComponent");
    public static final Resource OrConstraintComponentOr = Ontology.resource(NAMESPACE, "OrConstraintComponent-or");

    public static final Property or = Ontology.property(NAMESPACE, "or");

    public static final Resource PatternConstraintComponent = Ontology.resource(NAMESPACE, "PatternConstraintComponent");
    public static final Resource PatternConstraintComponentPattern = Ontology.resource(NAMESPACE, "PatternConstraintComponent-pattern");
    public static final Resource PatternConstraintComponentFlags = Ontology.resource(NAMESPACE, "PatternConstraintComponent-flags");

    public static final Property flags = Ontology.property(NAMESPACE, "flags");
    public static final Property pattern = Ontology.property(NAMESPACE, "pattern");

    public static final Resource PropertyConstraintComponent = Ontology.resource(NAMESPACE, "PropertyConstraintComponent");
    public static final Resource PropertyConstraintComponentProperty = Ontology.resource(NAMESPACE, "PropertyConstraintComponent-property");

    public static final Property property = Ontology.property(NAMESPACE, "property");

    public static final Resource QualifiedMaxCountConstraintComponent = Ontology.resource(NAMESPACE, "QualifiedMaxCountConstraintComponent");
    public static final Resource QualifiedMaxCountConstraintComponentQualifiedMaxCount = Ontology.resource(NAMESPACE, "QualifiedMaxCountConstraintComponent-qualifiedMaxCount");
    public static final Resource QualifiedMaxCountConstraintComponentQualifiedValueShape = Ontology.resource(NAMESPACE, "QualifiedMaxCountConstraintComponent-qualifiedValueShape");
    public static final Resource QualifiedMaxCountConstraintComponentQualifiedValueShapesDisjoint = Ontology.resource(NAMESPACE, "QualifiedMaxCountConstraintComponent-qualifiedValueShapesDisjoint");
    public static final Resource QualifiedMinCountConstraintComponent = Ontology.resource(NAMESPACE, "QualifiedMinCountConstraintComponent");
    public static final Resource QualifiedMinCountConstraintComponentQualifiedMinCount = Ontology.resource(NAMESPACE, "QualifiedMinCountConstraintComponent-qualifiedMinCount");
    public static final Resource QualifiedMinCountConstraintComponentQualifiedValueShape = Ontology.resource(NAMESPACE, "QualifiedMinCountConstraintComponent-qualifiedValueShape");
    public static final Resource QualifiedMinCountConstraintComponentQualifiedValueShapesDisjoint = Ontology.resource(NAMESPACE, "QualifiedMinCountConstraintComponent-qualifiedValueShapesDisjoint");

    public static final Property qualifiedMaxCount = Ontology.property(NAMESPACE, "qualifiedMaxCount");
    public static final Property qualifiedMinCount = Ontology.property(NAMESPACE, "qualifiedMinCount");
    public static final Property qualifiedValueShape = Ontology.property(NAMESPACE, "qualifiedValueShape");
    public static final Property qualifiedValueShapesDisjoint = Ontology.property(NAMESPACE, "qualifiedValueShapesDisjoint");

    public static final Resource UniqueLangConstraintComponent = Ontology.resource(NAMESPACE, "UniqueLangConstraintComponent");
    public static final Resource UniqueLangConstraintComponentUniqueLang = Ontology.resource(NAMESPACE, "UniqueLangConstraintComponent-uniqueLang");

    public static final Property uniqueLang = Ontology.property(NAMESPACE, "uniqueLang");

    public static final Resource XoneConstraintComponent = Ontology.resource(NAMESPACE, "XoneConstraintComponent");
    public static final Resource XoneConstraintComponentXone = Ontology.resource(NAMESPACE, "XoneConstraintComponent-xone");

    public static final Property xone = Ontology.property(NAMESPACE, "xone");

    // General SPARQL execution support --------------------------------------------
    public static final Resource SPARQLExecutable = Ontology.resource(NAMESPACE, "SPARQLExecutable");
    public static final Resource SPARQLAskExecutable = Ontology.resource(NAMESPACE, "SPARQLAskExecutable");

    public static final Property ask = Ontology.property(NAMESPACE, "ask");

    public static final Resource SPARQLConstructExecutable = Ontology.resource(NAMESPACE, "SPARQLConstructExecutable");

    public static final Property construct = Ontology.property(NAMESPACE, "construct");

    public static final Resource SPARQLSelectExecutable = Ontology.resource(NAMESPACE, "SPARQLSelectExecutable");

    public static final Property select = Ontology.property(NAMESPACE, "select");

    public static final Resource SPARQLUpdateExecutable = Ontology.resource(NAMESPACE, "SPARQLUpdateExecutable");

    public static final Property update = Ontology.property(NAMESPACE, "update");
    public static final Property prefixes = Ontology.property(NAMESPACE, "prefixes");

    public static final Resource PrefixDeclaration = Ontology.resource(NAMESPACE, "PrefixDeclaration");

    public static final Property declare = Ontology.property(NAMESPACE, "declare");
    public static final Property prefix = Ontology.property(NAMESPACE, "prefix");
    public static final Property namespace = Ontology.property(NAMESPACE, "namespace");

    // SPARQL-based Constraints support --------------------------------------------
    public static final Resource SPARQLConstraintComponent = Ontology.resource(NAMESPACE, "SPARQLConstraintComponent");
    public static final Resource SPARQLConstraintComponentSparql = Ontology.resource(NAMESPACE, "SPARQLConstraintComponent-sparql");

    public static final Property sparql = Ontology.property(NAMESPACE, "sparql");

    public static final Resource SPARQLConstraint = Ontology.resource(NAMESPACE, "SPARQLConstraint");

    // Non-validating constraint properties ----------------------------------------
    public static final Property defaultValue = Ontology.property(NAMESPACE, "defaultValue");
    public static final Property description = Ontology.property(NAMESPACE, "description");
    public static final Property group = Ontology.property(NAMESPACE, "group");
    public static final Property name = Ontology.property(NAMESPACE, "name");
    public static final Property order = Ontology.property(NAMESPACE, "order");

    public static final Resource PropertyGroup = Ontology.resource(NAMESPACE, "PropertyGroup");

    // -----------------------------------------------------------------------------
    // SHACL ADVANCED FEATURES -----------------------------------------------------
    // -----------------------------------------------------------------------------
    // Advanced Target vocabulary --------------------------------------------------
    public static final Property target = Ontology.property(NAMESPACE, "target");

    public static final Resource Target = Ontology.resource(NAMESPACE, "Target");
    public static final Resource TargetType = Ontology.resource(NAMESPACE, "TargetType");
    public static final Resource SPARQLTarget = Ontology.resource(NAMESPACE, "SPARQLTarget");
    public static final Resource SPARQLTargetType = Ontology.resource(NAMESPACE, "SPARQLTargetType");

    // Functions Vocabulary --------------------------------------------------------
    public static final Resource Function = Ontology.resource(NAMESPACE, "Function");

    public static final Property returnType = Ontology.property(NAMESPACE, "returnType");

    public static final Resource SPARQLFunction = Ontology.resource(NAMESPACE, "SPARQLFunction");

    // Result Annotations ----------------------------------------------------------
    public static final Property resultAnnotation = Ontology.property(NAMESPACE, "resultAnnotation");

    public static final Resource ResultAnnotation = Ontology.resource(NAMESPACE, "ResultAnnotation");

    public static final Property annotationProperty = Ontology.property(NAMESPACE, "annotationProperty");
    public static final Property annotationValue = Ontology.property(NAMESPACE, "annotationValue");
    public static final Property annotationVarName = Ontology.property(NAMESPACE, "annotationVarName");

    // Node Expressions ------------------------------------------------------------
    public static final Property thisProeprty = Ontology.property(NAMESPACE, "this");
    public static final Property filterShape = Ontology.property(NAMESPACE, "filterShape");
    public static final Property nodes = Ontology.property(NAMESPACE, "nodes");
    public static final Property intersection = Ontology.property(NAMESPACE, "intersection");
    public static final Property union = Ontology.property(NAMESPACE, "union");

    // Expression Constraints ------------------------------------------------------
    public static final Resource ExpressionConstraintComponent = Ontology.resource(NAMESPACE, "ExpressionConstraintComponent");
    public static final Resource ExpressionConstraintComponentExpression = Ontology.resource(NAMESPACE, "ExpressionConstraintComponent-expression");

    public static final Property expression = Ontology.property(NAMESPACE, "expression");

    // Rules -----------------------------------------------------------------------
    public static final Resource Rule = Ontology.resource(NAMESPACE, "Rule");

    public static final Property rule = Ontology.property(NAMESPACE, "rule");
    public static final Property condition = Ontology.property(NAMESPACE, "condition");

    public static final Resource TripleRule = Ontology.resource(NAMESPACE, "TripleRule");

    public static final Property subject = Ontology.property(NAMESPACE, "subject");
    public static final Property predicate = Ontology.property(NAMESPACE, "predicate");
    public static final Property object = Ontology.property(NAMESPACE, "object");

    public static final Resource SPARQLRule = Ontology.resource(NAMESPACE, "SPARQLRule");

    // SHACL-JS --------------------------------------------------------------------
    public static final Resource JSExecutable = Ontology.resource(NAMESPACE, "JSExecutable");
    public static final Resource JSTarget = Ontology.resource(NAMESPACE, "JSTarget");
    public static final Resource JSTargetType = Ontology.resource(NAMESPACE, "JSTargetType");
    public static final Resource JSConstraint = Ontology.resource(NAMESPACE, "JSConstraint");
    public static final Resource JSConstraintComponent = Ontology.resource(NAMESPACE, "JSConstraintComponent");
    public static final Resource JSConstraintJs = Ontology.resource(NAMESPACE, "JSConstraint-js");

    public static final Property js = Ontology.property(NAMESPACE, "js");
    public static final Property jsFunctionName = Ontology.property(NAMESPACE, "jsFunctionName");
    public static final Property jsLibrary = Ontology.property(NAMESPACE, "jsLibrary");
    public static final Property jsLibraryURL = Ontology.property(NAMESPACE, "jsLibraryURL");

    public static final Resource JSFunction = Ontology.resource(NAMESPACE, "JSFunction");
    public static final Resource JSLibrary = Ontology.resource(NAMESPACE, "JSLibrary");
    public static final Resource JSRule = Ontology.resource(NAMESPACE, "JSRule");
    public static final Resource JSValidator = Ontology.resource(NAMESPACE, "JSValidator");
}
