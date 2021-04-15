<template>
  <b-card v-if="selected">
    <template v-slot:header>
      <h3>
        <opensilex-Icon icon="ik#ik-clipboard" />
        {{ $t("component.common.details-label") }}
      </h3>
      <div class="card-header-right" v-if="withActions">
        <b-button-group>
          <opensilex-EditButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
              )
            "
            @click="editInfrastructure()"
            label="InfrastructureTree.edit"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID
              )
            "
            @click="deleteInfrastructure()"
            label="InfrastructureTree.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
        <opensilex-ModalForm
          ref="infrastructureForm"
          component="opensilex-InfrastructureForm"
          createTitle="InfrastructureTree.add"
          editTitle="InfrastructureTree.update"
          icon="ik#ik-globe"
          @onCreate="$emit('refresh', $event)"
          @onUpdate="$emit('refresh', $event)"
          :initForm="setParent"
        ></opensilex-ModalForm>
      </div>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
        label="component.common.name"
        :value="selected.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        :type="selected.rdf_type"
        :typeLabel="selected.rdf_type_name"
      ></opensilex-TypeView>
      <!-- Parent -->
      <opensilex-InfrastructureUriView
        v-if="selected.parent"
        :uri="selected.parent"
        title="InfrastructureDetail.parent-orga"
      >
      </opensilex-InfrastructureUriView>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { InfrastructureGetDTO } from "opensilex-core/index";

@Component
export default class InfrastructureDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop({
    default: false,
  })
  withActions;

  @Ref("infrastructureForm") readonly infrastructureForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  editInfrastructure() {
    this.$opensilex
      .getService("opensilex-core.OrganisationsService")
      .getInfrastructure(this.selected.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        this.infrastructureForm.showEditForm(http.response.result);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteInfrastructure() {
    this.$emit("onDelete");
  }

  setParent(form) {
    form.parent = this.selected.parent;
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureDetail:
    parent-orga: Parent organization
fr:
  InfrastructureDetail:
    parent-orga: Organisation parente
</i18n>