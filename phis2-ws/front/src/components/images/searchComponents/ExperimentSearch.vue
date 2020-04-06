<template>
  <div>
    <b-form-group id="input-group-2" label="Experiment:" label-for="experiment">
      <b-form-select
        id="experiment"
        v-model="experiment"
        :options="experiments"
        @input="update"
        @focus.native="userAction"
      >
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
  // the way to see if it is select by a user or by the change of an other element of the form
  // In this case the images will be asynch load for nothing
  userFocus = false;
  $router: VueRouter;
  experiment: string = null;
  experiments: any = [];

  update() {
    if (this.userFocus) {
      EventBus.$emit("experienceHasChanged", this.experiment);
    }
  }
  userAction() {
    this.userFocus = true;
  }
  created() {
    // EventBus.$on("imageTypeSelected", type => {
    //   this.userFocus = false;
    //   this.experiment = null;
    // });
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
