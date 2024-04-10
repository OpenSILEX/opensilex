<template>
    <b-form>
        <!-- URI -->
        <opensilex-UriForm
            :uri.sync="form.uri"
            label="OntologyObjectForm.uri-label"
            helpMessage="component.common.uri-help-message"
            :editMode="editMode"
            :generated.sync="uriGenerated"
        ></opensilex-UriForm>

        <!-- Name -->
        <opensilex-InputForm
            :value.sync="form.name"
            label="component.common.name"
            type="text"
            :required="true"
            placeholder="OntologyObjectForm.form-name-placeholder"
        ></opensilex-InputForm>

        <!-- Type -->
        <opensilex-TypeForm
            v-if="baseType"
            :type.sync="form.rdf_type"
            :baseType="baseType"
            :required="true"
            :disabled="editMode"
            :ignoreRoot="false"
            placeholder="OntologyObjectForm.form-type-placeholder"
            @select="typeSwitch($event.id,false)"
        ></opensilex-TypeForm>

        <!-- Custom properties -->
        <opensilex-OntologyRelationsForm
            v-if="baseType && loadCustomProperties"
            ref="ontologyRelationsForm"
            :rdfType="this.form.rdf_type"
            :relations="this.form.relations"
            :excludedProperties="this.excludedProperties"
            :customComponentProps="this.customComponentProps"
            :baseType="this.baseType"
            :editMode="editMode"
            :context="context ? { experimentURI: context} : undefined"
            :initHandler="this.initHandler"
        ></opensilex-OntologyRelationsForm>

        <slot v-if="form.rdf_type" v-bind:form="form"></slot>
    </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OntologyRelationsForm from "./OntologyRelationsForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {MultiValuedRDFObjectRelation} from "./models/MultiValuedRDFObjectRelation";
import Rdfs from "../../ontologies/Rdfs";

@Component
/**
 * Component used for handling URI, type, name and custom properties for a given type
 */
export default class OntologyObjectForm extends Vue {

    $opensilex: OpenSilexVuePlugin;
    uriGenerated = true;

    @Ref("ontologyRelationsForm") readonly ontologyRelationsForm!: OntologyRelationsForm;

    @Prop()
    editMode;

    excludedProperties = new Set<string>([Rdfs.getShortURI(Rdfs.LABEL)]);

    customComponentProps = new Map<string, Map<string, any>>();

    @Prop({
        default: () => {
            return {
                uri: null,
                rdf_type: null,
                name: "",
                relations: []
            };
        }
    })
    form;

    reset() {
        this.uriGenerated = true;
    }

    getEmptyForm() {
        return {
            uri: null,
            rdf_type: null,
            name: "",
            relations: []
        };
    }

    setBaseType(type, parentType) {
        this.baseType = parentType;
        // ontologyRelationsForm exists only after baseType has been set, so we wait the next tick
        this.$nextTick(() => {
            if (this.ontologyRelationsForm) {
              this.ontologyRelationsForm.typeSwitch(type, true);
            }
        });
    }

    context: string = "";

    setContext(context) {
        this.context = context;
        return this;
    }

    initHandler = (relation: MultiValuedRDFObjectRelation) => {};

    setInitHandler(handler) {
        this.initHandler = handler;
    }

    updateRelations() {
      this.ontologyRelationsForm.updateRelation(null, null);
    }

    propertyFilter = property => property;

    setTypePropertyFilterHandler(handler) {
        this.propertyFilter = handler;
    }

    baseType = null;

    async typeSwitch(type: string, initialLoad: boolean) {
        if(this.ontologyRelationsForm){
          await this.ontologyRelationsForm.typeSwitch(type, initialLoad);
        }
    }

    setExcludedProperties(excludedProperties: Set<string>) {
        this.excludedProperties = excludedProperties;
    }

    setCustomComponentProps(customComponentProps: Map<string, Map<string, any>>){
        this.customComponentProps = customComponentProps;
    }

    loadCustomProperties: boolean = true;

    setLoadCustomProperties(loadCustomProperties: boolean){
        this.loadCustomProperties = loadCustomProperties;
    }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    OntologyObjectForm:
        uri-label: Object URI
        form-name-placeholder: Enter object name
        form-type-placeholder: Select object type

fr:
    OntologyObjectForm:
        uri-label: URI de l'objet
        form-name-placeholder: Saisir le nom de l'objet
        form-type-placeholder: Sélectionner le type de l'objet

</i18n>


