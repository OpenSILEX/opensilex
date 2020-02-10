<template>
  <div>
    <b-form >
      <phis2ws-ImageTypeSearch></phis2ws-ImageTypeSearch>

      <phis2ws-TimeSearch></phis2ws-TimeSearch>

      <phis2ws-ExperimentSearch></phis2ws-ExperimentSearch>

      <phis2ws-SciObjectTypeSearch></phis2ws-SciObjectTypeSearch>

      <phis2ws-SciObjectSearch></phis2ws-SciObjectSearch>

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

  getObjectList() {
    if (this.form.experiment === null && this.form.objectType === null) {
      this.form.objectList = [];
      this.$emit("onSearchFormSubmit", this.form);
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
            this.$emit("onSearchFormSubmit", this.form);
          }
        )
        .catch(error => {
          console.log(error);
          this.form.objectList = [];
          this.$emit("onSearchFormSubmit", this.form);
        });
    }
  }

  created() {
    EventBus.$on("experienceHasChanged", experience => {

      this.form.experiment = experience;
      if (experience === null) {
        this.form.objectList = [];
        this.$emit("onSearchFormSubmit", this.form);
      } else {
        this.getObjectList();
      }
    });
    EventBus.$on("soTypeHasChanged", type => {
      this.form.objectType = type;
      if (type === null) {
        this.form.objectList = [];
        this.$emit("onSearchFormSubmit", this.form);
      } else {
        this.getObjectList();
      }
    });
    EventBus.$on("imageTypeSelected", type => {
      this.form.rdfType = type;
      this.$emit("onSearchFormSubmit", this.form);
    });
    EventBus.$on("startDateHasChanged", startDate => {
      this.form.startDate = startDate;
      this.$emit("onSearchFormSubmit", this.form);
    });
    EventBus.$on("endDateHasChanged", endDate => {
      this.form.endDate = endDate;
      this.$emit("onSearchFormSubmit", this.form);
    });
    EventBus.$on("searchObjectSelected", sciObjects => {
      if (sciObjects.length === 0) {
        this.getObjectList();
      } else {
        this.form.objectList = [];
        for (let [key, val] of Object.entries(sciObjects)) {
          this.form.objectList.push(val);
        }
        this.$emit("onSearchFormSubmit", this.form);
      }
    });
  }
}
</script>

<style scoped lang="scss">
.btn-phis {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:hover,
.btn-phis:focus,
.btn-phis.active {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:focus {
  outline: 0;
  -webkit-box-shadow: none;
  box-shadow: none;
}
</style>
