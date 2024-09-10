<template>
    <b-form>

        <opensilex-InputForm
            :value.sync="rdf_type"
            label="component.common.type"
            type="text"
            :disabled="true"
        ></opensilex-InputForm>

        <!-- Parent -->
        <opensilex-FormSelector
            :selected.sync="form.property"
            :options="propertiesOptions"
            :required="true"
            label="OntologyClassPropertyForm.property"
            helpMessage="OntologyClassPropertyForm.property-help"
            @update:selected="updateIsListProperty"
        ></opensilex-FormSelector>


        <!-- is_required -->
        <opensilex-FormField
            :required="true"
            label="OntologyClassDetail.required"
            helpMessage="OntologyClassPropertyForm.required-help"
        >
            <template v-slot:field="field">
                <b-form-checkbox
                    v-model="form.is_required" switch
                ></b-form-checkbox>
            </template>
        </opensilex-FormField>

        <!-- is_list -->
        <opensilex-FormField
            :required="true"
            label="OntologyClassDetail.list"
            helpMessage="OntologyClassPropertyForm.is-list-help"
        >
            <template v-slot:field="field">
                <b-form-checkbox
                    :disabled="dataTypeProperties.indexOf(form.property) >= 0"
                    v-model="form.is_list" switch
                ></b-form-checkbox>
            </template>
        </opensilex-FormField>

    </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import { ResourceTreeDTO } from 'opensilex-core/index';

@Component
export default class OntologyClassPropertyForm extends Vue {
    $opensilex: any;
    OWL = OWL;

    @Prop()
    editMode;

    @Prop({
        default: () => {
            return {
                property: null,
                is_required: false,
                is_list: false,
            };
        },
    })
    form;

    disableIsListCheckBox() : boolean {
        let dataPropIdx = this.dataTypeProperties.indexOf(this.form.property);
        return dataPropIdx >= 0;
    }

    getEmptyForm() {
        return {
            property: null,
            is_required: false,
            is_list: false,
        };
    }

    availableProperties = null;
    dataTypeProperties = [];

    setProperties(properties: ResourceTreeDTO[]) {
        this.availableProperties = properties;

        this.dataTypeProperties = [];
        this.availableProperties.forEach((prop) => {
            if (prop.rdf_type == "owl:DatatypeProperty") {
                this.dataTypeProperties.push(prop.uri);
            }
        });
    }

    rdf_type = null;

    setClassURI(rdf_type) {
        this.rdf_type = rdf_type;
    }

    domain = null;

    setDomain(domain) {
        this.domain = domain;
    }

    get propertiesOptions() {
        return this.buildTreeListOptions(
            this.availableProperties,
            []
        );
    }

    updateIsListProperty() {
        if (!this.form.property || !this.dataTypeProperties) {
            return;
        }

        // if the property is a data property then set is_list to false, since we don't actually handle generics list component for data-property
        if(this.isDataProperty(this.form.property)){
            this.form.is_list = false;
        }
    }

    create(form) {
        let propertyForm = {
            rdf_type: this.rdf_type,
            property: form.property,
            required: form.is_required,
            list: form.is_list,
            domain: this.domain
        };

        return this.$opensilex
            .getService("opensilex.OntologyService")
            .addClassPropertyRestriction(propertyForm)
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                let msg = this.$i18n.t("OntologyClassPropertyForm.link-success-msg", [form.property, form.rdf_type]).toString();
                this.$opensilex.showSuccessToast(msg);
            })
            .catch(this.$opensilex.errorHandler);
    }

    update(form) {
        let propertyForm = {
            rdf_type: this.rdf_type,
            property: form.property,
            required: form.is_required,
            list: form.is_list,
            domain: this.domain
        };

        return this.$opensilex
            .getService("opensilex.OntologyService")
            .updateClassPropertyRestriction(propertyForm)
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                let msg = this.$i18n.t("OntologyClassPropertyForm.link-success-msg", form.property, form.rdf_type).toString();
                this.$opensilex.showSuccessToast(msg);
            })
            .catch(this.$opensilex.errorHandler);
    }

    buildTreeListOptions(resourceTrees: Array<any>, excludeProperties) {
        let options = [];

        if (resourceTrees != null) {
            resourceTrees.forEach((resourceTree: any) => {
                let subOption = this.buildTreeOptions(resourceTree, excludeProperties);
                options.push(subOption);
            });
        }

        return options;
    }

    isDataProperty(property: string): boolean{
        return this.dataTypeProperties.indexOf(property) >= 0;
    }

    buildTreeOptions(resourceTree: any, excludeProperties: Array<string>) {

        let isDataProperty = this.isDataProperty(resourceTree.uri);
        let propertyType = isDataProperty ?
            this.$t("OntologyPropertyForm.dataProperty") :
            this.$t("OntologyPropertyForm.objectProperty");

        let option = {
            id: resourceTree.uri,
            label: resourceTree.name + " (" + propertyType + ")",
            isDefaultExpanded: true,
            isDisabled: excludeProperties.indexOf(resourceTree.uri) >= 0,
            children: [],
        };

        resourceTree.children.forEach((child) => {
            let subOption = this.buildTreeOptions(child, excludeProperties);
            option.children.push(subOption);
        });

        if (resourceTree.disabled) {
            option.isDisabled = true;
        }

        if (option.children.length == 0) {
            delete option.children;
        }

        return option;
    }

}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    OntologyClassPropertyForm:
        property: Property
        link-success-msg: 'The property {0} has been added to {0} type'
        property-help: Select the property to associate to the type. Only properties which are not already associated, are selectable.
        required-help: Check this checkbox to make this property required for the selected type.
        is-list-help: Check this checkbox in order to use multiple values. Currently only object-properties are supported.
fr:
    OntologyClassPropertyForm:
        property: Propriété
        link-success-msg: 'La propriété {0} a été ajoutée au type {1}'
        property-help: 'Selectionner la propriété à associer au type. Seul les propriétés qui ne sont pas déjà associées, sont sélectionnables.'
        required-help: Cocher cette case pour rendre cette propriété obligatoire pour le type selectionné
        is-list-help: 'Cocher cette case pour pouvoir utiliser une liste de valeurs. Seul les propriétés "objets" sont supportés.'


</i18n>