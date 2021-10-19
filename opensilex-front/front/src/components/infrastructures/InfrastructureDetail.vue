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
          @onCreate="$emit('onCreate', $event)"
          @onUpdate="$emit('onUpdate', $event)"
          :initForm="setParent"
        ></opensilex-ModalForm>
      </div>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView
        :uri="selected.uri"
        :value="selected.uri"
        :to="{
          path: '/infrastructure/details/' + encodeURIComponent(selected.uri),
        }"
      ></opensilex-UriView>
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

      <opensilex-UriListView
          label="InfrastructureDetail.groups.label"
          :list="groupUriList"
          :inline="false"
          v-if="hasGroups"
      >
      </opensilex-UriListView>

      <opensilex-UriListView
          label="InfrastructureDetail.facilities.label"
          :list="facilityUriList"
          :inline="false"
          v-if="hasFacilities"
        >
      </opensilex-UriListView>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";
import {InfrastructureUpdateDTO} from "opensilex-core/model/infrastructureUpdateDTO";

@Component
export default class InfrastructureDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected: InfrastructureGetDTO;

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

  get hasGroups() {
    return this.selected.groups.length > 0;
  }

  get hasFacilities() {
    return this.selected.facilities.length > 0;
  }

  get groupUriList() {
    return this.selected.groups.map(group => {
      return {
        uri: group.uri,
        value: group.name,
        to: {
          path: "/groups#" + encodeURIComponent(group.uri),
        },
      }
    });
  }

  get facilityUriList() {
    return this.selected.facilities.map(facility => {
      return {
        uri: facility.uri,
        value: facility.name,
        to: {
          path: "/infrastructure/facility" + encodeURIComponent(facility.uri)
        }
      };
    });
  }

  editInfrastructure() {
    this.$opensilex
      .getService("opensilex-core.OrganisationsService")
      .getInfrastructure(this.selected.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let getDto = http.response.result;
        let editDto = {
          ...getDto,
          uri: getDto.uri,
          groups: getDto.groups.map(group => group.uri)
        };
        this.infrastructureForm.showEditForm(editDto);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteInfrastructure() {
    this.$emit("onDelete");
  }

  setParent(form) {
    form.parent = this.selected.parent;
  }

  setInfrastructure(form) {
    form.groups = this.selected.groups.map((group) => group.uri);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureDetail:
    parent-orga: Parent organization
    groups:
      label: "Groups"
      edit: "Edit"
    facilities:
      label: "Facilities"
fr:
  InfrastructureDetail:
    parent-orga: Organisation parente
    groups:
      label: "Groupes"
      edit: "Modifier"
    facilities:
      label: "Installations techniques"
</i18n>