<template>
  <div>
    <b-form @submit="onSubmit">
      <phis2ws-ImageTypeSearch></phis2ws-ImageTypeSearch>

      <phis2ws-TimeSearch></phis2ws-TimeSearch>

      <phis2ws-ExperimentSearch></phis2ws-ExperimentSearch>

      <phis2ws-SciObjectTypeSearch></phis2ws-SciObjectTypeSearch>

      <phis2ws-SciObjectSearch></phis2ws-SciObjectSearch>

      <b-btn type="submit" variant="primary">Submit <font-awesome-icon icon="search" size="sm" /></b-btn>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ScientificObjectsService } from "../../lib/api/scientificObjects.service";
import { ScientificObjectDTO } from "../../lib/model/scientificObjectDTO";
import { EventBus } from "./event-bus";

@Component
export default class ImageSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }
  form: any = {
    rdfType: null,
    startDate: undefined,
    endDate: undefined,
    objectList: [],
    objectType: null,
    experiment: null
  };

  onSubmit(evt) {
    evt.preventDefault();
    this.$emit("onSearchFormSubmit", this.form);
  }

  getObjectList() {
    if (this.form.experiment === null && this.form.objectType === null) {
      this.form.objectList = [];
    } else {
      let service: ScientificObjectsService = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
      );

      //the fields of rest service must be undefined to be ignored
      let selectedExperimentField: string;
      let selectedSoTypeField: string;
      if (this.form.objectType === null) {
        selectedSoTypeField = undefined;
      } else {
        selectedSoTypeField = this.form.objectType;
      }
      if (this.form.experiment === null) {
        selectedExperimentField = undefined;
      } else {
        selectedExperimentField = this.form.experiment;
      }

      const result = service
        .getScientificObjectsBySearch(
          this.user.getAuthorizationHeader(),
          8000,
          0,
          undefined,
          selectedExperimentField,
          undefined,
          selectedSoTypeField
        )
        .then(
          (
            http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>
          ) => {
            const res = http.response.result as any;
            const data = res.data;
            this.form.objectList = [];
            data.forEach(element => {
              this.form.objectList.push(element.uri);
            });
          }
        )
        .catch(error => {
          console.log(error);
          this.form.objectList = [];
        });
    }
  }

  created() {
    EventBus.$on("experienceHasChanged", experience => {
      this.form.experiment = experience;
      this.getObjectList();
    });
    EventBus.$on("soTypeHasChanged", type => {
      this.form.objectType = type;
      this.getObjectList();
    });
    EventBus.$on("imageTypeSelected", type => {
      this.form.rdfType = type;
    });
    EventBus.$on("startDateHasChanged", startDate => {
      this.form.startDate = startDate;
    });
    EventBus.$on("endDateHasChanged", endDate => {
      this.form.endDate = endDate;
    });
    EventBus.$on("searchObjectSelected", sciObjects => {
      if (sciObjects.length === 0) {
        this.getObjectList();
      } else {
        this.form.objectList = [];
        for (let [key, val] of Object.entries(sciObjects)) {
          this.form.objectList.push(val);
        }
      }
    });

    
  }
}
</script>

<style scoped lang="scss">
</style>
