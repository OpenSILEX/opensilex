<template>
  <b-modal ref="modalRef" @ok.prevent="validate" size="xl" :static="true">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <!-- URI -->
        <opensilex-UriForm
          :uri.sync="form.uri"
          label="component.group.group-uri"
          helpMessage="component.common.uri.help-message"
          :editMode="editMode"
        ></opensilex-UriForm>

        <!-- Name -->
        <opensilex-InputForm
          :value.sync="form.name"
          label="component.common.name"
          type="text"
          :required="true"
          placeholder="component.group.form-name-placeholder"
        ></opensilex-InputForm>

        <!-- Description -->
        <opensilex-TextAreaForm
          :value.sync="form.description"
          label="component.common.description"
          type="text"
          :required="true"
          placeholder="component.group.form-description-placeholder"
        ></opensilex-TextAreaForm>

        <!-- User profile selection -->
        <opensilex-GroupUserProfileForm ref="userProfilesRef" :userProfiles="form.userProfiles"></opensilex-GroupUserProfileForm>
      </b-form>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  UserCreationDTO,
  GroupUpdateDTO,
  SecurityService,
  UserGetDTO,
  ProfileGetDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import GroupUserProfileForm from "./GroupUserProfileForm.vue";
import { BvModal } from "bootstrap-vue";

@Component
export default class GroupForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  service: SecurityService;

  @Ref("userProfilesRef") readonly userProfilesRef!: GroupUserProfileForm;

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        name: "",
        description: "",
        userProfiles: []
      };
    }
  })
  form: GroupUpdateDTO;

  reset() {
    this.uriGenerated = true;
    this.userProfilesRef?.clearForm();
  }

  getEmptyForm() {
    return {
      uri: null,
      name: "",
      description: "",
      userProfiles: []
    };
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .createGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Group created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Group already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("component.group.errors.group-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .updateGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Group updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

