<!--
  - ******************************************************************************
  -                         SiteList.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 12/06/2024 16:20
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  - ******************************************************************************
  -->
<template>
  <div>
    <opensilex-StringFilter
        :filter.sync="filter"
        @update="onStringFilterUpdate()"
        placeholder="SiteList.filter-placeholder"
        :lazy="false"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchSites"
        :fields="fields"
        defaultSortBy="name"
    >
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/organization/site/details/' + encodeURIComponent(data.item.uri)}"
        />
      </template>

      <template v-slot:cell(city)="{data}">
        {{ data.item.address ? data.item.address.locality : null }}
      </template>

      <template v-slot:cell(facilities)="{data}">
        <opensilex-FacilitiesModalList
            :facilities="data.item.facilities"
            :currentSite="data.item"
            @onCRUD="refresh"
        />
      </template>

      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
              @click="emitOnEdit(data.item)"
              label="component.common.update"
              :small="true"
          ></opensilex-EditButton>

          <opensilex-DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_DELETE_ID)"
              @click="onDeleteClick(data.item)"
              label="component.common.delete"
              :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import {OpenSilexStore} from "../../../models/Store";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {Component, Ref} from "vue-property-decorator";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {SiteGetListDTO} from "opensilex-core/model/siteGetListDTO";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {SiteGetDTO} from "opensilex-core/model/siteGetDTO";
import {SiteUpdateDTO} from "opensilex-core/model/siteUpdateDTO";
import DTOConverter from "../../../models/DTOConverter";

@Component
export default class SiteList extends Vue {

  //#region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin;
  public $store: OpenSilexStore;
  private service: OrganizationsService;
  //#endregion

  //#region Refs
  @Ref("tableRef") private readonly tableRef!: any;
  //#endregion

  //#region Data
  private filter: string = "";

  private fields = [
    {key: 'name', label: 'component.common.name', sortable: true},
    {key: 'city', label: 'SiteList.address', sortable: true},
    {key: 'facilities', label: 'SiteList.facilities'},
    {key: 'actions', label: 'component.common.actions'},
  ];

  //#endregion

  //#region Computed
  private get user() {
    return this.$store.state.user;
  }

  private get credentials() {
    return this.$store.state.credentials;
  }

  //#endregion

  //#region Event
  /**
   * get all infos and convert it to UpdateDTO before emitting
   */
  private emitOnEdit(site: SiteGetListDTO) {
    //get all infos and convert it to UpdateDTO before emitting
    this.service
        .getSite(site.uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          let editDto: SiteUpdateDTO = DTOConverter.extractURIFromResourceProperties(http.response.result);
          this.$emit('onEdit', editDto);
        })
        .catch(this.$opensilex.errorHandler);
  }

  //#endregion

  //#region Event handlers
  private onStringFilterUpdate() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  private onDeleteClick(dto: SiteGetListDTO) {
    this.service
        .deleteSite(dto.uri)
        .then(() => {
          this.refresh();
          this.$opensilex.showSuccessToast("component.common.delete.success")
        })
        .catch(this.$opensilex.errorHandler);
  }

  //#endregion

  //#region Hooks
  private created() {
    this.service = this.$opensilex.getService("opensilex-core.OrganizationsService");
  }

  //#endregion

  //#region Private methods
  private searchSites(options) {
    return this.service.searchSites(
        this.filter,
        null,
        options.orderBy,
        options.currentPage,
        options.pageSize);
  }

  public refresh(): void {
    this.tableRef.refresh();
  }

  //#endregion
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  SiteList:
    address: "City"
    filter-placeholder: "Filter by name"
    facilities: "Facilities"
fr:
  SiteList:
    address: "Ville"
    filter-placeholder: "Filtrer par nom"
    facilities: "Installations"
</i18n>