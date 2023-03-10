<template>
  <div v-if="selected && selected.uri">
    <b-card>
      <template v-slot:header v-if="globalView">
        <h3>{{ $t("ScientificObjectDetail.generalInformation") }}:</h3>
        <div class="card-header-right">
          <b-button-group>
            <opensilex-FavoriteButton
                :uri="selected.uri"
            ></opensilex-FavoriteButton>
            <opensilex-EditButton
                v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                )
              "
              @click="soForm.editScientificObject(selected.uri)"
              label="ExperimentScientificObjects.edit-scientific-object"
              :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                )
              "
              label="ExperimentScientificObjects.delete-scientific-object"
              @click="deleteScientificObject(selected.uri)"
              :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
          <opensilex-ScientificObjectForm
            ref="soForm"
            @onUpdate="$emit('onUpdate', selected.uri)"
            @onCreate="$emit('onCreate', selected.uri)"
          ></opensilex-ScientificObjectForm>
        </div>
      </template>

      <!-- URI -->
      <opensilex-UriView
        v-if="withBasicProperties"
        :uri="selected.uri"
      ></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView
        v-if="withBasicProperties"
        :value="selected.name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        v-if="withBasicProperties"
        :type="selected.rdf_type"
        :typeLabel="selected.rdf_type_name"
      ></opensilex-TypeView>

      <div v-if="selected && selected.uri">
        <div v-for="(v, index) in typeProperties" v-bind:key="index">
          <!-- isHosted : should be calculated and not be a property -->
          <div v-if="!Array.isArray(v.property) && !(['vocabulary:isHosted'].indexOf( v.definition.uri ) > -1)" class="static-field">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <component
              :is="v.definition.view_component"
              :value="v.property"
              :experiment="experiment"
            ></component>
          </div>
          <!-- isHosted : should be calculated and not be a property -->
          <div
            v-else-if="v.property && v.property.length > 0 && !(['vocabulary:isHosted'].indexOf( v.definition.uri ) > -1)"
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

        <!-- Geometry -->
        <opensilex-GeometryCopy
          v-if="selected.geometry"
          :value="selected.geometry"
        ></opensilex-GeometryCopy>
      </div>
      <!--Last Position-->
      <opensilex-StringView v-if="withBasicProperties && lastPosition.event" label="Event.lastPosition">
          <!-- Position detail -->
          <span>{{new Date(lastPosition.move_time).toLocaleString()}}</span>
          <ul>
            <li v-if="lastPosition.to">{{lastPosition.to.name}}</li>
            <li v-if="lastPosition.position && (lastPosition.position.x || lastPosition.position.y || lastPosition.position.z)">{{customCoordinatesText(lastPosition.position)}}</li>
            <li v-if="lastPosition.position && lastPosition.position.text">{{lastPosition.position.text}}</li>
            <li v-if="lastPosition.position && lastPosition.position.point">
              <opensilex-GeometryCopy label="" :value="lastPosition.position.point">
              </opensilex-GeometryCopy>
            </li>
          </ul>
      </opensilex-StringView>
    </b-card>

    <b-card v-for="(value, index) in objectByContext" :key="index">
      <template v-slot:header>
        <h3>
          {{ $t("component.experiment.view.title") }}:
          <opensilex-UriLink
            :allowCopy="false"
            :to="{
              path:
                '/experiment/details/' + encodeURIComponent(value.experiment),
            }"
            :value="value.experiment_name"
          ></opensilex-UriLink>
        </h3>
      </template>
      <!-- Name -->
      <opensilex-StringView
        :value="value.name"
        label="component.common.name"
      ></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView
        v-if="selected.rdf_type !== value.rdf_type"
        :type="value.rdf_type"
        :typeLabel="value.rdf_type_name"
      ></opensilex-TypeView>

      <div
        v-for="(v, index) in getCustomTypeProperties(value)"
        v-bind:key="index"
      >
        <div v-if="!Array.isArray(v.property)" class="static-field">
          <span class="field-view-title">{{ v.definition.name }}</span>
          <component
            :is="v.definition.view_component"
            :value="v.property"
            :experiment="value.experiment"
          ></component>
        </div>
        <div
          v-else-if="v.property && v.property.length > 0"
          class="static-field"
        >
          <span class="field-view-title">{{ v.definition.name }}</span>
          <ul>
            <br />
            <li v-for="(prop, propIndex) in v.property" v-bind:key="propIndex">
              <component
                :is="v.definition.view_component"
                :value="prop"
                :experiment="value.experiment"
              ></component>
            </li>
          </ul>
        </div>
      </div>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import {VueRDFTypePropertyDTO} from "../../lib";
import {PositionGetDTO} from "../../../../../opensilex-core/front/src/lib";

@Component
export default class ScientificObjectDetailProperties extends Vue {
  $opensilex: any;

  @Prop()
  selected;
  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};

  @Prop({
    default: () => [],
  })
  objectByContext;

  @Prop({
    default: false,
  })
  globalView;

  @Prop({
    default: true,
  })
  withBasicProperties;

  @Prop({
    default: null,
  })
  experiment;
  lastPosition:PositionGetDTO = {
    event: null,
    from: null,
    position: {
      point: null,
      text: null,
      x:null,
      y:null,
      z:null
    },
    to: null
  };
  mounted() {
    if (this.selected) {
      this.onSelectionChange();
    }
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("soForm") readonly soForm!: any;

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];
    this.valueByProperties = {};
    this.classModel = {};
    this.$opensilex.disableLoader();
    if (this.globalView) {
      return Promise.all([
        this.$opensilex
          .getService("opensilex.VueJsOntologyExtensionService")
          .getRDFTypeProperties(
            this.selected.rdf_type,
            this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
          ),
        this.$opensilex
          .getService("opensilex.ScientificObjectsService")
          .getScientificObjectDetail(this.selected.uri, undefined),
        this.$opensilex
          .getService("opensilex.PositionsService")
          .getPosition(this.selected.uri)
          .catch(() => null),
      ]).then((result) => {
        this.$opensilex.enableLoader();

        this.classModel = result[0].response.result;
        if (result[2] != null) {
          this.lastPosition = result[2].response.result;
        }

        let valueByProperties = this.buildValueByProperties(result[1].response.result.relations);
        this.buildTypeProperties(this.typeProperties, valueByProperties);
        this.valueByProperties = valueByProperties;
      });
    } else {
      return Promise.all([
        this.$opensilex
          .getService("opensilex.VueJsOntologyExtensionService")
          .getRDFTypeProperties(
            this.selected.rdf_type,
            this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
          ),
        this.$opensilex
          .getService("opensilex.PositionsService")
          .getPosition(this.selected.uri)
          .catch(() => null),
      ]).then((result) => {
        this.$opensilex.enableLoader();

        this.classModel = result[0].response.result;
        if (result[1] != null) {
          this.lastPosition = result[1].response.result;
        }

        let valueByProperties = this.buildValueByProperties(this.selected.relations);
        this.buildTypeProperties(this.typeProperties, valueByProperties);
        this.valueByProperties = valueByProperties;
      });
    }
  }

  getCustomTypeProperties(customObjet) {
    let valueByProperties = this.buildValueByProperties(customObjet.relations);
    for (let propUri in valueByProperties) {
      if (this.valueByProperties[propUri]) {
        if (
          this.checkRelationValueEquality(
            valueByProperties[propUri],
            this.valueByProperties[propUri]
          )
        ) {
          delete valueByProperties[propUri];
        }
      }
    }
    let typeProperties = [];
    this.buildTypeProperties(typeProperties, valueByProperties);
    return typeProperties;
  }

  checkRelationValueEquality(a, b) {
    if (Array.isArray(a)) {
      if (!Array.isArray(b)) {
        return false;
      } else {
        if (a.length != b.length) {
          return false;
        } else {
          let intersect = a.filter((x) => {
            let hasMatch = false;
            for (let y of b) {
              if (this.checkRelationValueEquality(x, y)) {
                hasMatch = true;
                break;
              }
            }
            return hasMatch;
          });
          return intersect.length == a.length;
        }
      }
    } else {
      if (Array.isArray(b)) {
        return false;
      } else {
        return a == b;
      }
    }
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

  loadProperties(typeProperties: Array<{definition: VueRDFTypePropertyDTO,property}> , properties: Array<VueRDFTypePropertyDTO>, valueByProperties) {

      if(! properties){
          return
      }
      properties.forEach(property => {

          if (valueByProperties[property.uri]) {
              if (property.is_list && !Array.isArray(valueByProperties[property.uri])) {
                  typeProperties.push({
                      definition: property,
                      property: [valueByProperties[property.uri]],
                  });
              } else {
                  typeProperties.push({
                      definition: property,
                      property: valueByProperties[property.uri],
                  });
              }
          }
  })
  }

  buildValueByProperties(ungroupedRelations: Array<{property,value}>) {
    let groupedRelations = {};

    for (let i in ungroupedRelations) {
      let relation = ungroupedRelations[i];

      if (
        groupedRelations[relation.property] &&
        !Array.isArray(groupedRelations[relation.property])
      ) {
        groupedRelations[relation.property] = [
          groupedRelations[relation.property],
        ];
      }

      if (Array.isArray(groupedRelations[relation.property])) {
        groupedRelations[relation.property].push(relation.value);
      } else {
        groupedRelations[relation.property] = relation.value;
      }
    }

    return groupedRelations;
  }

  deleteScientificObject(uri) {
    let scientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    scientificObjectsService
      .deleteScientificObject(uri)
      .then(() => {
        this.$router.push({
          path: "/scientific-objects",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  customCoordinatesText(position: any): string {

    if (!position) {
      return undefined;
    }

    let customCoordinates = "";

    if (position.x) {
      customCoordinates += "X:" + position.x;
    }
    if (position.y) {
      if (customCoordinates.length > 0) {
        customCoordinates += ", ";
      }
      customCoordinates += "Y:" + position.y;
    }
    if (position.z) {
      if (customCoordinates.length > 0) {
        customCoordinates += ", ";
      }
      customCoordinates += "Z:" + position.z;
    }

    if (customCoordinates.length == 0) {
      return undefined;
    }
    return customCoordinates;
  }
}
</script>

<i18n>
en:
  Event:
    lastPosition: Last position
fr:
  Event:
    lastPosition: Dernière position
</i18n>