<template>
    <b-card v-if="selected && selected.uri">

        <b-tabs content-class="mt-3" :value=tabsIndex @input="updateTabs">
            <b-tab :title="$t('ScientificObjectDetail.title')"></b-tab>
            <b-tab :title="$t('Documents')"></b-tab>
            <b-tab :title="$t('Annotation.list-title')"></b-tab>
            <b-tab :title="$t('Event.list-title')"></b-tab>
        </b-tabs>
        <!--    <template v-slot:header>-->
        <!--      <h3>-->
        <!--        &lt;!&ndash; <opensilex-Icon icon="ik#ik-clipboard" /> &ndash;&gt;-->
        <!--        {{$t("ScientificObjectDetail.title")}}-->
        <!--      </h3>-->
        <!--    </template>-->
        <div v-if="loadDetails()">
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.name"></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView :type="selected.type" :typeLabel="selected.typeLabel"></opensilex-TypeView>

      <!-- Type -->
      <opensilex-GeometryView
        v-if="selected.geometry"
        label="component.common.geometry"
        :value="selected.geometry"
      ></opensilex-GeometryView>

      <div v-for="(v, index) in typeProperties" v-bind:key="index">
        <div class="static-field" v-if="!v.definition.isList">
          <span class="field-view-title">{{v.definition.name}}:</span>
          <component :is="v.definition.viewComponent" :value="v.property"></component>
        </div>
        <div class="static-field" v-else-if="v.property && v.property.length > 0">
          <span class="field-view-title">{{v.definition.name}}:</span>
          <ul>
            <br />
            <li v-for="(prop, propIndex) in v.property" v-bind:key="propIndex">
              <component :is="v.definition.viewComponent" :value="prop"></component>
            </li>
          </ul>
        </div>
            </div>
     

        <opensilex-AnnotationList
                v-if="isAnnotationTab()"
                ref="annotationList"
                :target="selected.uri"
                :enableActions="true"
                :columnsToDisplay="new Set(['author','motivation','created'])"
                :modificationCredentialId="credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
                :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
        ></opensilex-AnnotationList>

        <opensilex-DocumentTabList
                v-if="isDocumentTab()"
                :uri="selected.uri"
                :modificationCredentialId="credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
                :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
        ></opensilex-DocumentTabList>

    </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import AnnotationModalForm from "../annotations/form/AnnotationModalForm.vue";

@Component
export default class ScientificObjectDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop({
    default: () => []
  })
  objectByContext;

  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};

  static DETAILS_TAB = "Details";
  static DOCUMENTS_TAB = "Documents";
  static ANNOTATIONS_TAB = "Annotations";
  static EVENTS_TAB = "Events";

  static tabsValues = [
    ScientificObjectDetail.DETAILS_TAB,
    ScientificObjectDetail.DOCUMENTS_TAB,
    ScientificObjectDetail.ANNOTATIONS_TAB,
    ScientificObjectDetail.EVENTS_TAB
  ];

  tabsIndex: number = 0;
  tabsValue: string = ScientificObjectDetail.DETAILS_TAB;

  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("annotationModalForm")
  readonly annotationModalForm!: AnnotationModalForm;

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

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];
    this.valueByProperties = {};

    return this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .getClassProperties(
        this.selected.type,
        this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI
      )
      .then(http => {
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

  private loadDetails(): boolean {
    return this.tabsValue == ScientificObjectDetail.DETAILS_TAB;
  }

  private isAnnotationTab(): boolean {
    return this.tabsValue == ScientificObjectDetail.ANNOTATIONS_TAB;
  }

  private loadEvents(): boolean {
    return this.tabsValue == ScientificObjectDetail.EVENTS_TAB;
  }

  private isDocumentTab(): boolean {
    return this.tabsValue == ScientificObjectDetail.DOCUMENTS_TAB;
  }

  private updateTabs(tabIndex) {
    if (tabIndex >= 0 && tabIndex < ScientificObjectDetail.tabsValues.length) {
      this.tabsIndex = tabIndex;
      this.tabsValue = ScientificObjectDetail.tabsValues[tabIndex];
    }
  }

  updateAnnotations() {
    this.$nextTick(() => {
      this.annotationList.refresh();
    });
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    ScientificObjectDetail:
        title: Detail
        generalInformation: Global informations

fr:
    ScientificObjectDetail:
        title: Détail
        generalInformation: Informations globales
</i18n>