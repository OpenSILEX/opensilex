<template>
  <b-card>
    <template v-slot:header>
      <h3>
        {{ $t("InfrastructureFacilitiesView.facilities") }}
        &nbsp;
        <font-awesome-icon
          icon="question-circle"
          v-b-tooltip.hover.top="
            $t('InfrastructureFacilitiesView.infrastructure-facility-help')
          "
        />
      </h3>
      <div class="card-header-right" v-if="withActions">
        <opensilex-CreateButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_FACILITY_MODIFICATION_ID
            )
          "
          @click="facilityForm.showCreateForm()"
          label="InfrastructureFacilitiesView.add"
        ></opensilex-CreateButton>
      </div>
    </template>

    <b-table
      striped
      hover
      small
      responsive
      sort-icon-left
      sort-by="rdf_type_name"
      :selectable="isSelectable"
      selectMode="single"
      :items="displayableFacilities"
      :fields="fields"
      @row-selected="onFacilitySelected"
      ref="facilityTable"
    >
      <template v-slot:head(name)="data">{{ $t(data.label) }}</template>
      <template v-slot:head(rdf_type_name)="data">{{
        $t(data.label)
      }}</template>
      <template v-slot:head(actions)="data" v-if="withActions">{{ $t(data.label) }}</template>

      <template v-slot:cell(name)="data">
        <opensilex-UriLink
          :to="{
            path:
              '/infrastructure/facility/details/' +
              encodeURIComponent(data.item.uri),
          }"
          :uri="data.item.uri"
          :value="data.item.name"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(rdf_type_name)="data">
        <span class="capitalize-first-letter">{{
          data.item.rdf_type_name
        }}</span>
      </template>

      <template v-slot:cell(actions)="data" v-if="withActions">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_FACILITY_MODIFICATION_ID
              )
            "
            @click="editFacility(data.item)"
            label="InfrastructureFacilitiesView.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_FACILITY_DELETE_ID
              )
            "
            @click="deleteFacility(data.item.uri)"
            label="InfrastructureFacilitiesView.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </b-table>

    <opensilex-OrganizationFacilityModalForm
      ref="facilityForm"
      v-if="
        withActions &&
        user.hasCredential(
          credentials.CREDENTIAL_FACILITY_MODIFICATION_ID
        )
      "
      @onCreate="onCreate"
      @onUpdate="onUpdate"
      :initForm="initForm"
    ></opensilex-OrganizationFacilityModalForm>
  </b-card>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {InfrastructureFacilityGetDTO} from "opensilex-core/model/infrastructureFacilityGetDTO";
import {BTable} from "bootstrap-vue";
import { NamedResourceDTOInfrastructureFacilityModel } from "opensilex-core/model/namedResourceDTOInfrastructureFacilityModel";
import {InfrastructureFacilityCreationDTO} from "opensilex-core/model/infrastructureFacilityCreationDTO";
import OrganizationFacilityModalForm from "./OrganizationFacilityModalForm.vue";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {NamedResourceDTOInfrastructureModel} from "opensilex-core/model/namedResourceDTOInfrastructureModel";
import {NamedResourceDTOSiteModel} from "opensilex-core/model/namedResourceDTOSiteModel";

@Component
export default class InfrastructureFacilitiesView extends Vue {
  $opensilex: any;
  $store: any;
  service: OrganizationsService;

  @Ref("facilityForm") readonly facilityForm!: OrganizationFacilityModalForm;
  @Ref("facilityTable") readonly facilityTable: BTable;

  @Prop()
  organization: NamedResourceDTOInfrastructureModel;

  @Prop()
  site: NamedResourceDTOSiteModel;

  /**
   * Facilities as prop. If defined, then these facilities only are displayed in the view. Otherwise, facilities are
   * fetched from the server.
   */
  @Prop()
  facilities: Array<NamedResourceDTOInfrastructureFacilityModel>;
  /**
   * Facilities fetched from the server, only if {@link facilities} is undefined.
   */
  fetchedFacilities: Array<NamedResourceDTOInfrastructureFacilityModel> = [];
  selectedFacility: NamedResourceDTOInfrastructureFacilityModel = undefined;

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false
  })
  withActions: boolean;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get displayableFacilities() {
    if (Array.isArray(this.facilities)) {
      return this.facilities;
    }
    return this.fetchedFacilities;
  }

  get fields() {
    let fields = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      },
      {
        key: "rdf_type_name",
        label: "component.common.type",
        sortable: true,
      }
    ];
    if (this.withActions) {
      fields.push({
        label: "component.common.actions",
        key: "actions",
        sortable: false
      });
    }
    return fields;
  }

  public deleteFacility(uri) {
    this.$opensilex
      .getService("opensilex.OrganizationsService")
      .deleteInfrastructureFacility(uri)
      .then(() => {
        this.$emit("onDelete", uri);
      });
  }

  onFacilitySelected(selected: Array<InfrastructureFacilityGetDTO>) {
    this.selectedFacility = selected[0];
    this.$emit('facilitySelected', this.selectedFacility);
  }

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-core.OrganizationsService"
    );

    this.refresh();
  }

  refresh() {
    if (Array.isArray(this.facilities)) {
      return;
    }

    return this.service.searchInfrastructureFacilities()
        .then((http: HttpResponse<OpenSilexResponse<Array<InfrastructureFacilityGetDTO>>>) => {
          this.fetchedFacilities = http.response.result;
        }).then(() => {

          // Select the first element
          if (this.isSelectable && this.fetchedFacilities.length > 0) {
            this.facilityTable.selectRow(0);
          }
        });
  }

  initForm(form: InfrastructureFacilityCreationDTO) {
    if (this.organization) {
      form.organizations = [this.organization.uri];
    }
  }

  createFacility() {
    this.facilityForm.showCreateForm()
  }

  editFacility(facility: InfrastructureFacilityGetDTO) {
    this.facilityForm.showEditForm(facility);
  }

  onUpdate() {
    this.$emit("onUpdate");
  }

  onCreate() {
    this.$emit("onCreate");
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureFacilitiesView:
    update: Update facility
    delete: Delete facility
    add: Add facility
    facilities: Facilities
    infrastructure-facility-help: "Factilities correspond to the various fixed installations of an organization.
                                  These can be greenhouses, cadastral plots, culture chambers, ..."
fr:
  InfrastructureFacilitiesView:
    update: Modifier l'installation technique
    delete: Supprimer l'installation technique
    add: Ajouter une installation technique
    facilities: Installations techniques
    infrastructure-facility-help: "Les installations techniques correspondent aux différentes installations fixes d'une organisation.
                                  Il peut s'agir de serres, parcelles cadastrales, chambres de culture, ..."
</i18n>
