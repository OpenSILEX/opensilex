<template>
  <opensilex-Card
      icon="ik#ik-clipboard"
      :label="$t('component.common.description')"
      class="facilityDetailComponent"
  >
    <template v-slot:rightHeader v-if="withActions">
      <b-button-group>
        <opensilex-EditButton
            v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_FACILITY_MODIFICATION_ID
                  )
                "
            @click="editInfrastructureFacility()"
            label="FacilitiesView.update"
            :small="true"
        ></opensilex-EditButton>
        <opensilex-DeleteButton
            v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_FACILITY_DELETE_ID
                  )
                "
            @click="deleteInfrastructureFacility()"
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
          ref="infrastructureFacilityForm"
          @onUpdate="refresh"
      ></opensilex-FacilityModalForm>
    </template>

    <template v-slot:body>
      <!-- URI -->
      <opensilex-UriView
          :uri="selectedFacilityOrDefault.uri"
          :value="selectedFacilityOrDefault.uri"
          :to="{
            path: '/facility/details/' + encodeURIComponent(selectedFacilityOrDefault.uri),
          }"
      >
      </opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
          :value="selectedFacilityOrDefault.name"
          label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
          :type="selectedFacilityOrDefault.rdf_type"
          :typeLabel="selectedFacilityOrDefault.rdf_type_name"
      ></opensilex-TypeView>

      <!-- Organisations -->
      <opensilex-UriListView
          v-if="hasOrganizations"
          label="FacilityDetail.organizations"
          :list="organizationUriList"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Site -->
      <opensilex-UriListView
          v-if="hasSites"
          label="FacilityDetail.site"
          :list="siteUriList"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Experiments -->
      <opensilex-UriListView
          v-if="hasExperiments"
          label="FacilityDetail.expsInProgress"
          :list="experimentUriList"
          :inline="false"
      >
      </opensilex-UriListView>

      <!-- Devices -->
      <opensilex-UriListView
          v-if="hasDevices"
          label="FacilityDetail.devices"
          :list="deviceUriList"
          :inline="true"
      >
      </opensilex-UriListView>

      <!-- Address -->
      <opensilex-AddressView
          v-if="selectedFacilityOrDefault.address"
          :address="selectedFacilityOrDefault.address"
          :geometry="selectedFacilityOrDefault.geometry"
          noGeometryLabel="FacilityDetail.noGeometryWarning"
      >
      </opensilex-AddressView>

      <div>
        <div v-for="(v, index) in typeProperties" v-bind:key="index">
          <div v-if="!Array.isArray(v.property)" class="static-field">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <component
                :is="v.definition.view_component"
                :value="v.property"
            ></component>
          </div>

          <div
              v-else-if="v.property && v.property.length > 0"
              class="static-field"
          >
            <span class="field-view-title">{{ v.definition.name }}</span>
            <ul>
              <br />
              <li
                  v-for="(prop, propIndex) in v.property"
                  v-bind:key="propIndex"
              >
                <component
                    :is="v.definition.view_component"
                    :value="prop"
                ></component>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>
  </opensilex-Card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import Vue from "vue";
import {Prop, Ref, Watch} from "vue-property-decorator";
import DTOConverter from "../../models/DTOConverter";
import { FacilityGetDTO } from 'opensilex-core/index';
import {ExperimentGetListDTO} from "opensilex-core/model/experimentGetListDTO";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {VueJsOntologyExtensionService} from "../../lib";

@Component
export default class FacilityDetail extends Vue {
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

  @Ref("infrastructureFacilityForm") readonly infrastructureFacilityForm!: any;

  created() {

  }

  refresh() {
    this.$emit("onUpdate");
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get selectedFacilityOrDefault() {
    if (this.selected) {
      return this.selected;
    }
    return {};
  }

  get hasOrganizations() {
    return !!this.selected && this.selected.organizations.length > 0;
  }

  get hasExperiments() {
    return !!this.experiments && this.experiments.length > 0;
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
          path: "/infrastructure/details/" + encodeURIComponent(org.uri),
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
          path: "/infrastructure/site/details/" + encodeURIComponent(site.uri),
        },
      };
    });
  }

  editInfrastructureFacility() {
    this.infrastructureFacilityForm.showEditForm(DTOConverter.extractURIFromResourceProperties(this.selected));
  }

  deleteInfrastructureFacility() {
    this.$opensilex
        .getService<OrganizationsService>("opensilex.OrganizationsService")
        .deleteFacility(this.selected.uri)
        .then(() => {
          this.$router.push({
            path: "/infrastructures",
          });
        })
        .catch(this.$opensilex.errorHandler);
  }

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];
    this.valueByProperties = {};
    this.classModel = {};

    if (!this.selected) {
      return;
    }

    this.$opensilex
        .getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService")
        .getRDFTypeProperties(
            this.selected.rdf_type,
            this.$opensilex.Oeso.FACILITY_TYPE_URI
        )
        .then((result) => {
          this.classModel = result.response.result;

          let valueByProperties = this.buildValueByProperties(
              this.selected.relations
          );
          this.buildTypeProperties(this.typeProperties, valueByProperties);
          this.valueByProperties = valueByProperties;
        });
  }

  buildValueByProperties(relationArray) {
    let valueByProperties = {};
    for (let i in relationArray) {
      let relation = relationArray[i];

      if (
          valueByProperties[relation.property] &&
          !Array.isArray(valueByProperties[relation.property])
      ) {
        valueByProperties[relation.property] = [
          valueByProperties[relation.property],
        ];
      }

      if (Array.isArray(valueByProperties[relation.property])) {
        valueByProperties[relation.property].push(relation.value);
      } else {
        valueByProperties[relation.property] = relation.value;
      }
    }

    return valueByProperties;
  }

  buildTypeProperties(typeProperties, valueByProperties) {
    this.loadProperties(
        typeProperties,
        this.classModel.data_properties,
        valueByProperties
    );
    this.loadProperties(
        typeProperties,
        this.classModel.object_properties,
        valueByProperties
    );

    let pOrder = this.classModel.properties_order;

    typeProperties.sort((a, b) => {
      let aProp = a.definition.property;
      let bProp = b.definition.property;
      if (aProp == bProp) {
        return 0;
      }

      if (aProp == "rdfs:label") {
        return -1;
      }

      if (bProp == "rdfs:label") {
        return 1;
      }

      let aIndex = pOrder.indexOf(aProp);
      let bIndex = pOrder.indexOf(bProp);
      if (aIndex == -1) {
        if (bIndex == -1) {
          return aProp.localeCompare(bProp);
        } else {
          return -1;
        }
      } else {
        if (bIndex == -1) {
          return 1;
        } else {
          return aIndex - bIndex;
        }
      }
    });
  }

  loadProperties(typeProperties, properties, valueByProperties) {
    for (let i in properties) {
      let property = properties[i];

      if (valueByProperties[property.property]) {
        if (
            property.is_list &&
            !Array.isArray(valueByProperties[property.property])
        ) {
          typeProperties.push({
            definition: property,
            property: [valueByProperties[property.property]],
          });
        } else {
          typeProperties.push({
            definition: property,
            property: valueByProperties[property.property],
          });
        }
      }
    }
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
  FacilityDetail:
    organizations: Organizations
    expsInProgress: Experiments in progress
    devices: Devices
    site: "Site"
    address: "Address"
    noGeometryWarning: No geometry was associated with the address. Maybe the address is invalid.
fr:
  FacilityDetail:
    organizations: Organisations
    expsInProgress: Experiences en cours
    devices: Dispositifs
    site: "Site"
    address: "Adresse"
    noGeometryWarning: Aucune géométrie n'a pu être déterminée à partir de l'adresse. L'adresse est peut-être invalide.
</i18n>