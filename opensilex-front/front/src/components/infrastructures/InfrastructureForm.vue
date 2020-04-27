<template>
  <b-modal ref="modalRef" @ok.prevent="validate" size="md" :static="true">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <!-- URI -->
        <opensilex-UriForm
          :uri.sync="form.uri"
          label="component.infrastructure.infrastructure-uri"
          :editMode="editMode"
        ></opensilex-UriForm>

        <!-- Name -->
        <opensilex-InputForm
          :value.sync="form.name"
          label="component.common.name"
          type="text"
          :required="true"
          placeholder="component.infrastructure.form-name-placeholder"
        ></opensilex-InputForm>

        <!-- Type -->
        <opensilex-TypeForm
          :type.sync="form.type"
          :baseType="$opensilex.Oeso.INFRASTRUCTURE_TYPE_URI"
          :required="true"
          placeholder="component.infrastructure.form-type-placeholder"
        ></opensilex-TypeForm>

        <!-- Parent -->
        <opensilex-SelectForm
          :selected.sync="form.parent"
          :options="parentOptions"
          label="component.common.parent"
          placeholder="component.infrastructure.form-parent-placeholder"
        ></opensilex-SelectForm>
      </b-form>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  OntologyService,
  InfrastructureGetDTO,
  InfrastructureCreationDTO,
  ResourceTreeDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  $i18n: any;
  $t: any;

  @Prop()
  public defaultParent: InfrastructureGetDTO;

  @Prop()
  public parentOptions: Array<any>;

  @Ref("modalRef") readonly modalRef!: any;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  form: InfrastructureCreationDTO = {
    uri: "",
    type: null,
    name: "",
    parent: null
  };

  title = "";

  editMode = false;

  clearForm() {
    let parentURI = null;
    if (this.defaultParent) {
      parentURI = this.defaultParent.uri;
    }
    this.form = {
      uri: "",
      type: null,
      name: "",
      parent: null
    };
  }

  uriGenerated = true;
  showCreateForm(parentURI) {
    this.clearForm();
    this.form.parent = parentURI;
    this.editMode = false;
    this.title = this.$t("component.infrastructure.add").toString();
    this.uriGenerated = true;
    this.$opensilex.filterItemTree(this.parentOptions, this.form.uri);
    this.validatorRef.reset();
    this.modalRef.show();
  }

  showEditForm(form: InfrastructureCreationDTO) {
    this.form = form;
    this.editMode = true;
    this.title = this.$t("component.infrastructure.update").toString();
    this.uriGenerated = true;
    this.$opensilex.filterItemTree(this.parentOptions, this.form.uri);
    let modalRef: any = this.modalRef;
    this.validatorRef.reset();
    modalRef.show();
  }

  hideForm() {
    let modalRef: any = this.modalRef;
    modalRef.hide();
  }

  get parentOptions() {
    if (this.editMode) {
      return this.$opensilex.buildTreeListOptions(this.parentInfrastructures, {
        disableSubTree: this.form.uri
      });
    } else {
      return this.$opensilex.buildTreeListOptions(this.parentInfrastructures);
    }
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.InfrastructuresService")
      .createInfrastructure(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure facility created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Infrastructure already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t(
              "component.infrastructure.errors.infrastructure-already-exists"
            )
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    delete form.typeLabel;
    return this.$opensilex
      .getService("opensilex.InfrastructuresService")
      .updateInfrastructure(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Infrastructure updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

