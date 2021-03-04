<template>
  <b-card v-if="selected && selected.uri">

   <opensilex-PageActions :tabs=true :returnButton="withReturnButton">
      <b-nav-item
      :active="isDetailsTab"
      @click.prevent="tabsValue = DETAILS_TAB"
      >{{ $t('component.common.details-label') }}
      </b-nav-item>

      <b-nav-item
      class="ml-3"
      @click.prevent="tabsValue = ANNOTATIONS_TAB"
      :active="isAnnotationTab"
      >{{ $t("Annotation.list-title") }}
      </b-nav-item>

      <b-nav-item
      class="ml-3"
      @click.prevent="tabsValue = DOCUMENTS_TAB"
      :active="isDocumentTab"
      >{{ $t("DocumentTabList.documents") }}
      </b-nav-item>

    </opensilex-PageActions>

    <div v-if="isDetailsTab">
      <b-card>
        <template v-slot:header v-if="withReturnButton">
          <h3 v-if="selected.experiment">
            {{ $t("component.experiment.view.title") }}:
            <opensilex-UriLink
              :to="{
                path:
                  '/experiment/details/' +
                  encodeURIComponent(selected.experiment),
              }"
              :value="selected.experiment_name"
              :allowCopy="false"
            ></opensilex-UriLink>
          </h3>
          <h3>{{ $t("ScientificObjectDetail.generalInformation") }}:</h3>
        </template>
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
          <div class="static-field" v-if="!v.definition.isList">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <component
              :is="v.definition.viewComponent"
              :value="v.property"
            ></component>
          </div>
          <div
            class="static-field"
            v-else-if="v.property && v.property.length > 0"
          >
            <span class="field-view-title">{{ v.definition.name }}</span>
            <ul>
              <br />
              <li
                v-for="(prop, propIndex) in v.property"
                v-bind:key="propIndex"
              >
                <component
                  :is="v.definition.viewComponent"
                  :value="prop"
                ></component>
              </li>
            </ul>
          </div>
        </div>

        <!-- Geometry -->
        <opensilex-GeometryView
          v-if="selected.geometry"
          label="component.common.geometry"
          :value="selected.geometry"
        ></opensilex-GeometryView>
      </b-card>

      <b-card v-for="(value, index) in objectByContext" :key="index">
        <template v-slot:header>
          <h3 v-if="value.experiment">
            {{ $t("component.experiment.view.title") }}:
            <opensilex-UriLink
              :to="{
                path:
                  '/experiment/details/' + encodeURIComponent(value.experiment),
              }"
              :value="value.experiment_name"
              :allowCopy="false"
            ></opensilex-UriLink>
          </h3>
        </template>
        <!-- Name -->
        <opensilex-StringView
          v-if="selected.name != value.name"
          label="component.common.name"
          :value="value.name"
        ></opensilex-StringView>
        <!-- Type -->
        <opensilex-TypeView
          v-if="selected.rdf_type != value.rdf_type"
          :type="value.rdf_type"
          :typeLabel="value.rdf_type_name"
        ></opensilex-TypeView>

        <div
          v-for="(v, index) in getCustomTypeProperties(value)"
          v-bind:key="index"
        >
          <div class="static-field" v-if="!v.definition.isList">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <component
              :is="v.definition.viewComponent"
              :value="v.property"
            ></component>
          </div>
          <div
            class="static-field"
            v-else-if="v.property && v.property.length > 0"
          >
            <span class="field-view-title">{{ v.definition.name }}</span>
            <ul>
              <br />
              <li
                v-for="(prop, propIndex) in v.property"
                v-bind:key="propIndex"
              >
                <component
                  :is="v.definition.viewComponent"
                  :value="prop"
                ></component>
              </li>
            </ul>
          </div>
        </div>
      </b-card>
    </div>
    
    <opensilex-AnnotationList
      v-if="isAnnotationTab"
      ref="annotationList"
      :target="selected.uri"
      :enableActions="true"
      :columnsToDisplay="new Set(['creator', 'motivation', 'created'])"
      :modificationCredentialId="
        credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
      "
      :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
      @onEdit="annotationModalForm.showEditForm($event)"
    ></opensilex-AnnotationList>

    <opensilex-Button
      v-if="
        isAnnotationTab &&
        user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
      "
      label="Annotation.add"
      variant="primary"
      :small="false"
      icon="fa#edit"
      @click="annotationModalForm.showCreateForm()"
    ></opensilex-Button>

    <opensilex-AnnotationModalForm
      v-if="
        isAnnotationTab &&
        user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
      "
      ref="annotationModalForm"
      :target="selected.uri"
      @onCreate="updateAnnotations"
      @onUpdate="updateAnnotations"
    ></opensilex-AnnotationModalForm>

    <opensilex-DocumentTabList
      v-if="isDocumentTab"
      :uri="selected.uri"
      :modificationCredentialId="
        credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
      "
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
    default: () => [],
  })
  objectByContext;

  @Prop({
    default: false,
  })
  withReturnButton;

  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};

  DETAILS_TAB = "Details";
  DOCUMENTS_TAB = "Documents";
  ANNOTATIONS_TAB = "Annotations";
  EVENTS_TAB = "Events";

  tabsIndex: number = 0;
  tabsValue: string = this.DETAILS_TAB;

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

  get isDetailsTab(): boolean {
    return this.tabsValue == this.DETAILS_TAB;
  }

  get isAnnotationTab(): boolean {
    return this.tabsValue == this.ANNOTATIONS_TAB;
  }

  get isDocumentTab(): boolean {
    return this.tabsValue == this.DOCUMENTS_TAB;
  }

  updateAnnotations() {
    this.$nextTick(() => {
      this.annotationList.refresh();
    });
  }
}
</script>

<style scoped lang="scss">
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