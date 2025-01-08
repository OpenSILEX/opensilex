import Ontology from "../ontologies/Skos";

export type SkosRelationDefinition = {
    uri: string,
    dtoKey: string,
    label: string,
    description: string
}

// TODO: get relations from web service ?
export const EXACT_MATCH: SkosRelationDefinition = {
    uri: Ontology.EXACT_MATCH,
    dtoKey: "exact_match",
    label: "component.skos.exactMatch",
    description: "component.skos.description.exactMatch"
};
export const CLOSE_MATCH: SkosRelationDefinition = {
    uri: Ontology.CLOSE_MATCH,
    dtoKey: "close_match",
    label: "component.skos.closeMatch",
    description: "component.skos.description.closeMatch"
};
export const BROAD_MATCH: SkosRelationDefinition = {
    uri: Ontology.BROAD_MATCH,
    dtoKey: "broad_match",
    label: "component.skos.broadMatch",
    description: "component.skos.description.broadMatch"
};
export const NARROW_MATCH: SkosRelationDefinition = {
    uri: Ontology.NARROW_MATCH,
    dtoKey: "narrow_match",
    label: "component.skos.narrowMatch",
    description: "component.skos.description.narrowMatch"
};

const SUPPORTED_SKOS_RELATIONS = new Set([
    EXACT_MATCH,
    CLOSE_MATCH,
    BROAD_MATCH,
    NARROW_MATCH
]);

export interface UriSkosRelation {
    uri: string,
    relationDtoKey: string
}

export default SUPPORTED_SKOS_RELATIONS;
