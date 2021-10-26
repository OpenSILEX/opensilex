<template>
  <opensilex-Card
      icon="ik#ik-clipboard"
      :label="$t('component.common.description')"
  >
    <template v-slot:rightHeader v-if="withActions">
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
      <opensilex-OrganizationFacilityModalForm
          v-if="
            user.hasCredential(
              credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID
            )
          "
          ref="infrastructureFacilityForm"
          @onUpdate="refresh"
      ></opensilex-OrganizationFacilityModalForm>
    </template>

    <template v-slot:body>
      <!-- URI -->
      <opensilex-UriView
          :uri="selectedFacilityOrDefault.uri"
          :value="selectedFacilityOrDefault.uri"
          :to="{
            path: '/infrastructure/facility/details/' + encodeURIComponent(selectedFacilityOrDefault.uri),
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
          label="OrganizationFacilityDetail.organizations"
          :list="organizationUriList"
          :inline="false"
      >
      </opensilex-UriListView>

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
import {InfrastructureFacilityGetDTO} from "opensilex-core/model/infrastructureFacilityGetDTO";
import {Prop, Ref, Watch} from "vue-property-decorator";

@Component
export default class OrganizationFacilityDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected: InfrastructureFacilityGetDTO;

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

  editInfrastructureFacility() {
    let editDto = {
      ...this.selected,
      organizations: this.selected.organizations.map(org => org.uri)
    };
    this.infrastructureFacilityForm.showEditForm(editDto);
  }

  deleteInfrastructureFacility() {
    this.$opensilex
        .getService("opensilex.OrganisationsService")
        .deleteInfrastructureFacility(this.selected.uri)
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

<style scoped>

</style>

<i18n>
en:
  OrganizationFacilityDetail:
    organizations: Organizations
fr:
  OrganizationFacilityDetail:
    organizations: Organisations
</i18n>