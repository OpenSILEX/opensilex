<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.group.group-uri"
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
      placeholder="component.group.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Description -->
    <opensilex-TextAreaForm
      :value.sync="form.description"
      label="component.common.description"
      :required="true"
      @keydown.native.enter.stop
      placeholder="component.group.form-description-placeholder"
    ></opensilex-TextAreaForm>

    <!-- User profile selection -->
    <opensilex-GroupUserProfileForm
      v-if="form.user_profiles"
      ref="userProfilesRef"
      :profiles.sync="form.user_profiles"
      @keydown.native.enter.stop
    ></opensilex-GroupUserProfileForm>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
// @ts-ignore
import { GroupUpdateDTO, SecurityService } from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import GroupUserProfileForm from "./GroupUserProfileForm.vue";

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
        user_profiles: []
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
      user_profiles: []
    };
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .createGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Group created", uri);
        this.$opensilex.showSuccessToast(this.$t("component.group.group-added"));
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Group already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.group.errors.group-already-exists")
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
        this.$opensilex.showSuccessToast(this.$t("component.group.group-updated"));
        
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

