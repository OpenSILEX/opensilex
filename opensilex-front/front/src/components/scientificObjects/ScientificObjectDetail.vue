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
        <opensilex-ScientificObjectDetailSimple
            :selected="selected"
        ></opensilex-ScientificObjectDetailSimple>
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
          v-if="selected.name !== value.name"
          label="component.common.name"
          :value="value.name"
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
          <div class="static-field" v-if="!v.definition.is_list">
            <span class="field-view-title">{{ v.definition.name }}</span>
            <component
              :is="v.definition.view_component"
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
                  :is="v.definition.view_component"
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

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
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