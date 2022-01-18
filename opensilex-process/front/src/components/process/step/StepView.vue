<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-file-text"
      title="component.process.step.title"
      description="component.process.step.description"
    ></opensilex-PageHeader>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-StepList
          ref="stepList"
          :redirectAfterCreation="true"
        ></opensilex-StepList>
      </template>
    </opensilex-PageContent>

  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { StepsService } from "opensilex-process/index";

@Component
export default class StepView extends Vue {
  $opensilex: any;
  service: StepsService;
  $store: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("stepForm") readonly stepForm!: any;
  @Ref("stepList") readonly stepList!: any;
  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
      this.tableRef.refresh();
  }
  
   created() {
    console.debug("Loading form view...");
    this.service = this.$opensilex.getService("opensilex.StepsService");
  }

}
</script>

<style scoped lang="scss">
</style>