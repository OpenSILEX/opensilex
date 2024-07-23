<template>
  <div>
    <!--CreateButton position on top for FacilityListView-->
    <div class="spaced-actions" v-if="withActions" >
      <opensilex-CreateButton
        v-if="
          user.hasCredential(credentials.CREDENTIAL_FACILITY_MODIFICATION_ID)"
        @click="facilityForm.showCreateForm()"
        label="FacilitiesView.add"
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

    <opensilex-StringFilter
        :filter.sync="filter"
        @update="refresh()"
        placeholder="component.facility.filter-placeholder"
        :debounce="300"
        :lazy="false"
    ></opensilex-StringFilter>

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
      class="scrollable-container"
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
              '/facility/details/' +
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
            label="FacilitiesView.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_FACILITY_DELETE_ID
              )
            "
            @click="deleteFacility(data.item.uri)"
            label="FacilitiesView.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </b-table>

    <opensilex-FacilityModalForm
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
    ></opensilex-FacilityModalForm>
  </b-card>
</div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {BTable} from "bootstrap-vue";
import FacilityModalForm from "./FacilityModalForm.vue";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {FacilityCreationDTO,
  NamedResourceDTOFacilityModel, NamedResourceDTOOrganizationModel, NamedResourceDTOSiteModel } from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import DTOConverter from "../../models/DTOConverter";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

@Component
export default class FacilitiesView extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  service: OrganizationsService;

  @Ref("facilityForm") readonly facilityForm!: FacilityModalForm;
  @Ref("facilityTable") readonly facilityTable: BTable;

  @Prop()
  organization: NamedResourceDTOOrganizationModel;

  @Prop()
  site: NamedResourceDTOSiteModel;

  /**
   * Facilities as prop. If defined, then these facilities only are displayed in the view. Otherwise, facilities are
   * fetched from the server.
   */
  @Prop()
  facilities: Array<NamedResourceDTOFacilityModel>;
  /**
   * Facilities fetched from the server, only if {@link facilities} is undefined.
   */
  fetchedFacilities: Array<NamedResourceDTOFacilityModel> = [];
  selectedFacility: NamedResourceDTOFacilityModel = undefined;

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false
  })
  withActions: boolean;

  filter: string = "";

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get displayableFacilities() {
    if (Array.isArray(this.facilities)) {
      return !this.filter
          ? this.facilities
          : this.facilities.filter(facility => facility.name.match(new RegExp(this.filter, "i")));
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
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .deleteFacility(uri)
      .then(() => {
        let message = this.$i18n.t("organizationFacilityForm.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$emit("onDelete", uri);
      });
  }

  onFacilitySelected(selected: Array<NamedResourceDTO>) {
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

      this.$opensilex.showLoader();
      return this.service.minimalSearchFacilities(this.filter)
        .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
          this.fetchedFacilities = http.response.result;
        }).then(() => {

          // Select the first element
          if (this.isSelectable && this.fetchedFacilities.length > 0) {
            this.facilityTable.selectRow(0);
          }
        }).finally( () => this.$opensilex.hideLoader());
  }

  initForm(form: FacilityCreationDTO) {
    if (this.organization) {
      form.organizations = [this.organization.uri];
    }
    if (this.site) {
      form.sites = [this.site.uri];
    }
  }

  createFacility() {
    this.facilityForm.showCreateForm()
  }

  editFacility(facility: NamedResourceDTO) {
    this.facilityForm.showEditForm(facility.uri);
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

.facilitiesHelp{
  font-size: 1.3em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
}

.spaced-actions {
  margin-top: -15px;
  margin-bottom: 10px;
}

.scrollable-container {
    width: 100%;
    height: 600px;
    overflow-y: auto; /* Enables vertical scrolling */
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
fr:
  FacilitiesView:
    update: Modifier l'installation environnementale
    delete: Supprimer l'installation environnementale
    add: Ajouter une installation environnementale
    facilities: Installations environnementales
    facility-help: "Les installations environnementales correspondent aux différentes installations fixes d'une organisation.
                                  Il peut s'agir de serres, champs, chambres de culture, chambres de croissance ..."
</i18n>
