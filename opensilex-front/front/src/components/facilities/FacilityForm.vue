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
        @update:type="typeSwitch"
    ></opensilex-TypeForm>

    <!-- Name -->
    <opensilex-InputForm
        :value.sync="form.name"
        label="component.common.name"
        type="text"
        :required="true"
        placeholder="OntologyObjectForm.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Organizations -->
    <opensilex-OrganizationSelector
        label="component.experiment.infrastructures"
        :infrastructures.sync="form.organizations"
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

    <!-- Geometry -->
    <opensilex-GeometryForm
      :value.sync="form.geometry"
      label="component.common.geometry"
      helpMessage="component.common.geometry-help"
    >
    </opensilex-GeometryForm>

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

    <!-- Dynamic fields -->
    <div v-for="(relation, index) in typeRelations" v-bind:key="index">
      <component
          :is="getInputComponent(relation.property)"
          :property="relation.property"
          :label="relation.property.name"
          :required="relation.property.is_required"
          :multiple="relation.property.is_list"
          :value.sync="relation.value"
          @update:value="updateRelation($event, relation.property)"
      ></component>
    </div>
    <slot v-if="form.rdf_type" v-bind:form="form"></slot>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService} from "../../lib";
import { FacilityCreationDTO } from 'opensilex-core/index';

@Component
export default class FacilityForm extends Vue {
  @Ref("validatorRef") readonly validatorRef!: any;

  $opensilex: any;
  ontologyService: OntologyService;
  vueOntologyService: VueJsOntologyExtensionService;
  uriGenerated = true;

  @Prop({default: false})
  editMode: boolean;

  @Prop({
    default: FacilityForm.getEmptyForm()
  })
  form: FacilityCreationDTO;
  hasAddress: boolean;

  baseType: string;
  typeModel = null;
  propertyComponents = [];

  getEmptyForm() {
    return FacilityForm.getEmptyForm();
  }

  static getEmptyForm(): FacilityCreationDTO {
    return {
      uri: undefined,
      rdf_type: undefined,
      name: undefined,
      geometry: undefined,
      address: undefined,
      organizations: [],
      sites: [],
      relations: []
    };
  }

  created() {
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
    this.baseType = this.$opensilex.Oeso.FACILITY_TYPE_URI;
    this.hasAddress = !!this.form.address;
  }

  @Watch("form")
  onFacilityChanged() {
    // Update hasAddress checkbox
    this.hasAddress = !!this.form.address;
    // Reset the type model
    this.resetTypeModel();
  }

  onAddressToggled() {
    this.form.address = this.hasAddress
      ? {}
      : undefined;
  }

  // Manage dynamic fields depending on the type
  // For now, there is no concrete difference between types
  // But might be useful in the future
  // taken from EventFrom.vue

  getInputComponent(property) {
    if (property.input_components_by_property && property.input_components_by_property[property.property]) {
      return property.input_components_by_property[property.property];
    }
    return property.input_component;
  }

  resetTypeModel(){
    this.typeModel = undefined;
  }

  setBaseType(baseType: string) {
    this.baseType = baseType;
  }

  get typeRelations() {
    let internalTypeProperties = [];

    if (this.typeModel) {
      for (let i in this.typeModel.data_properties) {
        let dataProperty = this.typeModel.data_properties[i];
        if (dataProperty.property != "rdfs:label") {

          let relation = this.form.relations.find(relation => relation.property == dataProperty.property);
          let relationValue = relation ? relation.value : undefined;

          internalTypeProperties.push({
            property: dataProperty,
            value: relationValue
          });
        }
      }

      for (let i in this.typeModel.object_properties) {

        let objectProperty = this.typeModel.object_properties[i];
        let relation = this.form.relations.find(relation => relation.property == objectProperty.property);
        let relationValue = relation ? relation.value : undefined;

        internalTypeProperties.push({
          property: objectProperty,
          value: relationValue
        });
      }
    }
    return internalTypeProperties;
  }

  typeSwitch(type) {
    if (!type) {
      return;
    }

    return this.vueOntologyService
        .getRDFTypeProperties(this.form.rdf_type, this.baseType)
        .then(http => {
          this.typeModel = http.response.result;
          if (!this.editMode) {
            let relations = [];
            for (let i in this.typeModel.data_properties) {
              let dataProperty = this.typeModel.data_properties[i];
              if (dataProperty.is_list) {
                relations.push({
                  value: [],
                  property: dataProperty.property
                });
              } else {
                relations.push({
                  value: undefined,
                  property: dataProperty.property
                });
              }
            }

            for (let i in this.typeModel.object_properties) {
              let objectProperty = this.typeModel.object_properties[i];
              if (objectProperty.is_list) {
                relations.push({
                  value: [],
                  property: objectProperty.property
                });
              } else {
                relations.push({
                  value: undefined,
                  property: objectProperty.property
                });
              }
            }

            this.form.relations = relations;
          }
        });
  }

  updateRelation(newValue, property){
    let relation = this.form.relations.find(relation =>
        relation.property == property
    );

    if (relation) {
      relation.value = newValue;
      return;
    }

    relation = {
      property: property.uri,
      value: newValue
    };
    this.form.relations.push(relation);
  }
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