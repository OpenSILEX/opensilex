<template>
  <div v-if="selected && selected.uri">
    <opensilex-PageActions v-if="!simpleDisplay" :returnButton="withReturnButton" :tabs="true">
      <b-nav-item
        :active="isDetailsTab"
        @click.prevent="tabsValue = DETAILS_TAB"
      >{{ $t("component.common.details-label") }}</b-nav-item>

      <b-nav-item
        @click.prevent="tabsValue = VISUALIZATION_TAB"
        :active="isVisualizationTab"
      >{{ $t("ScientificObjectVisualizationTab.visualization") }}</b-nav-item>

      <b-nav-item
        :active="isDatafilesTab"
        @click.prevent="tabsValue = DATAFILES_TAB"
      >{{ $t("ScientificObjectDataFiles.datafiles") }}</b-nav-item>

      <b-nav-item
        :active="isAnnotationTab"
        @click.prevent="tabsValue = ANNOTATIONS_TAB"
      >{{ $t("Annotation.list-title") }}</b-nav-item>

      <b-nav-item
        :active="isDocumentTab"
        @click.prevent="tabsValue = DOCUMENTS_TAB"
      >{{ $t("DocumentTabList.documents") }}</b-nav-item>
    </opensilex-PageActions>

    <div v-if="simpleDisplay">
      <opensilex-ScientificObjectDetailAdvanced v-if="selected" :selected="selected"></opensilex-ScientificObjectDetailAdvanced>
    </div>

    <div v-if="isDetailsTab && !simpleDisplay">
      <b-card>
        <template v-if="withReturnButton" v-slot:header>
          <h3 v-if="selected.experiment">
            {{ $t("component.experiment.view.title") }}:
            <opensilex-UriLink
              :allowCopy="false"
              :to="{
                path:
                  '/experiment/details/' +
                  encodeURIComponent(selected.experiment),
              }"
              :value="selected.experiment_name"
            ></opensilex-UriLink>
          </h3>
          <h3>{{ $t("ScientificObjectDetail.generalInformation") }}:</h3>
        </template>
        <!-- URI -->
        <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
        <!-- Name -->
        <opensilex-StringView :value="selected.name" label="component.common.name"></opensilex-StringView>
        <!-- Type -->
        <opensilex-TypeView :type="selected.rdf_type" :typeLabel="selected.rdf_type_name"></opensilex-TypeView>

        <opensilex-ScientificObjectDetailAdvanced v-if="selected" :selected="selected"></opensilex-ScientificObjectDetailAdvanced>
      </b-card>

      <b-card v-for="(value, index) in objectByContext" :key="index">
        <template v-slot:header>
          <h3 v-if="value.experiment">
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

        <div v-for="(v, index) in getCustomTypeProperties(value)" v-bind:key="index">
          <div v-if="!v.definition.isList" class="static-field">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <component :is="v.definition.viewComponent" :value="v.property"></component>
          </div>
          <div v-else-if="v.property && v.property.length > 0" class="static-field">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <ul>
              <br />
              <li v-for="(prop, propIndex) in v.property" v-bind:key="propIndex">
                <component :is="v.definition.viewComponent" :value="prop"></component>
              </li>
            </ul>
          </div>
        </div>
      </b-card>
    </div>

    <opensilex-ScientificObjectDataFiles v-if="isDatafilesTab" :uri="selected.uri"></opensilex-ScientificObjectDataFiles>

    <div v-if="isAnnotationTab">
      <opensilex-AnnotationList
        ref="annotationList"
        :columnsToDisplay="new Set(['creator', 'motivation', 'created'])"
        :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
        :enableActions="true"
        :modificationCredentialId=" credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
        :target="selected.uri"
        @onEdit="annotationModalForm.showEditForm($event)"
      ></opensilex-AnnotationList>
    </div>

    <opensilex-ScientificObjectVisualizationTab
      v-if="isVisualizationTab"
      :scientificObject="selected.uri"
      :modificationCredentialId="credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
    ></opensilex-ScientificObjectVisualizationTab>

    <opensilex-DocumentTabList
      v-if="isDocumentTab"
      :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
      :modificationCredentialId=" credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
      :uri="selected.uri"
    ></opensilex-DocumentTabList>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import AnnotationModalForm from "../annotations/form/AnnotationModalForm.vue";

@Component
export default class ScientificObjectDetailBasic extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop({
    default: () => []
  })
  objectByContext;

  @Prop({
    default: false
  })
  withReturnButton;

  @Prop({
    default: false
  })
  simpleDisplay;

  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};

  DETAILS_TAB = "Details";
  VISUALIZATION_TAB = "Visualization";
  DOCUMENTS_TAB = "Documents";
  ANNOTATIONS_TAB = "Annotations";
  EVENTS_TAB = "Events";
  DATAFILES_TAB = "Datafiles";

  tabsIndex: number = 0;
  tabsValue: string = this.DETAILS_TAB;

  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("annotationModalForm")
  readonly annotationModalForm!: AnnotationModalForm;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get isDetailsTab(): boolean {
    return this.tabsValue == this.DETAILS_TAB;
  }

  get isVisualizationTab(): boolean {
    return this.tabsValue == this.VISUALIZATION_TAB;
  }

  get isAnnotationTab(): boolean {
    return this.tabsValue == this.ANNOTATIONS_TAB;
  }

  get isDocumentTab(): boolean {
    return this.tabsValue == this.DOCUMENTS_TAB;
  }

  get isDatafilesTab(): boolean {
    return this.tabsValue == this.DATAFILES_TAB;
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

    // return this.$opensilex
    //   .getService("opensilex.VueJsOntologyExtensionService")
    //   .getClassProperties(
    //     this.selected.rdf_type,
    //     this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
    //   )
    //   .then((http) => {
    //     this.classModel = http.response.result;
    //     let valueByProperties = this.buildValueByProperties(
    //       this.selected.relations
    //     );
    //     this.buildTypeProperties(this.typeProperties, valueByProperties);
    //     this.valueByProperties = valueByProperties;
    //   });
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
            property: [valueByProperties[property.property]]
          });
        } else {
          typeProperties.push({
            definition: property,
            property: valueByProperties[property.property]
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
          valueByProperties[relation.property]
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
          let intersect = a.filter(x => {
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

  updateAnnotations() {
    this.$nextTick(() => {
      this.annotationList.refresh();
    });
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
