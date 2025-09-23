<template>
    <ValidationObserver ref="validatorRef">
        <!-- URI -->
        <opensilex-UriForm
                :uri.sync="facility.uri"
                label="OntologyObjectForm.uri-label"
                helpMessage="component.common.uri-help-message"
                :editMode="editMode"
                :generated.sync="uriGenerated"
        ></opensilex-UriForm>

        <!-- Type -->
        <opensilex-TypeForm
                v-if="baseType"
                :type.sync="facility.rdf_type"
                :baseType="baseType"
                :ignoreRoot="false"
                :required="true"
                :disabled="editMode"
                placeholder="OntologyObjectForm.form-type-placeholder"
                @select="typeSwitch($event.id, false)"
        ></opensilex-TypeForm>

        <!-- Name -->
        <opensilex-InputForm
                :value.sync="facility.name"
                label="component.common.name"
                type="text"
                :required="true"
                placeholder="OntologyObjectForm.form-name-placeholder"
        ></opensilex-InputForm>

        <!-- Description -->
        <opensilex-TextAreaForm
                :value.sync="facility.description"
                label="component.common.description"
                placeholder="component.common.description"
                @keydown.native.enter.stop
        >
        </opensilex-TextAreaForm>

        <!-- Group of variables -->
        <opensilex-GroupVariablesSelector
                label="VariableView.groupVariable"
                :variableGroup.sync="facility.variableGroups"
                :multiple="true"
        >
        </opensilex-GroupVariablesSelector>

        <!-- Custom properties -->
        <opensilex-OntologyRelationsForm
                ref="ontologyRelationsForm"
                :rdfType="this.facility.rdf_type"
                :relations="this.facility.relations"
                :baseType="this.baseType"
                :editMode="editMode"
        ></opensilex-OntologyRelationsForm>
        <slot v-if="facility.rdf_type" v-bind:form="facility"></slot>

        <!-- Organizations -->
        <opensilex-OrganizationSelector
                label="component.experiment.organizations"
                :organizations.sync="facility.organizations"
                :multiple="true"
        ></opensilex-OrganizationSelector>

        <!-- Site -->
        <opensilex-SiteSelector
                label="component.common.organization.site"
                :multiple="true"
                :sites.sync="facility.sites"
        >
        </opensilex-SiteSelector>

        <!-- Warning iff more than one site is associated to the facility. While it is currently accepted in the model,
         we don't currently have any use cases requiring a single facility to belong to multiple sites. This should change
         in the future. -->
        <b-alert
                v-if="Array.isArray(facility.sites) && facility.sites.length > 1"
                variant="warning"
                show
        >
            {{ $t("component.facility.warning.facility-should-have-unique-site") }}
        </b-alert>

        <!-- Address toggle -->
        <b-form-checkbox
                v-model="hasAddress"
                :value="true"
                :unchecked-value="false"
                @change="onAddressToggled"
                switches
        >{{ $t("FacilityForm.toggleAddress") }}
        </b-form-checkbox>

        <!-- Address -->
        <opensilex-AddressForm
                :address.sync="facility.address"
        >
        </opensilex-AddressForm>
        <br>
    </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService} from "../../lib";
import {FacilityCreationDTO} from 'opensilex-core/index';
import OntologyRelationsForm from "../ontology/OntologyRelationsForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class FacilityForm extends Vue {
    //#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin;
    private ontologyService: OntologyService;
    private vueOntologyService: VueJsOntologyExtensionService;
    //endregion

    //#region Props
    @Prop({default: false})
    private readonly editMode: boolean;
    @Prop({default: true})
    private readonly uriGenerated: boolean;
    @PropSync("form", {default: FacilityForm.getEmptyForm()})
    private readonly facility: FacilityCreationDTO;
    //endregion

    //#region Refs
    @Ref("validatorRef")
    private readonly validatorRef!: any;
    @Ref("ontologyRelationsForm")
    private readonly ontologyRelationsForm: OntologyRelationsForm;
    //endregion

    //#region Data
    private hasAddress: boolean;
    private baseType: string;
    private typeModel = null;
    private propertyComponents = [];

    //#region Computed
    @Watch("facility")
    onFacilityChanged() {
        // Update hasAddress checkbox
        this.hasAddress = !!this.facility.address;
        // Reset the type model
        this.resetTypeModel();
    }
    //endregion

    //#region Events
    //endregion

    //#region Events handlers
    private onAddressToggled() {
        this.facility.address = this.hasAddress
                ? {}
                : undefined;
    }
    //endregion

    //#region Public methods
    public reset() {
        this.validatorRef.reset();
    }

    public validate() {
        return this.validatorRef.validate();
    }

    public typeSwitch(type: string, initialLoad: boolean) {
        if (this.ontologyRelationsForm) {
            this.ontologyRelationsForm.typeSwitch(type, initialLoad);
        }
    }

    public getEmptyForm() {
        return FacilityForm.getEmptyForm();
    }

    public static getEmptyForm(): FacilityCreationDTO {
        return {
            uri: undefined,
            rdf_type: undefined,
            name: undefined,
            description: undefined,
            address: undefined,
            organizations: [],
            sites: [],
            variableGroups: [],
            relations: [],
            locations: []
        };
    }
    //endregion

    //#region Hooks
    private created() {
        this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
        this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
        this.baseType = this.$opensilex.Oeso.FACILITY_TYPE_URI;
        this.hasAddress = !!this.facility.address;
    }
    //endregion

    //#region Private methods

    // Manage dynamic fields depending on the type
    // For now, there is no concrete difference between types
    // But might be useful in the future
    // taken from EventFrom.vue
    private getInputComponent(property) {
        if (property.input_components_by_property && property.input_components_by_property[property.property]) {
            return property.input_components_by_property[property.property];
        }
        return property.input_component;
    }

    private resetTypeModel() {
        this.typeModel = undefined;
    }
    //endregion
}
</script>

<style scoped>

</style>

<i18n>
en:
    FacilityForm:
        toggleAddress: Address
fr:
    FacilityForm:
        toggleAddress: Adresse
</i18n>