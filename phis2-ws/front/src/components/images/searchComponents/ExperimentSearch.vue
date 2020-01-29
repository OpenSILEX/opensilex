<template>
  <div>
    <b-form-group id="input-group-2" label="Experiment:" label-for="experiment">
      <b-form-select
        id="experiment"
        v-model="experiment"
        :options="experiments"
        @input="update"
        required
      >
        <template v-slot:first>
          <option :value="null" disabled>-- Please select an Experiment --</option>
        </template>
      </b-form-select>
    </b-form-group>
  </div>
</template>


<script lang="ts">
import { Component,Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { ExperimentsService } from "../../../lib/api/experiments.service";
import { Experiment } from "../../../lib/model/experiment";

@Component
export default class ExperimentSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  @Prop()
  experiment:string;
  experiments:any = [];

  update() {
      this.$emit("experimentSelected", this.experiment); 
  }
  created() {

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
