<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.profile.profile-uri"
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
      placeholder="component.profile.form-name-placeholder"
    ></opensilex-InputForm>

    <b-table-simple hover small responsive>
      <b-thead>
        <b-tr>
          <b-th>{{$t("ProfileForm.credentialGroups")}}</b-th>
          <b-th>{{$t("ProfileForm.credentials")}}</b-th>
        </b-tr>
      </b-thead>
      <b-tbody>
        <b-tr v-for="credentialsGroup in credentialsGroups" v-bind:key="credentialsGroup.group_id">
          <b-td>{{$t(credentialsGroup.group_key_name)}}</b-td>
          <b-td>
            <b-form-checkbox-group
              v-bind:key="credentialsGroup.group_id"
              v-model="selectedCredentials[credentialsGroup.group_id]"
              v-bind:options="credentialOptions[credentialsGroup.group_id]"
              switches
            ></b-form-checkbox-group>
          </b-td>
        </b-tr>
      </b-tbody>
    </b-table-simple>
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
      def[credentialsGroups[i].group_id] = [];

      for (let j = 0; j < credentialsGroups[i].credentials.length; j++) {
        let credentialId = credentialsGroups[i].credentials[j].id;
        if (this.form.credentials && this.form.credentials.indexOf(credentialId) >= 0) {
          def[credentialsGroups[i].group_id].push(credentialId);
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
      def[credentialsGroups[i].group_id] = [];

      for (let j = 0; j < credentialsGroups[i].credentials.length; j++) {
        let credential = credentialsGroups[i].credentials[j];
        def[credentialsGroups[i].group_id].push({
          text: this.$t(credential.name),
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

<i18n>
en:
  ProfileForm:
    credentialGroups: Credential groups
    credentials: Credentials

fr:
  ProfileForm:
    credentialGroups: Groupes d'autorisation
    credentials: Autorisations    
</i18n>