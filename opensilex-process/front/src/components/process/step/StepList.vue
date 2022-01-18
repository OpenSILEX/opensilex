<template>
  <b-card>
    <template v-slot:header>
      <h3>
        {{ $t("component.process.step.title") }}
        &nbsp;
        <font-awesome-icon
          icon="question-circle"
          v-b-tooltip.hover.top="
            $t('component.process.step.step-help')
          "
        />
      </h3>
      <div class="card-header-right" v-if="withActions">
        <!-- TO DO : CREDENTIAL
             v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
        <opensilex-CreateButton
          @click="stepForm.showCreateForm()"
          label="component.process.step.add"
        ></opensilex-CreateButton>
      </div>
    </template>

    <b-table
      striped
      hover
      small
      responsive
      sort-icon-left
      sort-by="name"
      :selectable="isSelectable"
      selectMode="single"
      :items="displayableSteps"
      :fields="fields"
      @row-selected="onStepSelected"
      ref="stepTable"
    >
      <template v-slot:head(name)="data">{{ $t(data.label) }}</template>
      <template v-slot:head(actions)="data" v-if="withActions">{{ $t(data.label) }}</template>

      <template v-slot:cell(name)="data">
        <opensilex-UriLink
          :to="{
            path:
              '/process/step/details/' +
              encodeURIComponent(data.item.uri),
          }"
          :uri="data.item.uri"
          :value="data.item.name"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(actions)="data" v-if="withActions">
        <b-button-group size="sm">
        <!-- TO DO : CREDENTIAL
             v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
          <opensilex-EditButton
            @click="editStep(data.item)"
            label="component.process.step.edit"
            :small="true"
          ></opensilex-EditButton>

        <!-- TO DO : CREDENTIAL
             v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
          <opensilex-DeleteButton
            @click="deleteStep(data.item.uri)"
            label="component.process.step.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </b-table>

    <!-- TO DO : CREDENTIAL
            v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
    <opensilex-StepForm
      ref="stepForm"
      v-if="withActions"
      @onCreate="onCreate"
      @onUpdate="onUpdate"
    ></opensilex-StepForm>
  </b-card>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {ProcessService} from "opensilex-process/api/process.service";
import {StepGetDTO} from "opensilex-process/model/StepGetDTO";
import {NamedResourceDTOStepModel} from "opensilex-process/model/NamedResourceDTOStepModel";

@Component
export default class StepList extends Vue {
  $opensilex: any;
  $store: any;
  service: ProcessService;

  @Ref("stepForm") readonly stepForm!: any;
  @Ref("stepTable") readonly stepTable: any;

  @Prop()
  steps: Array<NamedResourceDTOStepModel>;

  fetchedSteps: Array<NamedResourceDTOStepModel> = [];
  selectedStep: NamedResourceDTOStepModel = undefined;

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false
  })
  withActions: boolean;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get displayableSteps() {
    if (Array.isArray(this.steps)) {
      return this.steps;
    }
    return this.fetchedSteps;
  }

  get fields() {
    let fields = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      }
    ];
    if (this.withActions) {
      fields.push({
        label: "component.common.actions",
        key: "actions",
        sortable: false
      });
    }
    return fields;
  }

  public deleteStep(uri) {
    this.$opensilex
      .getService("opensilex.ProcessService")
      .deleteStep(uri)
      .then(() => {
        this.$emit("onDelete", uri);
      });
  }

  onStepSelected(selected: Array<StepGetDTO>) {
    this.selectedStep = selected[0];
    this.$emit('stepSelected', this.selectedStep);
  }

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-process.ProcessService"
    );

    this.refresh();
  }

  refresh() {
    this.stepTable.refresh();
  }

  createStep() {
    this.stepForm.showCreateForm()
  }

  editStep(step: StepGetDTO) {
    this.stepForm.showEditForm(step);
  }

  onUpdate() {
    this.$emit("onUpdate");
  }

  onCreate() {
    this.$emit("onCreate");
  }
}
</script>

<style scoped lang="scss">
</style>