<template>
  <div>
    <b-form @submit="onSubmit" >
      <phis2ws-ImageTypeSearch :rdfType="form.rdfType" @imageTypeSelected="onImageTypeSelected"></phis2ws-ImageTypeSearch>
      <phis2ws-ExperimentSearch
        :experiment="selectedExperiment"
        @experimentSelected="onExperimentSelected"
      ></phis2ws-ExperimentSearch>
      <phis2ws-SciObjectSearch
        :selectedExperiment="selectedExperiment"
        @searchObjectSelected="onSearchObjectSelected"
      ></phis2ws-SciObjectSearch>

      <b-form inline>
        <label class="mr-sm-2" for="inline-form-custom-select-pref">Start Date</label>
        <b-input-group class="mt-3 mb-3" size="sm" id="inline-form-custom-select-pref">
          <datePicker
            v-model="form.startDate"
            input-class="form-control"
            placeholder="Select a date"
            :clear-button="true"
            @input="onStartDateSelected"
            @cleared="onStartDateCleared"
          ></datePicker>
        </b-input-group>

        <label class="mr-sm-2 ml-4" for="inline-2">End Date</label>
        <b-input-group class="mt-3 mb-3" size="sm" id="inline-2">
          <datePicker
            v-model="form.endDate"
            input-class="form-control"
            placeholder="Select a date"
            :clear-button="true"
            @input="onEndDateSelected"
            @cleared="onEndDateCleared"
          ></datePicker>
        </b-input-group>
      </b-form>

      <b-btn type="submit" variant="primary">Submit</b-btn>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { UriService } from "../../lib/api/uri.service";
import { Uri } from "../../lib/model/uri";
import VueRouter from "vue-router";

@Component
export default class ImageSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }
  selectedExperiment: any = "";
  $router: VueRouter;
  form: any = {
    rdfType: "",
    startDate: "",
    endDate: "",
    objectList: []
  };
  types: any = [];

  imageTypeUri: string = "http://www.opensilex.org/vocabulary/oeso#Image";

  onExperimentSelected(value) {
    //get the so (objectList) & emit onSearchFormChange event with the result
    //get the so type
    
    this.selectedExperiment = value;
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          experiment: encodeURI(value)
        }
      })
      .catch(function() {});
  }

  onImageTypeSelected(value) {
    this.form.rdfType = value;
    this.update();
  }

  onSubmit(evt) {
    evt.preventDefault();
    this.$emit("onSearchFormSubmit");
  }


  update() {
    this.$emit("onSearchFormChange", this.form);
  }

  onSearchObjectSelected(value) {
    this.form.objectList = [];
    // this.form.objectTagList = [];
    for (let [key, val] of Object.entries(value)) {
      this.form.objectList.push(val);
      //this.form.objectTagList.push(key);
    }
    this.update();
  }

  onStartDateSelected() {
    console.log("start date" + this.form.startDate);
    this.update();
  }

  onEndDateSelected() {
    console.log("end date" + this.form.endDate);
    this.update();
  }

  onStartDateCleared() {
    this.form.startDate = "";
  }
  onEndDateCleared() {
    this.form.endDate = "";
  }

  created() {
    let query: any = this.$route.query;

    if (query.startDate) {
      this.form.startDate = query.startDate;
    }
    if (query.endDate) {
      this.form.endDate = query.endDate;
    }
    if (query.rdfType) {
      this.form.rdfType = query.rdfType;
    }
    if (query.experiment) {
      this.selectedExperiment = query.experiment;
    }
    let service: UriService = this.$opensilex.getService(
      "opensilex.UriService"
    );
    const result = service
      .getDescendants(
        this.user.getAuthorizationHeader(),
        this.imageTypeUri,
        100,
        0
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<Uri>>>) => {
        const res = http.response.result as any;
        res.data.forEach(element => {
          this.types.push({
            value: element.uri,
            text: element.uri.split("#")[1]
          });
        });
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
