<template>
    <div>
        <div v-for="(relation, index) in typeRelations" v-bind:key="index">

            <!--
                Dynamic component(s) rending according internal relations
                Additional prop can be added to these component by using v-bind
            -->
            <component
                :is="getInputComponent(relation.property)"
                :property="relation.property"
                :label="relation.property.name"
                :required="relation.property.is_required"
                :multiple="relation.property.is_list"
                :value.sync="relation.value"
                :context="context"
                @update:value="updateRelation($event,relation.property)"
                v-bind="getCustomPropsForComponent(relation.property.uri)"
            ></component>
        </div>
    </div>

</template>

<script lang="ts">

import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import {VueJsOntologyExtensionService, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../lib";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {MultiValuedRDFObjectRelation} from "./models/MultiValuedRDFObjectRelation";

@Component
/**
 * Component used for handling all custom properties in any form
 */
export default class OntologyRelationsForm extends Vue {

    $opensilex: OpenSilexVuePlugin;
    vueOntologyService: VueJsOntologyExtensionService;

    /**
     * Initial relations, multi-valued are decomposed into multiple mono-valued relation.
     * These relations are always sync with internal relations, in order to ensure that
     * updates from component (on mono or multi-valued relation) are always effective on
     * prop.
     */
    @Prop()
    relations: Array<RDFObjectRelationDTO>;

    /*
    * Internal relations used and updated by component.
    */
    internalRelations: Array<MultiValuedRDFObjectRelation> = [];

    /**
     * The specific type of a model. Must be a subClassOf/equals of baseType
     */
    @Prop()
    rdfType: string;

    /**
     * The root type URI, change according upper-component which use this component. (E.g. event, os, device)
     */
    @Prop()
    baseType: string;

    /**
     * The set of property URI to exclude from custom properties handling.
     * This property can be set, when some property are handled in code/hard.
     *
     * @description You can pass a short or full URI for properties, this has no effect since this component take care of this.
     */
    @Prop()
    excludedProperties: Set<string>;

    /**
     * Type definition, contains data/properties and associated input/view component.
     * This model is changed after each call to {@link this.typeSwitch()}
     */
    typeModel: VueRDFTypeDTO = null;

    /**
     * The model graph/context. Can be passed in some custom component has a "context" property
     */
    @Prop()
    context: { experimentURI: string };


    /**
     * Function which can be applied on startup and on each type update.
     * This function can be used when some specific logic on {@link internalRelations} need to be applied
     *
     * @param relation : relation object (property + value)
     */
    @Prop({
        type: Function,
        default: function (relation) {}
    })
    initHandler: { (relation: MultiValuedRDFObjectRelation): void };

    /**
     * Map used to pass some custom properties to some component associated to a custom property.
     * Map key correspond to component id
     *
     * Map value correspond to :
     *      - key : VueJS component prop associated to component
     *      - value : VueJS component prop value
     */
    @Prop({default: () => new Map<string, Map<string, any>>()})
    customComponentProps: Map<string, Map<string, any>>;

    created() {
        this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
        this.internalRelations = [];
    }

    get typeRelations(): Array<MultiValuedRDFObjectRelation> {

        if (!this.typeModel) {
            return [];
        }
        return this.internalRelations;
    }

    /**
     * @return the sorted list of VueRDFTypePropertyDTO which must be handled by this component.
     * It include all data and object properties from the {@link typeModel} minus properties from {@link excludedProperties}
     */
    getHandledProperties(): Array<VueRDFTypePropertyDTO> {

        // compute short form of properties in order to compare properties URI coming from service with URI from excludedProperties
        // this ensure URI equality according registered namespaces into OpenSilexVuePlugin
        let shortExcludedProperties = new Set<string>();
        this.excludedProperties.forEach(property => shortExcludedProperties.add(this.$opensilex.getShortUri(property)))

        let properties =  this.typeModel.data_properties
            .concat(this.typeModel.object_properties)
            .filter(property => ! shortExcludedProperties.has(this.$opensilex.getShortUri(property.uri)));

        return this.sortProperties(properties);
    }

    /**
     * @return an object with has custom prop as keys and custom value as values.
     * Keys/Values are retrieved from {@link customComponentProps} if a key matching with property is found,
     * else an empty object is returned.
     *
     * @param property URI of the property
     */
    getCustomPropsForComponent(property: string): any {
        if (!this.customComponentProps || !this.customComponentProps.has(property)) {
            return {};
        }
        let customAttributes = this.customComponentProps.get(property);
        if (!customAttributes || customAttributes.size == 0) {
            return {};
        }
        return Object.fromEntries(customAttributes);
    }

    /**
     * Update {@link internalRelations} when the type change.
     * A call to the {@link VueJsOntologyExtensionService#getRDFTypeProperties} API is done in order to retrieve class definition
     * for the given type.
     * @param type type URI
     * @param initialLoad if true, then {@link internalRelations} are initialized according {@link relations}
     *
     */
    typeSwitch(type: string, initialLoad: boolean) {

        // only in create mode, since in update mode, the type can't be changed
        if (!type || type.length == 0) {
            return;
        }

        return this.vueOntologyService
            .getRDFTypeProperties(type, this.baseType)
            .then(http => {
                this.typeModel = http.response.result;
                this.internalRelations.splice(0);

                if (initialLoad) {
                    this.internalRelations.push(...this.toMultiValuedRelations(this.relations));
                } else {
                    this.internalRelations.push(...this.getHandledProperties().map(property => {
                        return {
                            property: property,
                            value: property.is_list ? [] : undefined
                        }
                    }));
                }

                if (this.initHandler) {
                    this.internalRelations.forEach(relation => {
                        this.initHandler.apply(this, [relation]);
                    });
                }


                // #TODO sort properties according typeModel order
            }).catch(this.$opensilex.errorHandler);
    }

    /**
     * @param properties properties to sort according {@link typeModel} {@link VueRDFTypeDTO#properties_order}
     * @return sorted properties if {@link typeModel} has a non null or empty {@link VueRDFTypeDTO#properties_order}, return properties else
     */
    sortProperties(properties: Array<VueRDFTypePropertyDTO>): Array<VueRDFTypePropertyDTO>{

        if(! this.typeModel.properties_order || this.typeModel.properties_order.length == 0){
            return properties;
        }

        return properties.sort((propModel1, propModel2) => {
            let property1 = propModel1.uri;
            let property2 = propModel2.uri;
            if (property1 == property2) {
                return 0;
            }

            // always put name (rdfs:label) in first
            if (this.$opensilex.checkURIs(property1, this.$opensilex.Rdfs.LABEL)) {
                return -1;
            }

            if (this.$opensilex.checkURIs(property2, this.$opensilex.Rdfs.LABEL)) {
                return 1;
            }

            let aIndex = this.typeModel.properties_order.indexOf(property1);
            let bIndex = this.typeModel.properties_order.indexOf(property2);
            if (aIndex == -1) {
                if (bIndex == -1) {
                    return property1.localeCompare(property2);
                } else {
                    return -1;
                }
            } else {
                if (bIndex == -1) {
                    return 1;
                } else {
                    return aIndex - bIndex;
                }
            }
        });
    }

    /**
     * @param property property description with association vue-js information
     * @return the vue component associated with property
     */
    getInputComponent(property: VueRDFTypePropertyDTO) {
        if (property.input_components_by_property && property.input_components_by_property[property.uri]) {
            return property.input_components_by_property[property.uri];
        }
        return property.input_component;
    }

    updateRelation(newValue: string | Array<string>, property: VueRDFTypePropertyDTO) {

        // sync input relations (mono-valued) to internal relations(mono or multivalued, grouped by property)
        this.relations.splice(0);
        this.relations.push(...OntologyRelationsForm.toMultipleMonoValuedRelations(this.internalRelations))
    }

    /**
     *
     * @param relations
     */
    static toMultipleMonoValuedRelations(relations: Array<MultiValuedRDFObjectRelation>): Array<RDFObjectRelationDTO> {

        let newRelations: Array<RDFObjectRelationDTO> = [];

        relations
            .filter(relation => relation.value)  // remove relation with no value
            .forEach(relation => {

                // if the relation is multi-valued then decompose it into multiple mono-valued relation
                // since the service is waiting for an array of mono-valued relation

                if (Array.isArray(relation.value)) {
                    relation.value.forEach(aValue => {
                        newRelations.push({
                            property: relation.property.uri,
                            value: aValue,
                        });
                    })
                } else { // just copy the value
                    newRelations.push({
                        property: relation.property.uri,
                        value: relation.value,
                    });
                }
            })

        return newRelations;
    }

    /**
     * Return an Array of mono or multi-valued relation. If there are several RDFObjectRelationDTO with the same property (multi-valued),
     * then all values are grouped by property.
     * @param relations an Array of mono-valued (one value per property) relation
     */
    toMultiValuedRelations(relations: Array<RDFObjectRelationDTO>): Array<MultiValuedRDFObjectRelation> {

        let valueByProperties = new Map<string, MultiValuedRDFObjectRelation>();

        // read each mono-valued relations and convert them into multi-valued relations (group by property)
        relations.forEach(relation => {

            // get data or object property model
            let propertyDto = this.typeModel.object_properties.find(propertyModel => propertyModel.uri == relation.property)
            if (!propertyDto) {
                propertyDto = this.typeModel.data_properties.find(propertyModel => propertyModel.uri == relation.property)
            }

            // multi-valued relation -> use array of values
            if (propertyDto.is_list) {

                // create the multi-valued relation is not already defined into map
                if (!valueByProperties.has(propertyDto.uri)) {

                    valueByProperties.set(propertyDto.uri, {
                        property: propertyDto,
                        value: [relation.value]
                    })
                } else {
                    // append value into array
                    let values = valueByProperties.get(propertyDto.uri).value as Array<string>;
                    values.push(relation.value);
                }

            } else { // mono-valued relation
                valueByProperties.set(propertyDto.uri, {
                    property: propertyDto,
                    value: relation.value
                })
            }
        })

        // init empty relations, those not present from initial relations
        let emptyRelations: Array<MultiValuedRDFObjectRelation> = this.getHandledProperties()
            .filter(property => !valueByProperties.has(property.uri))
            .map(property => {
                return {
                    property: property,
                    value: property.is_list ? [] : undefined
                }
            })

        // return relations from initial relations (filled) + relations with no value (property/component set)
        return Array.from(valueByProperties.values()).concat(emptyRelations);
    }
}
</script>

<style scoped>

</style>