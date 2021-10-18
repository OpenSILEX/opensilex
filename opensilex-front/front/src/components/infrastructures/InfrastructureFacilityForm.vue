<template>
  <opensilex-ModalForm
    ref="facilityForm"
    component="opensilex-OntologyObjectForm"
    createTitle="InfrastructureFacilitiesView.add"
    editTitle="InfrastructureFacilitiesView.update"
    icon="ik#ik-map"
    :createAction="callInfrastructureFacilityCreation"
    :updateAction="callInfrastructureFacilityUpdate"
    @onCreate="$emit('onCreate', $event)"
    @onUpdate="$emit('onUpdate', $event)"
  ></opensilex-ModalForm>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class InfrastructureFacilityForm extends Vue {
  $opensilex: any;

  @Ref("facilityForm") readonly facilityForm!: any;

  showEditForm(form) {
    this.$opensilex
      .getService("opensilex.OrganisationsService")
      .getInfrastructureFacility(form.uri)
      .then((http) => {
        this.facilityForm
          .getFormRef()
          .setBaseType(this.$opensilex.Oeso.INFRASTRUCTURE_FACILITY_TYPE_URI);
        this.facilityForm.showEditForm(http.response.result);
      }).catch(this.$opensilex.errorHandler);
  }

  showCreateForm() {
    this.facilityForm
      .getFormRef()
      .setBaseType(this.$opensilex.Oeso.INFRASTRUCTURE_FACILITY_TYPE_URI);
    this.facilityForm.showCreateForm();
  }

  callInfrastructureFacilityCreation(form) {
    let definedRelations = [];
    for (let i in form.relations) {
      let relation = form.relations[i];
      if (relation.value != null) {
        if (Array.isArray(relation.value)) {
          for (let j in relation.value) {
            definedRelations.push({
              property: relation.property,
              value: relation.value[j],
            });
          }
        } else {
          definedRelations.push(relation);
        }
      }
    }

    form.relations = definedRelations;

    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .createInfrastructureFacility(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility created", uri);
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Infrastructure facility already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t(
              "InfrastructureFacilityForm.infrastructure-facility-already-exists"
            )
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  callInfrastructureFacilityUpdate(form) {
    let definedRelations = [];
    for (let i in form.relations) {
      let relation = form.relations[i];
      if (relation.value != null) {
        if (Array.isArray(relation.value)) {
          for (let j in relation.value) {
            definedRelations.push({
              property: relation.property,
              value: relation.value[j],
            });
          }
        } else {
          definedRelations.push(relation);
        }
      }
    }

    form.relations = definedRelations;
    return this.$opensilex
      .getService("opensilex.OrganisationsService")
      .updateInfrastructureFacility(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureFacilityForm:
    facility-uri: Infrastructure facility URI
    form-name-placeholder: Enter infrastructure facility name
    form-type-placeholder: Select infrastructure facility type
    infrastructure-facility-already-exists: Infrastructure facility already exists with this URI
fr:
  InfrastructureFacilityForm:
    facility-uri: URI de l'installation technique
    form-name-placeholder: Saisir le nom de l'installation technique
    form-type-placeholder: Sélectionner le type de l'installation technique
    infrastructure-facility-already-exists: Une installation technique existe déjà avec cette URI
</i18n>