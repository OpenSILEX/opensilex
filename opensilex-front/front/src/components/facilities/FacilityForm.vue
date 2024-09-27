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

    <!-- Type -->
    <opensilex-TypeForm
        v-if="baseType"
        :type.sync="form.rdf_type"
        :baseType="baseType"
        :ignoreRoot="false"
        :required="true"
        :disabled="editMode"
        placeholder="OntologyObjectForm.form-type-placeholder"
        @select="typeSwitch($event.id, false)"
    ></opensilex-TypeForm>

    <!-- Name -->
    <opensilex-InputForm
        :value.sync="form.name"
        label="component.common.name"
        type="text"
        :required="true"
        placeholder="OntologyObjectForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Description -->
    <opensilex-TextAreaForm
            :value.sync="form.description"
            label="component.common.description"
            placeholder="component.common.description"
            @keydown.native.enter.stop
    >
    </opensilex-TextAreaForm>

    <!-- Group of variables -->
    <opensilex-GroupVariablesSelector
            label="VariableView.groupVariable"
            :variableGroup.sync="form.variableGroups"
            :multiple="true"
    >
    </opensilex-GroupVariablesSelector>

<!-- TODO: ajoute "name" ?-->
    <!-- Custom properties -->
    <opensilex-OntologyRelationsForm
            ref="ontologyRelationsForm"
            :rdfType="this.form.rdf_type"
            :relations="this.form.relations"
            :baseType="this.baseType"
            :editMode="editMode"
    ></opensilex-OntologyRelationsForm>
    <slot v-if="form.rdf_type" v-bind:form="form"></slot>

    <!-- Organizations -->
    <opensilex-OrganizationSelector
        label="component.experiment.organizations"
        :organizations.sync="form.organizations"
        :multiple="true"
    ></opensilex-OrganizationSelector>

    <!-- Site -->
    <opensilex-SiteSelector
        label="component.common.organization.site"
        :multiple="true"
        :sites.sync="form.sites"
    >
    </opensilex-SiteSelector>

    <!-- Warning iff more than one site is associated to the facility. While it is currently accepted in the model,
     we don't currently have any use cases requiring a single facility to belong to multiple sites. This should change
     in the future. -->
    <b-alert
            v-if="Array.isArray(form.sites) && form.sites.length > 1"
            variant="warning"
            show
    >
      {{$t("component.facility.warning.facility-should-have-unique-site")}}
    </b-alert>

    <!-- Address toggle -->
    <b-form-checkbox
            v-model="hasAddress"
            :value="true"
            :unchecked-value="false"
            @change="onAddressToggled"
            switches
    >{{$t("FacilityForm.toggleAddress")}}</b-form-checkbox>

    <!-- Address -->
    <opensilex-AddressForm
            :address.sync="form.address"
    >
    </opensilex-AddressForm>

    <!-- POSITION -->
    <br/>
    <p>
      <b>Positions</b>
    </p>
    <hr/>

    <div class="row">
      <div class="col">
        <!-- Geometry -->
        <opensilex-GeometryForm
                :value.sync="form.geometry"
                label="component.common.geometry"
                helpMessage="component.common.geometry-help"
        >
        </opensilex-GeometryForm>
      </div>
      <!-- Add position - only for update -->
            <div class="col-2">
              <opensilex-AddChildButton
                      v-if="editMode"
                      label="FacilityForm.add-position"
                      :small="true"
              ></opensilex-AddChildButton>
            </div>
    </div>

    <!-- Dates -->
    <div class="row">
      <div class="col">
        <opensilex-DateTimeForm
                :value.sync="form.date"
                label="Event.start"
                :maxDate="form.end"
                :required="true"
        ></opensilex-DateTimeForm>
      </div>
      <div class="col">
        <opensilex-DateTimeForm
                :value.sync="form.endDate"
                label="Event.end"
                :minDate="form.date"
                :required="false"
        ></opensilex-DateTimeForm>
      </div>
    </div>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService} from "../../lib";
import { FacilityCreationDTO } from 'opensilex-core/index';
import OntologyRelationsForm from "../ontology/OntologyRelationsForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class FacilityForm extends Vue {
  //#region Plugins and services
  $opensilex: OpenSilexVuePlugin;
  ontologyService: OntologyService;
  vueOntologyService: VueJsOntologyExtensionService;
  //endregion

  //#region Props
  @Prop({default: false})
  private readonly editMode: boolean;

  @Prop({default: FacilityForm.getEmptyForm()})
  private readonly form: FacilityCreationDTO;
  //endregion

  //#region Refs
  @Ref("validatorRef")
  private readonly validatorRef!: any;
  @Ref("ontologyRelationsForm")
  private readonly ontologyRelationsForm: OntologyRelationsForm;
  //endregion

  //#region Data
  private uriGenerated = true;
  private hasAddress: boolean;
  private baseType: string;
  private typeModel = null;
  private propertyComponents = [];
  //endregion

  //#region Computed
  @Watch("form")
  onFacilityChanged() {
    // Update hasAddress checkbox
    this.hasAddress = !!this.form.address;
    // Reset the type model
    this.resetTypeModel();
  }
  //endregion

  //#region Events
  //endregion

  //#region Events handlers
  private onAddressToggled() {
    this.form.address = this.hasAddress
            ? {}
            : undefined;
  }
  //endregion

  //#region Public methods
  public setBaseType(baseType: string) {
    this.baseType = baseType;
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
      geometry: undefined,
      address: undefined,
      organizations: [],
      sites: [],
      variableGroups: [],
      relations: [],
      date: undefined,
      endDate: undefined
    };
  }
  //endregion

  //#region Hooks
  private created() {
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
    this.baseType = this.$opensilex.Oeso.FACILITY_TYPE_URI;
    this.hasAddress = !!this.form.address;
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

  private resetTypeModel(){
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
    toggleAddress: "Address"
fr:
  FacilityForm:
    toggleAddress: "Adresse"
</i18n>