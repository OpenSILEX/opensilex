<template>
  <b-modal ref="modalRef" @ok.prevent="validate" size="xl" :static="true">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <!-- URI -->
        <opensilex-FormAutogenerateURI
          label="component.infrastructure.device.device-uri"
          :uri="form.uri"
          :editMode="editMode"
          :uriGenerated="uriGenerated"
        ></opensilex-FormAutogenerateURI>
        <!-- Name -->
        <b-form-group :label="$t('component.common.name') + ':'" label-for="name" required>
          <ValidationProvider
            :name="$t('component.common.name')"
            rules="required"
            v-slot="{ errors }"
          >
            <b-form-input
              id="name"
              v-model="form.name"
              type="text"
              required
              :placeholder="$t('component.infrastructure.device.form-name-placeholder')"
            ></b-form-input>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
        <!-- Type -->
        <b-form-group :label="$t('component.common.type') + ':'" label-for="type" required>
          <ValidationProvider
            :name="$t('component.common.type')"
            rules="required"
            v-slot="{ errors }"
          >
            <treeselect
              id="type"
              :options="infraTypesOptions"
              :load-options="initInfraDeviceTypes"
              :placeholder="$t('component.infrastructure.device.form-type-placeholder')"
              v-model="form.type"
            />
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
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
  InfrastructureDeviceCreationDTO,
  InfrastructuresService
} from "opensilex-core/index";

@Component
export default class InfrastructureDeviceForm extends Vue {
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

  form: InfrastructureDeviceCreationDTO = {
    uri: "",
    type: null,
    name: "",
    infrastructure: null
  };

  infraTypesOptions = null;

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

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadInfraDeviceTypes();
      }
    );
  }

  initInfraDeviceTypes({ action, parentNode, callback }) {
    this.loadInfraDeviceTypes(callback);
  }

  loadInfraDeviceTypes(callback?) {
    this.ontologyService
      .getSubClassesOf(Oeso.INFRASTRUCTURE_DEVICE_TYPE_URI, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        this.infraTypesOptions = this.$opensilex.buildTreeListOptions(
          http.response.result
        );

        if (callback) {
          callback();
        }
      })
      .catch(this.$opensilex.errorHandler);
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
    this.title = this.$t("component.infrastructure.device.add").toString();
    this.uriGenerated = true;
    this.validatorRef.reset();
    this.modalRef.show();
  }

  showEditForm(form: InfrastructureDeviceCreationDTO) {
    this.form = form;
    this.editMode = true;
    this.title = this.$t("component.infrastructure.device.update").toString();
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
          .updateInfrastructureDevice(this.form)
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
          .createInfrastructureDevice(this.form)
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
              console.error("Infrastructure device already exists", error);
              this.$opensilex.errorHandler(
                error,
                this.$i18n.t(
                  "component.infrastructure.errors.infrastructure-device-already-exists"
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

