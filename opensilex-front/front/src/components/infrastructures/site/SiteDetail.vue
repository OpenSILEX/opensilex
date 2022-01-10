<template>
  <div style="display: contents;">
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
        <opensilex-AddressView
            v-if="selected.address"
            :address="selected.address"
            :geometry="selected.geometry"
            noGeometryLabel="SiteDetail.noGeometryWarning"
        >
        </opensilex-AddressView>
      </div>
    </b-card>
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
import {ResourceDagDTO} from "opensilex-core/model/resourceDagDTO";

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

  created() {
    this.organizationService = this.$opensilex.getService("opensilex-core.OrganisationsService");
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get hasOrganizations() {
    return Array.isArray(this.selected.organizations) && this.selected.organizations.length > 0;
  }

  get hasGroups() {
    return Array.isArray(this.selected.groups) && this.selected.groups.length > 0;
  }

  get hasFacilities() {
    return Array.isArray(this.selected.facilities) && this.selected.facilities.length > 0;
  }

  get organizationUriList() {
    return this.selected.organizations.map(organization => {
      return {
        uri: organization.uri,
        value: organization.name,
        to: {
          path: "/infrastructure/facility/details/" + encodeURIComponent(organization.uri),
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
    noGeometryWarning: No geometry was associated with the address. Maybe the address is invalid.
fr:
  SiteDetail:
    facilities: Installations techniques
    groups: Groupes
    noGeometryWarning: Aucune géométrie n'a pu être déterminée à partir de l'adresse. L'adresse est peut-être invalide.
</i18n>