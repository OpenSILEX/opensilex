<template>
  <b-modal ref="modalRef" @ok.prevent="validate" size="md" :static="true">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <!-- URI -->
        <!-- URI -->
        <opensilex-UriForm
          :uri.sync="form.uri"
          label="component.infrastructure.facility.facility-uri"
          :editMode="editMode"
        ></opensilex-UriForm>

        <!-- Name -->
        <opensilex-InputForm
          :value.sync="form.name"
          label="component.common.name"
          type="text"
          :required="true"
          placeholder="component.infrastructure.facility.form-name-placeholder"
        ></opensilex-InputForm>

        <!-- Type -->
        <opensilex-TypeForm
          :type.sync="form.type"
          :baseType="$opensilex.Oeso.INFRASTRUCTURE_FACILITY_TYPE_URI"
          :required="true"
          placeholder="component.infrastructure.facility.form-type-placeholder"
        ></opensilex-TypeForm>
      </b-form>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Oeso from "../../ontologies/Oeso";
import {
  OntologyService,
  InfrastructureGetDTO,
  InfrastructureCreationDTO,
  ResourceTreeDTO,
  InfrastructureFacilityCreationDTO,
  InfrastructuresService
} from "opensilex-core/index";

@Component
export default class InfrastructureFacilityForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  $i18n: any;
  $t: any;
  ontologyService: OntologyService;
  infrastructureService: InfrastructuresService;

  @Prop()
  public parentURI: string;

  @Ref("modalRef") readonly modalRef!: any;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  uriGenerated = true;

  form: InfrastructureFacilityCreationDTO = {
    uri: "",
    type: null,
    name: "",
    infrastructure: null
  };

  title = "";

  editMode = false;

  created() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-security.OntologyService"
    );
    this.infrastructureService = this.$opensilex.getService(
      "opensilex-core.InfrastructuresService"
    );
  }

  clearForm() {
    this.form = {
      uri: "",
      type: null,
      name: "",
      infrastructure: this.parentURI
    };
  }

  showCreateForm(parentURI) {
    this.clearForm();
    this.form.infrastructure = parentURI;
    this.editMode = false;
    this.title = this.$t("component.infrastructure.facility.add").toString();
    this.uriGenerated = true;
    this.validatorRef.reset();
    this.modalRef.show();
  }

  showEditForm(form: InfrastructureFacilityCreationDTO) {
    this.form = form;
    this.editMode = true;
    this.title = this.$t("component.infrastructure.facility.update").toString();
    this.uriGenerated = true;
    let modalRef: any = this.modalRef;
    this.validatorRef.reset();
    modalRef.show();
  }

  hideForm() {
    let modalRef: any = this.modalRef;
    modalRef.hide();
  }

  onValidate() {
    this.form.infrastructure = this.parentURI;
    return new Promise((resolve, reject) => {
      if (this.editMode) {
        this.infrastructureService
          .updateInfrastructureFacility(this.form)
          .then(() => {
            this.$emit("onUpdate", this.form);
            resolve();
          })
          .catch(error => {
            this.$opensilex.errorHandler(error);
            reject();
          });
      } else {
        this.infrastructureService
          .createInfrastructureFacility(this.form)
          .then(() => {
            this.$emit("onCreate", this.form);
            resolve();
          })
          .catch(error => {
            this.$opensilex.errorHandler(error);
            reject();
          });
      }
    });
  }

  validate() {
    let validatorRef: any = this.validatorRef;
    validatorRef.validate().then(isValid => {
      if (isValid) {
        if (this.uriGenerated && !this.editMode) {
          this.form.uri = null;
        }

        this.onValidate()
          .then(() => {
            this.$nextTick(() => {
              let modalRef: any = this.modalRef;
              modalRef.hide();
            });
          })
          .catch(error => {
            if (error.status == 409) {
              console.error("Infrastructure facility already exists", error);
              this.$opensilex.errorHandler(
                error,
                this.$i18n.t(
                  "component.infrastructure.errors.infrastructure-facility-already-exists"
                )
              );
            } else {
              this.$opensilex.errorHandler(error);
            }
          });
      }
    });
  }
}
</script>

<style scoped lang="scss">
</style>

