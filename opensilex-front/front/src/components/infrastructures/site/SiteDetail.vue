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
              @click="editSite()"
              label="InfrastructureTree.edit"
              :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
              v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID
              )
            "
              @click="deleteSite()"
              label="InfrastructureTree.delete"
              :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
        <opensilex-ModalForm
            ref="siteForm"
            component="opensilex-SiteForm"
            createTitle="InfrastructureTree.add"
            editTitle="InfrastructureTree.update"
            icon="ik#ik-globe"
            @onCreate="$emit('onCreate', $event)"
            @onUpdate="$emit('onUpdate', $event)"
            :initForm="initForm"
        ></opensilex-ModalForm>
      </div>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView
          :uri="selected.uri"
          :value="selected.uri"
          :to="{
          path: '/infrastructure/site/details/' + encodeURIComponent(selected.uri),
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

      <!-- Organizations -->
      <opensilex-UriListView
          v-if="hasOrganizations"
          :list="organizationUriList"
          label="SiteDetail.organizations"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Organizations -->
      <opensilex-UriListView
          v-if="hasFacilities"
          :list="facilityUriList"
          label="SiteDetail.facilities"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Groups -->
      <opensilex-UriListView
          label="SiteDetail.groups"
          :list="groupUriList"
          :inline="false"
          v-if="hasGroups"
      >
      </opensilex-UriListView>

      <!-- Address -->
      <opensilex-StringView
          v-if="selected.address"
          :value="selected.address.readableAddress"
          label="SiteDetail.address"
      ></opensilex-StringView>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";
import {OrganisationsService} from "opensilex-core/api/organisations.service";
import {SiteGetDTO} from "opensilex-core/model/siteGetDTO";
import {UriLinkDescription} from "../../common/views/UriListView.vue";
import {SiteUpdateDTO} from "opensilex-core/model/siteUpdateDTO";

@Component
export default class SiteDetail extends Vue {
  $opensilex: any;
  organizationService: OrganisationsService;

  @Prop()
  selected: SiteGetDTO;

  @Prop({
    default: false,
  })
  withActions;

  @Ref("siteForm") readonly siteForm!: any;

  organizationUriList: Array<UriLinkDescription> = [];

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

  get hasOrganizations() {
    return this.organizationUriList && this.organizationUriList.length > 0;
  }

  get hasGroups() {
    return Array.isArray(this.selected.groups) && this.selected.groups.length > 0;
  }

  get hasFacilities() {
    return Array.isArray(this.selected.facilities) && this.selected.facilities.length > 0;
  }

  get facilityUriList() {
    return this.selected.facilities.map(facility => {
      return {
        uri: facility.uri,
        value: facility.name,
        to: {
          path: "/infrastructure/facility/details/" + encodeURIComponent(facility.uri),
        },
      }
    });
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

  @Watch("selected")
  refresh() {
    if (!this.selected || !this.selected.organizations || this.selected.organizations.length === 0) {
      this.organizationUriList = [];
      return;
    }

    this.organizationService
        .searchInfrastructures(".*", this.selected.organizations.map(org => org.uri))
        .then((http: HttpResponse<OpenSilexResponse<Array<InfrastructureGetDTO>>>) => {
          // Update the list of parent organizations
          this.organizationUriList = http.response.result.map(organization => {
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

  editSite() {
    this.organizationService
        .getSite(this.selected.uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          let getDto = http.response.result;
          let editDto: SiteUpdateDTO = {
            ...getDto,
            uri: getDto.uri,
            groups: getDto.groups.map(group => group.uri),
            facilities: getDto.facilities.map(facility => facility.uri),
            organizations: getDto.organizations.map(org => org.uri)
          };
          this.siteForm.showEditForm(editDto);
        })
        .catch(this.$opensilex.errorHandler);
  }

  deleteSite() {
    this.$emit("onDelete");
  }

  initForm(form) {
    form.organizations = this.selected.organizations;
  }

  setGroups(form) {
    form.groups = this.selected.groups.map((group) => group.uri);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  SiteDetail:
    organizations: Organizations
    facilities: Facilities
    groups: Groups
    address: Address
fr:
  SiteDetail:
    facilities: Installations techniques
    groups: Groupes
    address: Adresse
</i18n>