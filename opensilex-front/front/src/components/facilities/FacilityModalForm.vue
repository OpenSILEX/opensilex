<template>
  <opensilex-ModalForm
    ref="facilityForm"
    component="opensilex-FacilityForm"
    createTitle="FacilitiesView.add"
    editTitle="FacilitiesView.update"
    icon="ik#ik-map"
    :createAction="callOrganizationFacilityCreation"
    :updateAction="callOrganizationFacilityUpdate"
    @onCreate="$emit('onCreate', $event)"
    @onUpdate="$emit('onUpdate', $event)"
    :initForm="initForm"
    :doNotHideOnError="true"
    :lazy="lazy"
  ></opensilex-ModalForm>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import DTOConverter from "../../models/DTOConverter";
import {FacilityCreationDTO} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {FacilityGetDTO} from "opensilex-core/model/facilityGetDTO";
import ModalForm from "../common/forms/ModalForm.vue";
import FacilityForm from "./FacilityForm.vue";
import {FacilityUpdateDTO} from "opensilex-core/model/facilityUpdateDTO";
import { UserGetDTO } from "../../../../../opensilex-security/front/src/lib";

@Component
export default class FacilityModalForm extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  lazy: boolean;

  @Prop({
    default: () => {}
  })
  initForm: (f: FacilityCreationDTO) => {};

  @Ref("facilityForm") readonly facilityForm!: ModalForm<FacilityForm, FacilityCreationDTO, FacilityUpdateDTO>;

  showEditForm(uri) {
    this.$opensilex
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .getFacility(uri)
      .then((http) => {
        let dto: FacilityGetDTO = http.response.result;
        let publisher: UserGetDTO = dto.publisher;
        this.facilityForm
          .getFormRef()
          .typeSwitch(dto.rdf_type, true);
        let editDto = DTOConverter.extractURIFromResourceProperties<FacilityGetDTO, FacilityUpdateDTO>(dto);
        editDto.publisher = publisher;       
        this.facilityForm.showEditForm(editDto);
      }).catch(this.$opensilex.errorHandler);
  }

  showCreateForm() {
    this.facilityForm.showCreateForm();
    this.$nextTick(() => {
        this.facilityForm
            .getFormRef()
            .setBaseType(this.$opensilex.Oeso.FACILITY_TYPE_URI);
    });
  }

  callOrganizationFacilityCreation(form: FacilityCreationDTO) {
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
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .createFacility(form)
      .then((http: HttpResponse<OpenSilexResponse<string>>) => {
        let message = this.$i18n.t("OrganizationFacilityForm:.name") + " " + form.name + " " + this.$i18n.t("component.common.success.creation-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch((error) => {
        if (error.status === 409) {
          console.error("Organization facility already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t(
              "OrganizationFacilityForm.organization-facility-already-exists"
            )
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  callOrganizationFacilityUpdate(form: FacilityUpdateDTO) {
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
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .updateFacility(form)
      .then((http: HttpResponse<OpenSilexResponse<string>>) => {
        let message = this.$i18n.t("OrganizationFacilityForm:.name") + " " + form.name + " " + this.$i18n.t("component.common.success.update-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  OrganizationFacilityForm:
    name: The facility
    facility-uri: Organization facility URI
    form-name-placeholder: Enter organization facility name
    form-type-placeholder: Select organization facility type
    organization-facility-already-exists: Organization facility already exists with this URI
fr:
  OrganizationFacilityForm:
    name: L'installation environnementale
    facility-uri: URI de l'installation environnementale
    form-name-placeholder: Saisir le nom de l'installation environnementale
    form-type-placeholder: Sélectionner le type de l'installation environnementale
    organization-facility-already-exists: Une installation environnementale existe déjà avec cette URI
</i18n>