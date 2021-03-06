<template>
  <div v-if="selected && selected.uri">
    <!-- Name -->
    <span v-if="withBasicProperties" class="field-view-title">{{
      $t("component.common.name")
    }}</span>
    <p></p>
    <opensilex-UriLink
      :to="{
        path: '/scientific-objects/details/' + encodeURIComponent(selected.uri),
      }"
      :uri="selected.uri"
      :value=" selected.name + ' (' + ( selected.rdf_type_name.charAt(0).toUpperCase() + selected.rdf_type_name.slice(1) ).bold() + ')'"
      target="_blank"
    >
    </opensilex-UriLink>
    <template v-if="selected && selected.uri">
     <div v-for="(v, index) in typeProperties" v-bind:key="index">
        <template v-if=" ['vocabulary:hasFactorLevel', 'vocabulary:hasGermplasm'].indexOf( v.definition.property ) > -1">
          <span class="field-view-title">{{ v.definition.name }}</span>
          <ul>
            <br />
            <li v-for="(prop, propIndex) in v.property" v-bind:key="propIndex">
              <component
                :is="v.definition.view_component"
                :experiment="experiment"
                :value="prop"
                target="_blank"
              ></component>
            </li>
          </ul>
        </template>
      </div>
      <div v-for="(v, index) in typeProperties" v-bind:key="index">
        <template
          v-if="isViewAllInformation && !Array.isArray(v.property)"
          class="static-field"
        >
          <span class="field-view-title">{{ v.definition.name }}</span>
          <component
            :is="v.definition.view_component"
            :experiment="experiment"
            :value="v.property"
          ></component>
        </template>
      </div>
    </template>
    <!-- Geometry -->
    <opensilex-GeometryCopy
      v-if="selected.geometry && isViewAllInformation"
      :value="selected.geometry"
    ></opensilex-GeometryCopy>
    <p>
      <a v-on:click="isViewAllInformation = !isViewAllInformation">{{
        $t(
          isViewAllInformation
            ? "ScientificObjectDetailMap.seeMoreInformation"
            : "ScientificObjectDetailMap.viewAllInformation"
        )
      }}</a>
    </p>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectDetailMap extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};
  isViewAllInformation: boolean = false;

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
  @Ref("soForm") readonly soForm!: any;

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
    this.classModel = {};

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
      this.classModel = result[0].response.result;
      let lastMove = null;
      if (result[1] != null) {
        lastMove = result[1].response.result;
      }

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
        lastMove &&
        lastMove.to &&
        lastMove.to.uri &&
        this.$opensilex.Oeso.checkURIs(
          relation.property,
          this.$opensilex.Oeso.HAS_FACILITY
        )
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
}
</script>
<style lang="scss" scoped>
ol, ul, dl {
  margin-bottom: 0;
  line-height: 1em;
}

#vl-overlay-detailItem > div > div > div > div > span.field-view-title,
#vl-overlay-detailItem > div > div > div > div > span > a > span,
#vl-overlay-detailItem > div > div > div > div > div > span,
#vl-overlay-detailItem > div > div > div > div > div > span.field-view-title {
  line-height: 1em !important;
}

#vl-overlay-detailItem > div > div > div > div {
  padding-top: 2%;
  padding-left: 2%;
}

#vl-overlay-detailItem > div > div > div > div > p > a {
  color: #007bff;
  cursor: pointer;
}

.field-view-title {
  min-width: 120px;
}

#vl-overlay-detailItem > div > div > div > div > div > span.static-field-line {
  display: block !important;
}

::v-deep .capitalize-first-letter {
  display: block;
}

::v-deep a {
  color: #007bff;
}
</style>
<i18n>
en:
  ScientificObjectDetailMap:
    viewAllInformation: Show more
    seeMoreInformation: Show less
fr:
  ScientificObjectDetailMap:
    viewAllInformation: Voir plus
    seeMoreInformation: Voir moins
</i18n>