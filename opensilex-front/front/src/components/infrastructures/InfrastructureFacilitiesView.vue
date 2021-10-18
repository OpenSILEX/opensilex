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
      <div class="card-header-right">
        <opensilex-CreateButton
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
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
      :items="facilities"
      :fields="fields"
      @row-selected="onFacilitySelected"
    >
      <template v-slot:head(name)="data">{{ $t(data.label) }}</template>
      <template v-slot:head(rdf_type_name)="data">{{
        $t(data.label)
      }}</template>
      <template v-slot:head(actions)="data">{{ $t(data.label) }}</template>

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

      <template v-slot:cell(actions)="data">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
              )
            "
            @click="editFacility(data.item)"
            label="InfrastructureFacilitiesView.update"
            :small="true"
          ></opensilex-EditButton>
          <opensilex-DeleteButton
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
              )
            "
            @click="deleteFacility(data.item.uri)"
            label="InfrastructureFacilitiesView.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </b-table>

    <opensilex-InfrastructureFacilityForm
      ref="facilityForm"
      v-if="
        user.hasCredential(
          credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
        )
      "
      @onCreate="$emit('onCreate', $event)"
      @onUpdate="$emit('onUpdate', $event)"
    ></opensilex-InfrastructureFacilityForm>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { InfrastructureGetDTO } from "opensilex-core/index";
import {OrganisationsService} from "opensilex-core/api/organisations.service";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {InfrastructureFacilityNamedDTO} from "opensilex-core/model/infrastructureFacilityNamedDTO";
import {InfrastructureFacilityGetDTO} from "opensilex-core/model/infrastructureFacilityGetDTO";

@Component
export default class InfrastructureFacilitiesView extends Vue {
  $opensilex: any;
  $store: any;
  service: OrganisationsService;

  @Ref("facilityForm") readonly facilityForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop({
    default: false,
  })
  isSelectable;

  fields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true,
    },
    {
      key: "rdf_type_name",
      label: "component.common.type",
      sortable: true,
    },
    {
      label: "component.common.actions",
      key: "actions",
    },
  ];

  facilities: Array<InfrastructureFacilityGetDTO> = [];
  selectedFacility: InfrastructureFacilityGetDTO = undefined;

  public deleteFacility(uri) {
    this.$opensilex
      .getService("opensilex.OrganisationsService")
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
        "opensilex-core.OrganisationsService"
    );

    this.refresh();
  }

  refresh() {
    this.service
        .searchInfrastructureFacilities()
        .then((http: HttpResponse<OpenSilexResponse<Array<InfrastructureFacilityGetDTO>>>) => {
          console.log("Refresh facilities", http.response);
          this.facilities = http.response.result
        });
  }

  editFacility(facility) {
    let copy = JSON.parse(JSON.stringify(facility));
    this.facilityForm.showEditForm(copy);
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
                                  Il peux s'agir de serres, parcelles cadastrales, chambres de culture, ..."
</i18n>
