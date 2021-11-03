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

    <!-- Organizations -->
    <opensilex-InfrastructureSelector
        label="component.experiment.infrastructures"
        :infrastructures.sync="form.organizations"
        :multiple="true"
    ></opensilex-InfrastructureSelector>

    <!-- Type -->
    <opensilex-TypeForm
        v-if="baseType"
        :type.sync="form.rdf_type"
        :baseType="baseType"
        :required="true"
        :disabled="editMode"
        placeholder="OntologyObjectForm.form-type-placeholder"
        @update:type="typeSwitch"
    ></opensilex-TypeForm>

    <!-- Dynamic fields -->
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
    <slot v-if="form.rdf_type" v-bind:form="form"></slot>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService} from "../../../lib";
import {InfrastructureFacilityCreationDTO} from "opensilex-core/model/infrastructureFacilityCreationDTO";

@Component
export default class OrganizationFacilityForm extends Vue {
  @Ref("validatorRef") readonly validatorRef!: any;

  $opensilex: any;
  ontologyService: OntologyService;
  vueOntologyService: VueJsOntologyExtensionService;
  uriGenerated = true;

  @Prop({default: false})
  editMode: boolean;

  @Prop({
    default: OrganizationFacilityForm.getEmptyForm()
  })
  form: InfrastructureFacilityCreationDTO;

  baseType: string;
  typeModel = null;
  propertyComponents = [];

  getEmptyForm() {
    return OrganizationFacilityForm.getEmptyForm();
  }

  static getEmptyForm() {
    return {
      uri: undefined,
      rdf_type: undefined,
      name: undefined,
      organizations: [],
      relations: []
    };
  }

  created() {
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.vueOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
    this.baseType = this.$opensilex.Oeso.FACILITY_TYPE_URI;
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

          internalTypeProperties.push({
            property: dataProperty,
            value: relation.value
          });
        }
      }

      for (let i in this.typeModel.object_properties) {

        let objectProperty = this.typeModel.object_properties[i];
        let relation = this.form.relations.find(relation => relation.property == objectProperty.property);

        internalTypeProperties.push({
          property: objectProperty,
          value: relation.value
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

  updateRelation(newValue,property){
    let relation = this.form.relations.find(relation =>
        relation.property == property.property
    );

    relation.value = newValue;
  }
}
</script>

<style scoped>

</style>