<template>
  <div v-if="selected && selected.uri" :class="{'ok embed-tabs': !globalView}">
    <opensilex-PageActions :returnButton="withReturnButton" :tabs="true">
      <b-nav-item
        :active="isDetailsTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.DETAILS_TAB)"
        >{{ $t("component.common.details-label") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.VISUALIZATION_TAB)"
        @click.prevent="onTabChanged(ScientificObjectDetail.VISUALIZATION_TAB)"
        :active="isVisualizationTab"
        >{{ $t("ScientificObjectVisualizationTab.visualization") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.DATAFILES_TAB)"
        :active="isDatafilesTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.DATAFILES_TAB)"
        >{{ $t("ScientificObjectDataFiles.datafiles") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.EVENTS_TAB)"
        :active="isEventTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.EVENTS_TAB)"
        >{{ $t("Event.list-title") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.POSITIONS_TAB)"
        :active="isPositionTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.POSITIONS_TAB)"
        >{{ $t("Position.list-title") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.ANNOTATIONS_TAB)"
        :active="isAnnotationTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.ANNOTATIONS_TAB)"
        >{{ $t("Annotation.list-title") }}
      </b-nav-item>

      <b-nav-item
        v-if="includeTab(ScientificObjectDetail.DOCUMENTS_TAB)"
        :active="isDocumentTab"
        @click.prevent="onTabChanged(ScientificObjectDetail.DOCUMENTS_TAB)"
        >{{ $t("DocumentTabList.documents") }}
      </b-nav-item>
    </opensilex-PageActions>

    <div v-if="isDetailsTab">
      <opensilex-ScientificObjectDetailProperties
        :globalView="globalView"
        :selected="selected"
        :objectByContext="objectByContext"
        :experiment="experiment"
        @onUpdate="$emit('onUpdate', $event)"
      ></opensilex-ScientificObjectDetailProperties>
    </div>

    <opensilex-ScientificObjectDataFiles
      v-if="isDatafilesTab"
      :uri="selected.uri"
      @redirectToDetail="onTabChanged(ScientificObjectDetail.DETAILS_TAB)"
    ></opensilex-ScientificObjectDataFiles>

    <div v-if="isAnnotationTab">
      <opensilex-AnnotationList
        ref="annotationList"
        :columnsToDisplay="new Set(['creator', 'motivation', 'created'])"
        :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
        :enableActions="true"
        :modificationCredentialId="
          credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID
        "
        :target="selected.uri"
      ></opensilex-AnnotationList>
    </div>

    <opensilex-ScientificObjectVisualizationTab
      v-if="isVisualizationTab"
      :scientificObject="selected"
    ></opensilex-ScientificObjectVisualizationTab>

    <opensilex-DocumentTabList
      v-if="isDocumentTab"
      :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
      :uri="selected.uri"
    ></opensilex-DocumentTabList>

    <opensilex-EventList
      v-if="isEventTab"
      ref="eventList"
      :target="selected.uri"
      :displayTargetFilter="false"
      :columnsToDisplay="getEventColumnToDisplay()"
      :maximizeFilterSize="true"
      :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
      :context="{experimentURI: experiment}"
    ></opensilex-EventList>

    <opensilex-PositionList
      v-if="isPositionTab"
      ref="positionList"
      :target="selected.uri"
      :columnsToDisplay="getPositionsColumnToDisplay()"
      :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
    ></opensilex-PositionList>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import PositionList from "../positions/list/PositionList.vue";
import EventList from "../events/list/EventList.vue";

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

  @Prop({
    default: false,
  })
  globalView;

  @Prop({
    default: null,
  })
  experiment;

  @Prop({
    default: ScientificObjectDetail.DETAILS_TAB
  })
  defaultTabsValue: string;

  getEventColumnToDisplay() : Set<string>{
      return this.globalView ? EventList.getDefaultColumns() : new Set(['type', 'end', 'description']) ;
  }

  getPositionsColumnToDisplay() : Set<string>{
      return this.globalView ? PositionList.getDefaultColumns() : new Set(['end']) ;
  }


  @Prop({
    default: () => {
      return [
        ScientificObjectDetail.VISUALIZATION_TAB,
        ScientificObjectDetail.DOCUMENTS_TAB,
        ScientificObjectDetail.ANNOTATIONS_TAB,
        ScientificObjectDetail.EVENTS_TAB,
        ScientificObjectDetail.POSITIONS_TAB,
        ScientificObjectDetail.DATAFILES_TAB,
      ];
    },
  })
  tabs;

  typeProperties = [];
  valueByProperties = {};
  classModel: any = {};

  ScientificObjectDetail = ScientificObjectDetail;

  public static DETAILS_TAB = "Details";
  public static VISUALIZATION_TAB = "Visualization";
  public static DOCUMENTS_TAB = "Documents";
  public static ANNOTATIONS_TAB = "Annotations";
  public static EVENTS_TAB = "Events";
  public static POSITIONS_TAB = "Positions";
  public static DATAFILES_TAB = "Datafiles";

  tabsValue: string = ScientificObjectDetail.DETAILS_TAB;

  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("eventList") readonly eventList!: EventList;
  @Ref("positionList") readonly positionList!: PositionList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get isDetailsTab(): boolean {
    return this.tabsValue === ScientificObjectDetail.DETAILS_TAB;
  }

  get isVisualizationTab(): boolean {
    return this.tabsValue === ScientificObjectDetail.VISUALIZATION_TAB;
  }

  get isAnnotationTab(): boolean {
    return this.tabsValue === ScientificObjectDetail.ANNOTATIONS_TAB;
  }

  get isDocumentTab(): boolean {
    return this.tabsValue === ScientificObjectDetail.DOCUMENTS_TAB;
  }

  get isEventTab(): boolean {
    return this.tabsValue === ScientificObjectDetail.EVENTS_TAB;
  }

  get isPositionTab(): boolean {
    return this.tabsValue === ScientificObjectDetail.POSITIONS_TAB;
  }

  get isDatafilesTab() {
    return this.tabsValue === ScientificObjectDetail.DATAFILES_TAB;
  }

  created() {
    // at start default tab is detail tab
    this.tabsValue = this.defaultTabsValue;
  }

  includeTab(tab) {
    return this.tabs.indexOf(tab) >= 0;
  }

  onTabChanged(tab){
    // overwrite with selected tab information and sent it
    this.tabsValue = tab;
    this.$emit("tabChanged", tab)
  }
}
</script>

<style lang="scss">
.withReturnButton {
  margin-left: 65px;
}

.back-button {
  float: left;
}

.embed-tabs > .card {
  margin-bottom: 0!important;
}

.embed-tabs .row .card:first-child  > .card-header {
  display: none
}

.embed-tabs .row .card:first-child  {
  border-top: none;
  margin-top: -5px;
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
