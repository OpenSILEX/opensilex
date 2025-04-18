<template>
  <opensilex-Card
      icon="ik#ik-clipboard"
      :label="$t('component.common.informations')"
      class="facilityDetailComponent"
      v-if="selected"
  >
    <template v-slot:rightHeader v-if="withActions">
      <b-button-group>
        <opensilex-EditButton
            v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_FACILITY_MODIFICATION_ID
                  )
                "
            @click="editOrganizationFacility()"
            label="FacilitiesView.update"
            :small="true"
        ></opensilex-EditButton>
        <opensilex-DeleteButton
            v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_FACILITY_DELETE_ID
                  )
                "
            @click="deleteOrganizationFacility()"
            label="FacilitiesView.delete"
            :small="true"
        ></opensilex-DeleteButton>
      </b-button-group>
      <opensilex-FacilityModalForm
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_FACILITY_MODIFICATION_ID
            )
          "
          ref="organizationFacilityForm"
          @onUpdate="refresh"
      ></opensilex-FacilityModalForm>
    </template>

    <template v-slot:body>
      <!-- URI -->
      <opensilex-UriView
          :uri="selected.uri"
          :value="selected.uri"
      >
      </opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
          :value="selected.name"
          label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
          :type="selected.rdf_type"
          :typeLabel="selected.rdf_type_name"
      ></opensilex-TypeView>

      <!-- Organisations -->
      <opensilex-UriListView
          v-if="hasOrganizations"
          label="FacilityDescription.organizations"
          :list="organizationUriList"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Site -->
      <opensilex-UriListView
          v-if="hasSites"
          label="FacilityDescription.site"
          :list="siteUriList"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Experiments -->
      <opensilex-UriListView
          v-if="hasExperiments"
          label="FacilityDescription.expsInProgress"
          :list="experimentUriList"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- VariableGroups -->
      <opensilex-UriListView
          v-if="hasVariableGroups"
          label="FacilityDescription.variable-groups"
          :list="variableGroupUriList"
          :inline="true"
      >
      </opensilex-UriListView>

      <!-- Devices -->
      <opensilex-UriListView
          v-if="hasDevices"
          label="FacilityDescription.devices"
          :list="deviceUriList"
          :inline="true"
      >
      </opensilex-UriListView>

      <!-- Geometry -->
      <opensilex-GeometryCopy
        v-if="selected.geometry"
        :value="selected.geometry"
      ></opensilex-GeometryCopy>

      <!-- Address -->
      <opensilex-AddressView
          v-if="selected.address"
          :address="selected.address"
      >
      </opensilex-AddressView>

      <opensilex-OntologyObjectProperties
        :selected="selected"
        :parentType="oeso.FACILITY_TYPE_URI"
        :relations="selected.relations"
      >
      </opensilex-OntologyObjectProperties>

      <!-- Metadata -->
      <opensilex-MetadataView
        v-if="selected.publisher && selected.publisher.uri"
        :publisher="selected.publisher"
        :publicationDate="selected.publication_date"
        :lastUpdatedDate="selected.last_updated_date" 
      ></opensilex-MetadataView>
    </template>
  </opensilex-Card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import Vue from "vue";
import {Prop, Ref, Watch} from "vue-property-decorator";
import DTOConverter from "../../../models/DTOConverter";
import { FacilityGetDTO } from 'opensilex-core/index';
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class FacilityDescription extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  selected: FacilityGetDTO;
  @Prop()
  experiments: Array<ExperimentGetListDTO>;
  @Prop()
  devices: Array<DeviceGetDTO>;

  @Prop({
    default: false
  })
  withActions: boolean;

  typeProperties = [];
  valueByProperties: any = {};
  classModel: any = {};

  @Ref("organizationFacilityForm") readonly organizationFacilityForm!: any;

  created() {

  }

  refresh() {
    this.$emit("onUpdate");
  }

  get oeso() {
      return this.$opensilex.Oeso;
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get hasOrganizations() {
    return !!this.selected && this.selected.organizations.length > 0;
  }

  get hasExperiments() {
    return !!this.experiments && this.experiments.length > 0;
  }

  get hasVariableGroups() {
    return !!this.selected && this.selected.variableGroups.length > 0;
  }

  get hasDevices() {
    return !!this.devices && this.devices.length > 0;
  }

  get hasSites() {
    return !!this.selected && this.selected.sites.length > 0;
  }

  get organizationUriList() {
    if (!this.selected) {
      return [];
    }

    return this.selected.organizations.map(org => {
      return {
        uri: org.uri,
        value: org.name,
        to: {
          path: "/organization/details/" + encodeURIComponent(org.uri),
        },
      };
    });
  }

  get experimentUriList() {
    if (!this.experiments) {
      return [];
    }

    return this.experiments.map(exp => {
      return {
        uri: exp.uri,
        value: exp.name,
        to: {
          path: "/experiment/details/" + encodeURIComponent(exp.uri),
        },
      };
    });
  }

  get deviceUriList() {
    if (!this.devices) {
      return [];
    }

    return this.devices.map(device => {
      return {
        uri: device.uri,
        value: device.name,
        to: {
          path: "/device/details/" + encodeURIComponent(device.uri),
        },
      };
    });
  }

  get siteUriList() {
    if (!this.selected) {
      return [];
    }
    return this.selected.sites.map(site => {
      return {
        uri: site.uri,
        value: site.name,
        to: {
          path: "/organization/site/details/" + encodeURIComponent(site.uri),
        },
      };
    });
  }

  get variableGroupUriList() {
    if (!this.selected) {
      return [];
    }
    return this.selected.variableGroups.map(varGroup => {
      return {
        uri: varGroup.uri,
        value: varGroup.name,
        to: {
          path: "/variables?elementType=VariableGroup&selected=" + encodeURIComponent(varGroup.uri),
        },
      };
    });
  }

  editOrganizationFacility() {
    this.organizationFacilityForm.showEditForm(this.selected.uri);
  }

  deleteOrganizationFacility() {
    this.$opensilex
        .getService<OrganizationsService>("opensilex.OrganizationsService")
        .deleteFacility(this.selected.uri)
        .then(() => {
          this.$router.push({
            path: "/organizations",
          });
        })
        .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped>
      @media only screen and (min-width: 768px) {
  .facilityDetailComponent {
    margin-top: 30px;
  }
}

</style>

<i18n>
en:
  FacilityDescription:
    organizations: Organizations
    expsInProgress: Experiments in progress
    variable-groups: Groups of variables
    devices: Devices
    site: "Site"
    address: "Address"

fr:
  FacilityDescription:
    organizations: Organisations
    expsInProgress: Experiences en cours
    variable-groups: Groupes de variables
    devices: Appareils
    site: "Site"
    address: "Adresse"
</i18n>