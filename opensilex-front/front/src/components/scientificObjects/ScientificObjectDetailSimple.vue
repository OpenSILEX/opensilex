<template>
  <div v-if="selected && selected.uri">
    <!-- URI -->
    <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
    <!-- Name -->
    <opensilex-StringView
      label="component.common.name"
      :value="selected.name"
    ></opensilex-StringView>
    <!-- Type -->
    <opensilex-TypeView
      :type="selected.rdf_type"
      :typeLabel="selected.rdf_type_name"
    ></opensilex-TypeView>

    <div v-for="(v, index) in typeProperties" v-bind:key="index">
      <div class="static-field" v-if="!v.definition.is_list">
        <span class="field-view-title">{{ v.definition.name }}</span>
        <component
          :is="v.definition.view_component"
          :value="v.property"
        ></component>
      </div>
      <div class="static-field" v-else-if="v.property && v.property.length > 0">
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

    <!-- Geometry -->
    <opensilex-GeometryView
      v-if="copyGeometryButton === false && selected.geometry"
      :value="selected.geometry"
      label="component.common.geometry"
    ></opensilex-GeometryView>
    <opensilex-GeometryCopy
      v-if="copyGeometryButton === true && selected.geometry"
      :value="selected.geometry"
    ></opensilex-GeometryCopy>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Watch } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectDetailSimple extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop({
    default: false,
  })
  copyGeometryButton;

  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  mounted() {
    if (this.selected) {
      this.onSelectionChange();
    }
  }

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];
    this.valueByProperties = {};

    return this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .getRDFTypeProperties(
        this.selected.rdf_type,
        this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
      )
      .then((http) => {
        this.classModel = http.response.result;
        let valueByProperties = this.buildValueByProperties(
          this.selected.relations
        );
        this.buildTypeProperties(this.typeProperties, valueByProperties);
        this.valueByProperties = valueByProperties;
      });
  }

  buildTypeProperties(typeProperties, valueByProperties) {
    this.loadProperties(
      typeProperties,
      this.classModel.dataProperties,
      valueByProperties
    );
    this.loadProperties(
      typeProperties,
      this.classModel.objectProperties,
      valueByProperties
    );

    let pOrder = this.classModel.propertiesOrder;

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
          property.isList &&
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
}
</script>

<style lang="scss" scoped>
.withReturnButton {
  margin-left: 65px;
}

.back-button {
  float: left;
}
</style>


<i18n>
en:
  ScientificObjectDetail:
    title: Detail
    generalInformation: Global information

fr:
  ScientificObjectDetail:
    title: Détail
    generalInformation: Informations globales
</i18n>