<!--
  - ******************************************************************************
  -                         SiteDetail.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 14/06/2024 13:29
  - Contact: yvan.roux@inrae.fr
  - ******************************************************************************
  -->

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
                user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
                @click="editSite()"
                label="SiteDetail.edit"
                :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
                v-if="
                user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_DELETE_ID)"
                @click="deleteSite()"
                label="SiteDetail.delete"
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
            :to="{path: '/organization/site/details/' + encodeURIComponent(selected.uri),}"
        ></opensilex-UriView>
        <!-- Name -->
        <opensilex-StringView
            label="component.common.name"
            :value="selected.name"
        ></opensilex-StringView>
        <!-- Description -->
        <opensilex-StringView
            label="component.common.description"
            :value="selected.description"
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
            :inline="true"
        />

        <!-- Groups -->
        <opensilex-UriListView
            label="SiteDetail.groups"
            :list="groupUriList"
            :inline="true"
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

          <!-- Warning if one site has an address but its geometry isn't found : problem with the geocoding service (address not found)
          but it must not block the request. -->
          <b-alert
                  v-if="selected.address && !selected.geometry"
                  variant="warning"
                  show
          >
              {{$t("component.common.geometry-address-warning")}}
          </b-alert>

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
        createTitle="add"
        editTitle="edit"
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
  private readonly selected: SiteGetDTO;

  @Prop({
    default: false,
  })
  private readonly withActions;

  @Ref("siteForm") readonly siteForm!: any;

  private created() {
    this.organizationService = this.$opensilex.getService("opensilex-core.OrganizationsService");
  }

  private get user() {
    return this.$store.state.user;
  }

  private get credentials() {
    return this.$store.state.credentials;
  }

  private get hasOrganizations() {
    return Array.isArray(this.selected.organizations) && this.selected.organizations.length > 0;
  }

  private get hasGroups() {
    return Array.isArray(this.selected.groups) && this.selected.groups.length > 0;
  }

  private get organizationUriList() {
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

  private get groupUriList() {
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

  private editSite() {
    this.organizationService
        .getSite(this.selected.uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          let editDto: SiteUpdateDTO = DTOConverter.extractURIFromResourceProperties(http.response.result);
          this.siteForm.showEditForm(editDto);
        })
        .catch(this.$opensilex.errorHandler);
  }

  private deleteSite() {
    this.organizationService
        .deleteSite(this.selected.uri)
        .then(() => {
          this.$emit("onDelete");
        })
        .catch(this.$opensilex.errorHandler);
  }

  private initForm(form) {
    form.organizations = this.selected.organizations;
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  add: Add site
  edit: Edit site
  delete: Delete site
  SiteDetail:
    organizations: Organizations
    facilities: Facilities
    groups: Groups
    noGeometryWarning: No geometry was associated with the address. Maybe the address is invalid.
fr:
  add: Ajouter un site
  edit: Modifier un site
  delete: Supprimer un site
  SiteDetail:
    facilities: Installations environnementales
    groups: Groupes
    noGeometryWarning: Aucune géométrie n'a pu être déterminée à partir de l'adresse. L'adresse est peut-être invalide.
</i18n>