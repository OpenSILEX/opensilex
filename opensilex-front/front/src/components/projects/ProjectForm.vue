<template>
  <b-modal ref="modalRef" @ok.prevent="validate">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <b-form ref="formRef">
      <b-form-group :label="$t('component.project.uri') + ':'" label-for="uri">
        <b-form-input id="uri" v-model="form.uri" :disabled="true" type="text"></b-form-input>
      </b-form-group>

      <b-form-group :label="$t('component.project.label') + ':'" label-for="acronym" required>
        <b-form-input
          id="acronym"
          v-model="form.label"
          type="text"
          required
          :placeholder="$t('component.project.form-acronym-placeholder')"
        ></b-form-input>
      </b-form-group>

      <b-form-group :label="$t('component.project.comment') + ':'" label-for="comment" required>
        <b-form-input
          id="comment"
          v-model="form.comment"
          type="text"
          :placeholder="$t('component.project.form-comment-placeholder')"
        ></b-form-input>
      </b-form-group>
    </b-form>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ProjectGetDTO } from "opensilex-core/index";

@Component
export default class ProjectForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  $i18n: any;

  get user() {
    return this.$store.state.user;
  }

  uriGenerated = true;

  form: ProjectGetDTO = {
    uri: "",
    label: "",
    shortname: "",
    description: "",
    objective: "",
    startDate: "",
    endDate: "",
    keywords: [],
    homePage: "",
    experiments: [],
    administrativeContacts: [],
    coordinators: [],
    scientificContacts: [],
    relatedProjects: []
  };

  title = "";

  editMode = false;

  clearForm() {
    this.form = {
      uri: "",
      label: "",
      shortname: undefined,
      description: undefined,
      objective: undefined,
      startDate: "",
      endDate: "",
      keywords: undefined,
      homePage: undefined,
      experiments: undefined,
      administrativeContacts: undefined,
      coordinators: undefined,
      scientificContacts: undefined,
      relatedProjects:undefined
    };
  }

  showCreateForm() {
    this.clearForm();
    this.form.startDate = "2000-12-01";
    this.form.endDate = "2000-12-02";
    this.editMode = false;
    this.title = this.$i18n.t("component.project.add").toString();
    this.uriGenerated = true;
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  showEditForm(form: ProjectGetDTO) {
    this.form = form;
    this.editMode = true;
    this.title = this.$i18n.t("component.project.update").toString();
    this.uriGenerated = true;
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hideForm() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();
  }

  onValidate() {
    return new Promise((resolve, reject) => {
      if (this.editMode) {
        this.$emit("onUpdate", this.form, result => {
          if (result instanceof Promise) {
            result.then(resolve).catch(reject);
          } else {
            resolve(result);
          }
        });
      } else {
        return this.$emit("onCreate", this.form, result => {
          if (result instanceof Promise) {
            result.then(resolve).catch(reject);
          } else {
            resolve(result);
          }
        });
      }
    });
  }

  validate() {
    let formRef: any = this.$refs.formRef;
    if (this.uriGenerated && !this.editMode) {
        this.form.uri = undefined;
      }
    if (formRef.checkValidity()) {
      if (this.uriGenerated && !this.editMode) {
        this.form.uri = undefined;
      }
      this.onValidate()
        .then(() => {
          this.$nextTick(() => {
            let modalRef: any = this.$refs.modalRef;
            modalRef.hide();
          });
        })
        .catch(error => {
          if (error.status == 409) {
            // TODO display error message project already exists
            console.error("TODO display error message project already exists");
          } else {
            this.$opensilex.errorHandler(error);
          }
        });
    } else {
      // TODO validation error management
      console.error("TODO validation error management");
    }
  }
}
</script>

<style scoped lang="scss">
</style>

