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
          :initForm="setParents"
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

      <!-- Parents -->
      <opensilex-UriListView
        v-if="hasParents"
        :list="parentUriList"
        label="InfrastructureDetail.parent-orga"
        :inline="false"
      >
      </opensilex-UriListView>

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
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";
import {InfrastructureUpdateDTO} from "opensilex-core/model/infrastructureUpdateDTO";
import {OrganisationsService} from "opensilex-core/api/organisations.service";

@Component
export default class InfrastructureDetail extends Vue {
  $opensilex: any;
  organizationService: OrganisationsService;

  @Prop()
  selected: InfrastructureGetDTO;

  @Prop({
    default: false,
  })
  withActions;

  @Ref("infrastructureForm") readonly infrastructureForm!: any;

  parentUriList: Array<any> = [];

  created() {
    this.organizationService = this.$opensilex.getService("opensilex-core.OrganisationsService");

    this.refresh();
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get hasParents() {
    return this.selected.parents.length > 0;
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
          path: "/infrastructure/facility/details/" + encodeURIComponent(facility.uri)
        }
      };
    });
  }

  @Watch("selected")
  refresh() {
    if (!this.selected) {
      return;
    }

    this.organizationService
      .searchInfrastructures(".*", this.selected.parents)
      .then((http: HttpResponse<OpenSilexResponse<Array<InfrastructureGetDTO>>>) => {
        // Update the list of parent organizations
        this.parentUriList = http.response.result.map(organization => {
          return {
            uri: organization.uri,
            value: organization.name,
            to: {
              path: "/infrastructure/details/" + encodeURIComponent(organization.uri)
            }
          };
        });
      });
  }

  editInfrastructure() {
    this.organizationService
      .getInfrastructure(this.selected.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let getDto = http.response.result;
        let editDto = {
          ...getDto,
          uri: getDto.uri,
          groups: getDto.groups.map(group => group.uri),
          facilities: getDto.facilities.map(facility => facility.uri)
        };
        this.infrastructureForm.showEditForm(editDto);
      })
      .catch(this.$opensilex.errorHandler);
  }

  deleteInfrastructure() {
    this.$emit("onDelete");
  }

  setParents(form) {
    form.parents = this.selected.parents;
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
    parent-orga: Parent organizations
    groups:
      label: "Groups"
      edit: "Edit"
    facilities:
      label: "Facilities"
fr:
  InfrastructureDetail:
    parent-orga: Organisations parentes
    groups:
      label: "Groupes"
      edit: "Modifier"
    facilities:
      label: "Installations techniques"
</i18n>