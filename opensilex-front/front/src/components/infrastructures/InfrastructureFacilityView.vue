<template>
  <div class="container-fluid" v-if="selected">
    <opensilex-PageHeader
      icon="ik#ik-globe"
      :title="selected.name"
      :description="selected.rdf_type_name"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="false" :returnButton="true">
    </opensilex-PageActions>
    <div class="row">
      <div class="col-md-12">
        <opensilex-Card
          icon="ik#ik-clipboard"
          :label="$t('component.common.description')"
        >
          <template v-slot:rightHeader>
            <b-button-group>
              <opensilex-EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
                  )
                "
                @click="editInfrastructureFacility()"
                label="InfrastructureFacilitiesView.update"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID
                  )
                "
                @click="deleteInfrastructureFacility()"
                label="InfrastructureFacilitiesView.delete"
                :small="true"
              ></opensilex-DeleteButton>
            </b-button-group>
            <opensilex-InfrastructureFacilityForm
              ref="infrastructureFacilityForm"
              :infrastructure="selected.organisation"
              @onUpdate="refresh"
            ></opensilex-InfrastructureFacilityForm>
          </template>

          <template v-slot:body>
            <!-- URI -->
            <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
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
            <!-- Organisation -->
            <opensilex-InfrastructureUriView
              :uri="selected.organisation"
            ></opensilex-InfrastructureUriView>

            <div>
              <div v-for="(v, index) in typeProperties" v-bind:key="index">
                <div v-if="!Array.isArray(v.property)" class="static-field">
                  <span class="field-view-title">{{ v.definition.name }}</span>
                  <component
                    :is="v.definition.view_component"
                    :value="v.property"
                    :experiment="experiment"
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
                        :experiment="experiment"
                      ></component>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { InfrastructureGetDTO } from "opensilex-core/index";

@Component
export default class InfrastructureFacilityView extends Vue {
  $opensilex: any;

  selected = null;
  uri = null;
  service;

  @Ref("infrastructureFacilityForm") readonly infrastructureFacilityForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService(
      "opensilex-core.OrganisationsService"
    );
    this.refresh();
  }

  refresh() {
    this.service
      .getInfrastructureFacility(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.selected = detailDTO;
      });
  }

  editInfrastructureFacility() {
    this.infrastructureFacilityForm.showEditForm(this.selected);
  }

  deleteInfrastructure() {
    this.$opensilex
      .getService("opensilex.OrganisationsService")
      .deleteInfrastructureFacility(this.uri)
      .then(() => {
        this.$router.push({
          path: "/infrastructures",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  typeProperties = [];
  valueByProperties: any = {};
  classModel: any = {};

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];
    this.valueByProperties = {};
    this.classModel = {};

    this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .getRDFTypeProperties(
        this.selected.rdf_type,
        this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
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

<style scoped lang="scss">
</style>

