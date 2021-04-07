<template>
  <div v-if="selected && selected.uri">
    <b-card>
      <template v-slot:header v-if="globalView">
        <h3>{{ $t("ScientificObjectDetail.generalInformation") }}:</h3>
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

        <!-- Geometry -->
        <opensilex-GeometryCopy
          v-if="selected.geometry"
          :value="selected.geometry"
        ></opensilex-GeometryCopy>
      </div>
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
        v-if="selected.name !== value.name"
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
              ></component>
            </li>
          </ul>
        </div>
      </div>
    </b-card>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Watch } from "vue-property-decorator";
import Vue from "vue";

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

  mounted() {
    if (this.selected) {
      this.onSelectionChange();
    }
  }

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];
    this.valueByProperties = {};

    return Promise.all([
      this.$opensilex
        .getService("opensilex.VueJsOntologyExtensionService")
        .getRDFTypeProperties(
          this.selected.rdf_type,
          this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
        ),
      this.$opensilex
        .getService("opensilex.PositionsService")
        .getPosition(this.selected.uri),
    ]).then((result) => {
      this.classModel = result[0].response.result;
      let lastMove = result[1].response.result;

      let valueByProperties = this.buildValueByProperties(
        this.selected.relations,
        lastMove
      );
      this.buildTypeProperties(this.typeProperties, valueByProperties);
      this.valueByProperties = valueByProperties;
    });
  }

  getCustomTypeProperties(customObjet) {
    let valueByProperties = this.buildValueByProperties(
      customObjet.relations,
      null
    );

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

  buildValueByProperties(relationArray, lastMove) {
    let valueByProperties = {};
    for (let i in relationArray) {
      let relation = relationArray[i];
      if (
        (lastMove && lastMove.to && lastMove.to.uri,
        this.$opensilex.Oeso.checkURIs(
          relation.property,
          this.$opensilex.Oeso.HAS_FACILITY
        ))
      ) {
        relation.value = lastMove.to.uri;
      }

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
}
</script>