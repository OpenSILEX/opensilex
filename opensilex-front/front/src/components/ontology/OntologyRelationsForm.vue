<template>
    <div>
        <div v-for="(relation, index) in typeRelations" v-bind:key="index">
            <component
                :is="getInputComponent(relation.property)"
                :property="relation.property"
                :label="relation.property.name"
                :required="relation.property.is_required"
                :multiple="relation.property.is_list"
                :value.sync="relation.value"
                @update:value="updateRelation($event,relation.property)"
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
     * The root type, change according upper-component which use this component. (E.g. event, os, device)
     */
    @Prop()
    baseType: string;

    /**
     * The set of property URI to exclude from custom properties handling.
     * This property can be set, when some property are handled in code/hard.
     */
    @Prop()
    excludedProperties: Set<string>;

    /**
     * Type definition, contains data/properties and associated input/view component.
     * This model is changed after each call to this.typeSwitch()
     */
    typeModel: VueRDFTypeDTO = null;

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

    getFilteredProperties(): Array<VueRDFTypePropertyDTO> {
        return this.typeModel.data_properties
            .concat(this.typeModel.object_properties)
            .filter(property => !this.excludedProperties.has(property.uri));
    }

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
                    this.internalRelations.push(...this.getFilteredProperties().map(property => {
                        return {
                            property: property,
                            value: property.is_list ? [] : undefined
                        }
                    }));
                }
            });
    }

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
        let emptyRelations: Array<MultiValuedRDFObjectRelation> = this.getFilteredProperties()
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