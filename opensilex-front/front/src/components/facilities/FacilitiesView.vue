<!--
  - ******************************************************************************
  -                         FacilitiesView.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 14/06/2024 13:32
  - Contact: yvan.roux@inrae.fr
  - ******************************************************************************
  -->

<template>
  <div>
    <!--CreateButton position on top for FacilityListView-->
    <div class="spaced-actions" v-if="withActions" >
      <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
          @click="facilityForm.showCreateForm()"
          :label="createButtonLabel"
          class="createButton"
      ></opensilex-CreateButton>
    </div>

  <b-card>
    <template v-slot:header>
      <h3>
        {{ $t("FacilitiesView.facilities") }}

        <!--CreateButton position on card for OrganizationView-->
        <span v-if="!withActions">
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
          @click="facilityForm.showCreateForm()"
          label="FacilitiesView.add"
          class="createButton"
        ></opensilex-CreateButton>
        </span>
        &nbsp;
        <font-awesome-icon
          icon="question-circle"
          class="facilitiesHelp"
          v-b-tooltip.hover.top="$t('FacilitiesView.facility-help')"
        />
      </h3>
    </template>


      <!--           Facilities table : if fetchAndShowCurrentExperiments then we want to sort by default on experiment_count in desc order-->
      <opensilex-TableView
          :items="displayableFacilities"
          :fields="fields"
          :globalFilterField="true"
          filterPlaceholder="component.facility.filter-placeholder"
          :sortBy="fetchAndShowCurrentExperiments ? 'experiment_count' : 'rdf_type_name'"
          :sortDesc="fetchAndShowCurrentExperiments"
          :selectable="isSelectable"
          @row-selected="onFacilitySelected"
          ref="tableView"
      >
        <template v-slot:cell(name)="{data}">
          <opensilex-UriLink
              :to="{path:'/facility/details/' +encodeURIComponent(data.item.uri),}"
              :uri="data.item.uri"
              :value="data.item.name"
          ></opensilex-UriLink>
        </template>

        <template v-slot:cell(experiment_count)="{data}" v-if="fetchAndShowCurrentExperiments">
          <opensilex-ExperimentsModalList
              :experiments="experimentByFacility[data.item.uri]"
              :currentFacility="data.item"
          />
        </template>

        <template v-slot:cell(rdf_type_name)="{data}">
                    <span class="capitalize-first-letter">{{
                        data.item.rdf_type_name
                      }}</span>
        </template>

        <template v-slot:cell(actions)="{data}" v-if="withActions">
          <b-button-group size="sm">
            <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
                @click="editFacility(data.item)"
                label="FacilitiesView.update"
                :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_FACILITY_DELETE_ID)"
                @click="deleteFacility(data.item.uri)"
                label="FacilitiesView.delete"
                :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
        </template>
      </opensilex-TableView>

      <opensilex-FacilityModalForm
          ref="facilityForm"
          v-if="withActions && user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
          @onCreate="onCreate"
          @onUpdate="onUpdate"
          :initForm="initForm"
      ></opensilex-FacilityModalForm>
    </b-card>
  </div>
</template>


<script lang="ts">

import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import FacilityModalForm from "./FacilityModalForm.vue";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {
  FacilityCreationDTO,
  FacilityGetDTO,
  NamedResourceDTOFacilityModel,
  NamedResourceDTOOrganizationModel,
  NamedResourceDTOSiteModel
} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../models/Store";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";
import {ExperimentsService} from "opensilex-core/api/experiments.service";

/**
 * a type that is the same as NamedResourceDTOFacilityModel but with an additional field experiment_count.
 * without it, the table can't sort by experiment_count.
 */
interface FacilityModelWithExperimentCount extends NamedResourceDTOFacilityModel {
  experiment_count: number;
}

@Component
export default class FacilitiesView extends Vue {
  //#region Plugin & Services
  public $opensilex: OpenSilexVuePlugin;
  public $store: OpenSilexStore;
  private service: OrganizationsService;
  private experimentsService: ExperimentsService;
  //#endregion

  //#region Props
  @Prop()
  private readonly organization: NamedResourceDTOOrganizationModel;

  @Prop()
  private readonly site: NamedResourceDTOSiteModel;

  /**
   * Facilities as prop. If defined, then these facilities only are displayed in the view. Otherwise, facilities are
   * fetched from the server.
   */
  @Prop()
  private readonly facilities: Array<NamedResourceDTOFacilityModel>;

  @Prop({default: false})
  private readonly isSelectable;

  @Prop({default: false})
  private readonly withActions: boolean;

  @Prop({default: false})
  private readonly fetchAndShowCurrentExperiments: boolean;

  @Prop({default: "FacilitiesView.add"})
  private createButtonLabel: string;
  //#endregion

  //#region Refs
  @Ref("facilityForm") private readonly facilityForm!: FacilityModalForm;

  @Ref("tableView") private readonly tableView;

  //#endregion

  //#region Data
  /**
   * Facilities fetched from the server, only if {@link facilities} is undefined.
   */
  private fetchedFacilities: Array<NamedResourceDTOFacilityModel> = [];

  private currentExperiments: Array<ExperimentGetListDTO> = [];

  private filter: string = "";
  //#endregion

  //#region Computed
  private get user() {
    return this.$store.state.user;
  }

  private get credentials() {
    return this.$store.state.credentials;
  }

  private get useFetchedFacilities() {
    return !Array.isArray(this.facilities);
  }

  /**
   * the list of facilities to display in the table.
   * If {@link useFetchedFacilities} is true, then fetchedFacilities are displayed.
   * If fetchAndShowCurrentExperiments is true, then the experiment_count field is added to the facilities.
   */
  private get displayableFacilities() {
    if (this.useFetchedFacilities) {
      return this.fetchedFacilities.map(facility => this.addExperimentCountIfNecessary(facility));
    }
    let facilitiesWithExperimentCount = this.facilities.map(facility => this.addExperimentCountIfNecessary(facility));
    return !this.filter
        ? facilitiesWithExperimentCount
        : facilitiesWithExperimentCount.filter(facility => facility.name.match(new RegExp(this.filter, "i")));
  }

  private addExperimentCountIfNecessary(facility: NamedResourceDTOFacilityModel) {
    if (!this.fetchAndShowCurrentExperiments) {
      return facility;
    }
    return {
      ...facility,
      experiment_count: this.experimentByFacility[facility.uri] ? this.experimentByFacility[facility.uri].length : 0
    }
  }

  /**
   * Get the list of experiments for each facility with facility uri as key.
   */
  private get experimentByFacility(): { [key: string]: Array<ExperimentGetListDTO> } {
    let experimentsByFacility = {};
    this.currentExperiments.forEach(experiment => {
      experiment.facilities.forEach(facilityUri => {
        if (!experimentsByFacility[facilityUri]) {
          experimentsByFacility[facilityUri] = [];
        }
        experimentsByFacility[facilityUri].push(experiment);
      });
    });
    return experimentsByFacility;
  }

  private get fields() {
    let fields = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      },
    ];
    if (this.fetchAndShowCurrentExperiments) {
      fields.push({
        key: "experiment_count",
        label: "FacilitiesView.experiment_count",
        sortable: true,
      });
    }
    fields.push({
      key: "rdf_type_name",
      label: "component.common.type",
      sortable: true,
    });
    if (this.withActions) {
      fields.push({
        label: "component.common.actions",
        key: "actions",
        sortable: false
      });
    }
    return fields;
  }

  //#endregion

  //#region Events
  private onFacilitySelected(selected: FacilityGetDTO) {
    this.$emit('facilitySelected', selected);
  }

  private onUpdate() {
    this.$emit("onUpdate");
  }

  private onCreate() {
    this.$emit("onCreate");
  }

  //#endregion

  //#region Public methods
  public deleteFacility(uri) {
    this.$opensilex
        .getService<OrganizationsService>("opensilex.OrganizationsService")
        .deleteFacility(uri)
        .then(() => {
          let message = this.$i18n.t("organizationFacilityForm.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
          this.$opensilex.showSuccessToast(message);
          this.$emit("onDelete", uri);
        });
  }

  public async refresh(): Promise<void> {
    this.$opensilex.showLoader();
    if (this.useFetchedFacilities) {
      await this.fetchFacilities();
    }
    if (this.fetchAndShowCurrentExperiments) {
      await this.fetchExperiments();
    }

    // Select the first element
    if (this.isSelectable && this.displayableFacilities.length > 0) {
      this.tableView.selectRow(0);
    }
    this.$opensilex.hideLoader();
  }

  //#endregion

  //#region Hooks
  private created() {
    this.service = this.$opensilex.getService("opensilex-core.OrganizationsService");
    this.experimentsService = this.$opensilex.getService("opensilex-core.ExperimentsService");
    this.refresh();
  }

  //#endregion

  //#region Private methods
  private fetchFacilities(): Promise<void> {
    return this.service.minimalSearchFacilities(this.filter)
        .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
          this.fetchedFacilities = http.response.result;
        })
  }

  private fetchExperiments(): Promise<void> {
    return this.experimentsService
        .searchExperiments(
            "", // label
            undefined, // year
            null, // is_ended
            null, // species
            null, // factorCategories
            null, // projects
            null, // isPublic
            this.displayableFacilities.map(facility => facility.uri), // facilities
            null,
            0,
            0
        )
        .then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => {
          const experiments = http.response.result;
          this.currentExperiments = experiments.filter(experiment => !this.experimentIsEnded(experiment))
        });
  }

  private experimentIsEnded(experiment: ExperimentGetListDTO): boolean {
    return experiment.end_date && new Date(experiment.end_date) < new Date();
  }

  private initForm(form: FacilityCreationDTO) {
    if (this.organization) {
      form.organizations = [this.organization.uri];
    }
    if (this.site) {
      form.sites = [this.site.uri];
    }
  }

  private editFacility(facility: FacilityGetDTO) {
    this.facilityForm.showEditForm(facility.uri);
  }
}
</script>

<style scoped lang="scss">

.facilitiesHelp {
  font-size: 1.3em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
}

.spaced-actions {
  margin-top: -15px;
  margin-bottom: 10px;
}

</style>

<i18n>
en:
  FacilitiesView:
    update: Update facility
    delete: Delete facility
    add: Add facility
    facilities: Facilities
    facility-help: "Factilities correspond to the various fixed installations of an organization.
                                  These can be greenhouses, fields, culture chambers, growth chambers ..."
    experiment_count: "current experiments"
fr:
  FacilitiesView:
    update: Modifier l'installation environnementale
    delete: Supprimer l'installation environnementale
    add: Ajouter une installation environnementale
    facilities: Installations environnementales
    facility-help: "Les installations environnementales correspondent aux différentes installations fixes d'une organisation.
                                  Il peut s'agir de serres, champs, chambres de culture, chambres de croissance ..."
    experiment_count: "expériences en cours"
</i18n>
