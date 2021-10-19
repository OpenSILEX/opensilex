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

<!--    <div v-for="(v, index) in typeProperties" v-bind:key="index">-->
<!--      <component-->
<!--          :is="getInputComponent(v.definition, v.property)"-->
<!--          :property="v.definition"-->
<!--          :value.sync="v.property"-->
<!--          @update:value="updateRelation($event, v.definition.property)"-->
<!--          :context="context"-->
<!--      ></component>-->
<!--    </div>-->
    <slot v-if="form.rdf_type" v-bind:form="form"></slot>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService} from "../../../lib";
import {InfrastructureFacilityCreationDTO} from "opensilex-core/model/infrastructureFacilityCreationDTO";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";

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
    this.baseType = this.$opensilex.Oeso.INFRASTRUCTURE_FACILITY_TYPE_URI;
  }

  setBaseType(baseType: string) {
    this.baseType = baseType;
  }

  typeSwitch(type) {
    if (!type) {
      return;
    }

    console.log("Switch type ", type);
  }
}
</script>

<style scoped>

</style>