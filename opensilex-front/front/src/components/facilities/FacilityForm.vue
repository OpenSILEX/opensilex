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

    <!-- POSITIONS -->
    <br/>
    <p>
      <b>Positions</b>
    </p>
    <hr/>

    <div class="row">
      <!-- Add position -->
      <div class="col-4">
        <opensilex-AddChildButton
            @click="addPosition"
            label="FacilityForm.add-position"
            :small="true"
        ></opensilex-AddChildButton>
        <span> {{ $t('FacilityForm.add-position') }}</span>
      </div>

      <!-- Geometry -->
      <div class="col-8">
        <opensilex-GeometryForm
            :value.sync="position.geojson"
            label="component.common.geometry"
            helpMessage="component.common.geometry-help"
        >
        </opensilex-GeometryForm>
      </div>
    </div>

    <!-- Dates -->
    <div class="row">
      <div class="col">
        <opensilex-DateTimeForm
            :value.sync="position.startDate"
            label="component.common.begin"
            :maxDate="position.endDate"
            :required="false"
        ></opensilex-DateTimeForm>
      </div>
      <div class="col">
        <opensilex-DateTimeForm
            :value.sync="position.endDate"
            label="component.common.end"
            :minDate="position.startDate"
            :required="!!position.geojson"
        ></opensilex-DateTimeForm>
      </div>
    </div>

    <!-- Position list -->
    <opensilex-TableView v-if="form.locations && form.locations.length !==0" :fields="fields" :items="form.locations">
      <template v-slot:cell(startDate)="{ data }">
        <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
      </template>

      <template v-slot:cell(endDate)="{ data }">
        <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
      </template>

      <template v-slot:cell(geometry)="{data}">
        <opensilex-GeometryCopy
            label="" :value="data.item.geojson">
        </opensilex-GeometryCopy>
      </template>

      <template v-slot:cell(actions)="{ data }">
        <!-- TODO: CREDENTIALS Update/delete Facility? Location?-->
        <b-button-group size="sm">
          <opensilex-EditButton
              @click="updatePosition(data)"
              label="component.common.list.buttons.update"
              :small="true"
          ></opensilex-EditButton>

          <opensilex-DeleteButton
              @click="deletePosition(data)"
              label="component.common.list.buttons.delete"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableView>
<!--    TODO: demander à Seb : validation et  ergonomie du form-->
<!--    <opensilex-LocationModalForm
        ref="locationModalForm"
    ></opensilex-LocationModalForm>-->
  </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService} from "../../lib";
import { FacilityCreationDTO, LocationObservationDTO } from 'opensilex-core/index';
import OntologyRelationsForm from "../ontology/OntologyRelationsForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import LocationModalForm from "@/components/geometry/LocationModalForm.vue";

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

  @Prop({default: FacilityForm.getEmptyForm()})
  private readonly form: FacilityCreationDTO;
  //endregion

  //#region Refs
  @Ref("validatorRef")
  private readonly validatorRef!: any;
  @Ref("ontologyRelationsForm")
  private readonly ontologyRelationsForm: OntologyRelationsForm;
  @Ref("locationModalForm")
  private readonly locationModalForm: LocationModalForm;
  //endregion

  //#region Data
  private uriGenerated = true;
  private hasAddress: boolean;
  private baseType: string;
  private typeModel = null;
  private propertyComponents = [];
  private position: LocationObservationDTO = this.getPositionEmpty();
  private fields = [
    {
      key: "date",
      label: "component.common.begin",
      sortable: true,
    },
    {
      key: "endDate",
      label: "component.common.end",
      sortable: true,
    },
    {
      key: "geometry",
      label: "component.common.geometry",
    },
    {
      key: "actions",
      label: "component.common.actions",
    },
  ]
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

  private addPosition(){
    if(this.position.geojson !== undefined){
      this.form.locations.push(this.position)
      this.position = this.getPositionEmpty();
    }
  }

  private updatePosition(data){
    this.locationModalForm.showEditForm(data.item);
  }

  private deletePosition(data){
    this.form.locations.splice(this.form.locations.indexOf(data.item),1)
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

  private getPositionEmpty(): LocationObservationDTO {
    return {
      geojson: undefined,
      startDate: undefined,
      endDate: undefined
    }
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
    add-position: Add position
fr:
  FacilityForm:
    toggleAddress: Adresse
    add-position: Ajouter une position
</i18n>