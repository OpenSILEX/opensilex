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
                  credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID
                )
              "
                @click="editSite()"
                label="OrganizationTree.edit"
                :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
                v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_ORGANIZATION_DELETE_ID
                )
              "
                @click="deleteSite()"
                label="OrganizationTree.delete"
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
            path: '/organization/site/details/' + encodeURIComponent(selected.uri),
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

        <!-- Facilities -->
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

        <!-- Metadata -->
        <opensilex-MetadataView
          v-if="selected.publisher && selected.publisher.uri"
          :publisher="selected.publisher"
          :publicationDate="selected.publication_date"
          :lastUpdatedDate="selected.last_updated_date" 
        ></opensilex-MetadataView>
      </div>
    </b-card>
    <opensilex-ModalForm
        ref="siteForm"
        component="opensilex-SiteForm"
        createTitle="OrganizationTree.addSite"
        editTitle="OrganizationTree.editSite"
        icon="ik#ik-globe"
        @onCreate="$emit('onCreate', $event)"
        @onUpdate="$emit('onUpdate', $event)"
        :initForm="initForm"
        :lazy="true"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import DTOConverter from "../../../models/DTOConverter";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import { SiteGetDTO, SiteUpdateDTO } from 'opensilex-core/index';

@Component
export default class SiteDetail extends Vue {
  $opensilex: OpenSilexVuePlugin;
  organizationService: OrganizationsService;

  @Prop()
  selected: SiteGetDTO;

  @Prop({
    default: false,
  })
  withActions;

  @Ref("siteForm") readonly siteForm!: any;

  created() {
    this.organizationService = this.$opensilex.getService("opensilex-core.OrganizationsService");
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
          path: "/organization/details/" + encodeURIComponent(organization.uri),
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
          path: "/facility/details/" + encodeURIComponent(facility.uri),
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
          let editDto: SiteUpdateDTO = DTOConverter.extractURIFromResourceProperties(http.response.result);
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
    facilities: Installations environnementales
    groups: Groupes
    noGeometryWarning: Aucune géométrie n'a pu être déterminée à partir de l'adresse. L'adresse est peut-être invalide.
</i18n>