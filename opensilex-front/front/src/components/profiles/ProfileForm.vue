<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.profile.profile-uri"
      helpMessage="component.common.uri.help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="component.profile.form-name-placeholder"
    ></opensilex-InputForm>

    <!-- Profiles categories -->
    <b-card no-body v-if="form.credentials">
      <b-tabs pills card>
        <b-tab
          v-for="credentialsGroup in credentialsGroups"
          v-bind:key="credentialsGroup.groupId"
          v-bind:title="$t(credentialsGroup.groupKeyLabel)"
        >
          <!-- Profiles category credentials -->
          <b-form-checkbox-group
            v-bind:key="credentialsGroup.groupId"
            v-model="selectedCredentials[credentialsGroup.groupId]"
            v-bind:options="credentialOptions[credentialsGroup.groupId]"
            switches
          ></b-form-checkbox-group>
        </b-tab>
      </b-tabs>
    </b-card>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class ProfileForm extends Vue {
  $opensilex: any;

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        name: "",
        credentials: []
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      uri: null,
      name: "",
      credentials: []
    };
  }

  create(form) {
    let credentials = [];
    for (let i in this._selectedCredentials) {
      credentials = credentials.concat(this._selectedCredentials[i]);
    }
    this.form.credentials = credentials;
    this.$opensilex
      .getService("opensilex.SecurityService")
      .createProfile(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Profile created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Profile already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.profile.errors.profile-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    let credentials = [];
    for (let i in this._selectedCredentials) {
      credentials = credentials.concat(this._selectedCredentials[i]);
    }
    this.form.credentials = credentials;
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .updateProfile(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Profile updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  created() {
    this.$opensilex.getCredentials().then(credentials => {
      this.credentialsGroups = credentials;
    });
  }

  credentialsGroups: any = [];

  private _selectedCredentials = null;

  get selectedCredentials() {
    let def: any = {};
    let credentialsGroups = this.credentialsGroups;
    for (let i = 0; i < credentialsGroups.length; i++) {
      def[credentialsGroups[i].groupId] = [];

      for (let j = 0; j < credentialsGroups[i].credentials.length; j++) {
        let credentialId = credentialsGroups[i].credentials[j].id;
        if (this.form.credentials.indexOf(credentialId) >= 0) {
          def[credentialsGroups[i].groupId].push(credentialId);
        }
      }
    }

    this._selectedCredentials = def;

    return this._selectedCredentials;
  }

  set selectedCredentials(value) {
    this._selectedCredentials = value;
  }

  get credentialOptions() {
    let credentialsGroups = this.credentialsGroups;
    let def: any = {};
    for (let i = 0; i < credentialsGroups.length; i++) {
      def[credentialsGroups[i].groupId] = [];

      for (let j = 0; j < credentialsGroups[i].credentials.length; j++) {
        let credential = credentialsGroups[i].credentials[j];
        def[credentialsGroups[i].groupId].push({
          text: this.$t(credential.label),
          value: credential.id
        });
      }
    }

    return def;
  }
}
</script>

<style scoped lang="scss">
</style>

