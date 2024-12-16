<!--
  - ******************************************************************************
  -                         OrganizationList.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 29/08/2024 09:16
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  - ******************************************************************************
  -->
<template>
  <div id="page-container">
    <!-- Toggle Sidebar-->
    <div class="searchMenuContainer"
         v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
         :title="$t('searchfilter.label')">
      <div class="searchMenuIcon">
        <i class="ik ik-search"></i>
      </div>
    </div>
    <!-- FILTERS -->
    <Transition>
      <div v-if="SearchFiltersToggle" id="filter-side-bar">
        <opensilex-SearchFilterField
            @clear="reset()"
            @search="refresh()"
            class="searchFilterField">
          <template v-slot:filters>

            <!-- Name -->
            <div>
              <opensilex-FilterField>
                <label for="name">{{ $t("component.common.name") }}</label>
                <opensilex-StringFilter
                    id="name"
                    :filter.sync="filter.name"
                    placeholder="OrganizationList.filter.name-placeholder"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                ></opensilex-StringFilter>
              </opensilex-FilterField><br>
            </div>

            <!-- Type -->
            <div>
              <opensilex-FilterField>
                <opensilex-TypeForm
                    :type.sync="filter.type_uri"
                    :baseType="$opensilex.Foaf.ORGANIZATION_TYPE_URI"
                    :ignoreRoot="false"
                    placeholder="OrganizationList.filter.type-placeholder"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                ></opensilex-TypeForm>
              </opensilex-FilterField>
            </div>

            <!-- Parents of-->
            <div>
              <opensilex-FilterField>
                <opensilex-FormSelector
                    :selected.sync="filter.direct_child_uri"
                    :options="parentOptions"
                    :multiple="false"
                    label="OrganizationList.filter.parent-organizations"
                    placeholder="OrganizationForm.form-parent-placeholder"
                    helpMessage="OrganizationList.filter.parent-organizations-help"
                    class="searchFilter"
                ></opensilex-FormSelector>
              </opensilex-FilterField>
            </div>

            <!-- Childs of-->
            <div>
              <opensilex-FilterField>
                <opensilex-FormSelector
                    :selected.sync="filter.direct_parent_uri"
                    :options="parentOptions"
                    :multiple="false"
                    label="OrganizationList.filter.child-organizations"
                    placeholder="OrganizationForm.form-parent-placeholder"
                    helpMessage="OrganizationList.filter.child-organizations-help"
                    class="searchFilter"
                ></opensilex-FormSelector>
              </opensilex-FilterField>
            </div>

            <!-- Facility -->
            <div>
              <opensilex-FilterField>
                <opensilex-FacilitySelector
                    ref="facilitySelector"
                    label="OrganizationForm.form-facilities-label"
                    :facilities.sync="filter.facility"
                    :multiple="false"
                    class="searchFilter"
                ></opensilex-FacilitySelector>
              </opensilex-FilterField>
            </div>


          </template>
        </opensilex-SearchFilterField>
      </div>
    </Transition>
    <opensilex-TableView
        :items="organizations"
        :fields="fields"
        sortBy="name"
    >
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/organization/details/' + encodeURIComponent(data.item.uri)}"
        />
      </template>

      <template v-slot:cell(rdf_type_name)="{data}">
        {{data.item.rdf_type_name}}
      </template>

            <template v-slot:cell(facilities)="{data}">
              <opensilex-FacilitiesModalList
                  :facilities="data.item.facilities"
                  :hostNameForTitle="data.item.name"
                  @onCRUD="refresh"
              />
            </template>

      <template v-slot:cell(actions)="{data}" v-if="!disableActions">
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
    </opensilex-TableView>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import {OpenSilexStore} from "../../models/Store";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Component, Prop, Watch} from "vue-property-decorator";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import DTOConverter from "../../models/DTOConverter";
import {OrganizationGetDTO} from "opensilex-core/model/organizationGetDTO";
import {OrganizationDagDTO} from "opensilex-core/model/organizationDagDTO";
import {OrganizationUpdateDTO} from "opensilex-core/model/organizationUpdateDTO";

/**
 * direct_child_uri is the uri of the organization for which we want to display the direct parents
 * direct_parent_uri is the uri of the organization for which we want to display the direct children
 */
interface OrganizationListFilter {
  name: string;
  type_uri: string;
  direct_child_uri: string
  direct_parent_uri: string
  facility: string;
}

@Component
export default class OrganizationList extends Vue {

  //#region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin;
  public $store: OpenSilexStore;
  private service: OrganizationsService;
  //#endregion

  //#region Props
  /**
   * List of URIs of organizations to fetch, if you don't want to search for all organizations
   */
  @Prop({default: null})
  private readonly organizationsToFetch: string[];

  @Prop({default: false})
  private disableActions: boolean;
  //#endregion

  //#region Data
  private organizations: OrganizationDagDTO[] = [];

  private filter: OrganizationListFilter = this.getEmptyFilter();

  private allOrganizations: OrganizationDagDTO[] = [];

  private SearchFiltersToggle: boolean = false;

  //#endregion

  //#region Computed

  private get user() {
    return this.$store.state.user;
  }

  private get credentials() {
    return this.$store.state.credentials;
  }

  private get fields() {
    const fields = [
      {key: 'name', label: 'component.common.name', sortable: true},
      {key: 'rdf_type_name', label: 'type', sortable: true},
      {key: 'facilities', label: 'OrganizationList.facilities'},
    ];
    if (!this.disableActions) {
      fields.push({key: 'actions', label: 'component.common.actions'});
    }
    return fields;
  }

  //#endregion

  //#region Event
  /**
   * get all infos and convert it to UpdateDTO before emitting
   */
  private emitOnEdit(organization: OrganizationDagDTO) {
    //get all infos and convert it to UpdateDTO before emitting
    this.service
        .getOrganization(organization.uri)
        .then((http: HttpResponse<OpenSilexResponse<OrganizationGetDTO>>) => {
          let editDto: OrganizationUpdateDTO = DTOConverter.extractURIFromResourceProperties(http.response.result);
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

  private onDeleteClick(dto: OrganizationDagDTO) {
    this.service
        .deleteOrganization(dto.uri)
        .then(() => {
          this.refresh();
          this.$opensilex.showSuccessToast(dto.name+" "+this.$i18n.t("component.common.success.delete-success-message"))
        })
        .catch(this.$opensilex.errorHandler);
  }

  /**
   * allow us to load Organizations for the parent filter only when the user click on the filter and not when the component is created.
   */
  @Watch('SearchFiltersToggle')
  private watchSearchFiltersToggle(TogleValue: boolean) {
    if (TogleValue) {
      this.refreshOrganizationsForParentFilter();
    }
  }

  //#endregion

  //#region Public methods
  public refresh(): void {
    this.fetchOrganization()

    if(this.SearchFiltersToggle && this.allOrganizations !== []){
      this.refreshOrganizationsForParentFilter();
    }
  }

  //#endregion

  //#region Hooks
  private created() {
    this.service = this.$opensilex.getService("opensilex-core.OrganizationsService");
    this.fetchOrganization()
  }

  //#endregion

  //#region Private methods
  private async fetchOrganization() {
    this.$opensilex.enableLoader();
    this.$opensilex.showLoader();

    const httpResult: HttpResponse<OpenSilexResponse<OrganizationDagDTO[]>> = await this.service.searchOrganizations(this.filter.name, this.organizationsToFetch, this.filter.type_uri, this.filter.direct_child_uri, this.filter.facility);
    let result = httpResult.response.result;

    if (this.filter.direct_parent_uri != null) {
      const parent_organization = this.allOrganizations.find((organization) => organization.uri === this.filter.direct_parent_uri);
      result = result.filter((organization) => parent_organization.children.includes(organization.uri));
    }

    this.organizations = result

    this.$opensilex.hideLoader();
    this.$opensilex.disableLoader();
  }

  private getEmptyFilter(): OrganizationListFilter {
    return {
      name: undefined,
      type_uri: undefined,
      direct_child_uri: undefined,
      direct_parent_uri: undefined,
      facility: undefined
    };
  }

  private reset() {
    this.filter = this.getEmptyFilter();
    this.refresh();
  }

  private refreshOrganizationsForParentFilter(): void{
    this.service.searchOrganizations().then(
        (http: HttpResponse<OpenSilexResponse<Array<OrganizationDagDTO>>>) => {
          this.allOrganizations = http.response.result;
        }
    ).catch(this.$opensilex.errorHandler);
  }

  get parentOptions() {
    return this.$opensilex.buildTreeFromDag(this.allOrganizations);
  }

  //#endregion
}
</script>

<style scoped lang="scss">
#page-container {
  display: flex;
}

#page-container > .searchMenuContainer {
  width: 25px;
}

#page-container > #filter-side-bar {
  width: fit-content;
}

#page-container>div{
  width: 100%;
}
</style>

<i18n>
en:
  OrganizationList:
    filter:
        name-placeholder: "Filter by name"
        type-placeholder: "Filter by type"
        parent-organizations: "Parents of"
        parent-organizations-help: "Only direct parents of this organization will be displayed"
        parent-organizations-placeholder: "Filter by child organization"
        child-organizations: "Children of"
        child-organizations-help: "Only direct children of this organization will be displayed"
        child-organizations-placeholder: "Filter by parent organization"
    facilities: "Facilities"
fr:
  OrganizationList:
    filter:
        name-placeholder: "Filtrer par nom"
        type-placeholder: "Filtrer par type"
        parent-organizations: "Parents de"
        parent-organizations-help: "Seuls les parents directs de cette organisation seront affichés"
        parent-organizations-placeholder: "Filtrer par organisation enfant"
        child-organizations: "Enfants de"
        child-organizations-help: "Seuls les enfants directs de cette organisation seront affichés"
        child-organizations-placeholder: "Filtrer par organisation parent"
    facilities: "Installations"
</i18n>