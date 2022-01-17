<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-monitor"
      title="component.process.title"
      description="component.process.description"
    ></opensilex-PageHeader>
    <opensilex-PageActions>
      <div>
        <b-tabs content-class="mt-3" :value=currentTabIndex @input="updateType">
          <b-tab :title="$t('component.process.step.title')"></b-tab>
          <b-tab :title="$t('component.process.title')"></b-tab>
        </b-tabs>
      </div>
    </opensilex-PageActions>
    <div class="row" v-if="processTab">
      <div class="col-md-6">
        <!-- Process list -->
        <opensilex-ProcessList
          ref="processList"
          @onSelect="onSelectedProcess"
        ></opensilex-ProcessList>
      </div>
      <div class="col-md-6">
        <!-- Process detail -->
        <opensilex-ProcessDetail
            :selected="selectedProcess"
            @onUpdate="refresh"
        ></opensilex-ProcessDetail>
        <!-- Steps -->
        <opensilex-StepView
            v-if="selectedStep"
            :withActions="stepActions"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
            :step="selectedStep"
            :process="selectedProcess"
            :isSelectable="false"
            ref="processStepsView"
            @stepSelected="updateSelectedStep"
        ></opensilex-StepView>
      </div>
    </div>
    <div class="row" v-if="stepTab">
      <div class="col-md-6">
        <!-- Steps -->
        <opensilex-StepView
            :withActions="true"
            @onUpdate="refresh"
            @onCreate="refresh"
            @onDelete="refresh"
            :isSelectable="true"
            ref="stepView"
            @stepSelected="updateSelectedStep"
        ></opensilex-StepView>
      </div>
      <div class="col-md-6">
        <!-- Steps detail -->
        <opensilex-StepDetail
            :selected="selectedStep"
        >
        </opensilex-StepDetail>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ProcessGetDTO, ProcessService} from "opensilex-process/index";
import {StepGetDTO} from "opensilex-core/model/StepGetDTO";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {NamedResourceDTOStepModel} from "opensilex-process/model/namedResourceDTOStepModel";
import PO2 from "../../ontologies/PO2";

@Component
export default class ProcessView extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  service: ProcessService;

  static PROCESS_TAB = "Process";
  static STEP_TAB = "Step";
  static TABS = [
      ProcessView.PROCESS_TAB,
      ProcessView.STEP_TAB
  ];

  currentTabIndex = 0;
  currentTabName = ProcessView.TABS[this.currentTabIndex];

  @Ref("processList") readonly processList!: any;
  @Ref("stepView") readonly stepView!: any;
  @Ref("processStepsView") readonly processStepsView!: any;

  selectedProcess: ProcessGetDTO = null;
  selectedStep: StepGetDTO = null;

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-core.ProcessService"
    );

    let query = this.$route.query;
    if (query && query.tab) {
      let requestedTab = decodeURIComponent(query.tab).toLowerCase();
      let index = ProcessView.TABS.findIndex(tab => tab.toLowerCase() === requestedTab);
      if (index >= 0) {
        this.updateType(index);
      } else {
        this.updateType(0);
      }
    } else {
      this.updateType(0);
    }
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get stepActions(): boolean {
    return !!this.selectedProcess;
  }

  get selectedSteps(): Array<NamedResourceDTOStepModel> {
    if (this.selectedProcess) {
      return this.selectedProcess.step;
    }
    return undefined;
  }

  get processTab() {
    return this.currentTabName == ProcessView.PROCESS_TAB;
  }

  get stepTab() {
    return this.currentTabName == ProcessView.STEP_TAB;
  }

  updateType(tabIndex) {
    if (tabIndex < 0 || tabIndex >= ProcessView.TABS.length) {
      return;
    }
    if (tabIndex !== this.currentTabIndex) {
      this.onTabChange(this.currentTabIndex, tabIndex);
      this.currentTabIndex = tabIndex;
      this.currentTabName = ProcessView.TABS[this.currentTabIndex];
    }
    this.$opensilex.updateURLParameter("tab",this.currentTabName);
  }

  onTabChange(oldIndex, newIndex) {
    if (ProcessView.TABS[oldIndex] === ProcessView.STEP_TAB) {
      this.selectedStep = undefined;
    }
  }

  onSelectedProcess(selection: ProcessGetDTO) {
      this.updateSelectedProcess(selection);
  }

  updateSelectedProcess(newSelection) {
    this.selectedProcess = newSelection;
  }

  updateSelectedStep(step: StepGetDTO) {
    this.service
        .getStep(step.uri)
        .then((http: HttpResponse<OpenSilexResponse<StepGetDTO>>) => {
          let detailDTO: StepGetDTO = http.response.result;
          this.selectedStep = detailDTO;
        });
  }

  refresh() {
    if (this.processList) {
      this.processList.refresh(this.selectedProcess ? this.selectedProcess.uri : undefined);
    }
    if (this.stepView) {
      this.stepView.refresh();
    }
  }
}
</script>

<style scoped lang="scss">
</style>