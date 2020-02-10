<template>
  <div>
    <b-form-group id="input-group-2" label="Experiment:" label-for="experiment">
      <b-form-select id="experiment" v-model="experiment" :options="experiments" @input="update">
        <template v-slot:first>
          <b-form-select-option :value="null">-- no experiment selected --</b-form-select-option>
        </template>
      </b-form-select>
    </b-form-group>
  </div>
</template>


<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { ExperimentsService } from "../../../lib/api/experiments.service";
import { Experiment } from "../../../lib/model/experiment";
import VueRouter from "vue-router";
import { EventBus } from "./../event-bus";

@Component
export default class ExperimentSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  $router: VueRouter;
  experiment: string = null;
  experiments: any = [];

  update() {
    EventBus.$emit("experienceHasChanged", this.experiment);
  }
  created() {
    EventBus.$on("imageTypeSelected", type => {
      this.experiment = null;
    });
    let service: ExperimentsService = this.$opensilex.getService(
      "opensilex.ExperimentsService"
    );
    const result = service
      .getExperimentsBySearch(
        this.user.getAuthorizationHeader(),
        100,
        0,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<Experiment>>>) => {
        const res = http.response.result as any;
        const data = res.data;
        data.forEach(element => {
          this.experiments.push({
            value: element.uri,
            text: element.alias
          });
        });
      })
      .catch(error => {
        console.log(error);
      });
  }
}
</script>


<style scoped lang="scss">
</style>
