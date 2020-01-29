<template>
  <b-form-group label="Scientific object search">
    <b-form-tags v-model="value" no-outer-focus class="mb-2">
      <template v-slot="{ tags, disabled }">
        <ul v-if="tags.length > 0" class="list-inline d-inline-block mb-2">
          <li v-for="(tag, index) in tags" :key="tag" class="list-inline-item">
            <b-form-tag
              @remove="onRemove(index)"
              :title="tag"
              :disabled="disabled"
              variant="info"
            >{{ tag }}</b-form-tag>
          </li>
        </ul>
        <b-form-input
          v-model="search"
          list="input-list"
          id="input-with-list"
          @input="onWrite($event);"
          @change="onChange($event);"
          @keydown.enter.native.prevent="onEnter()"
        ></b-form-input>
        <b-form-datalist id="input-list" :options="options"></b-form-datalist>
      </template>
    </b-form-tags>
  </b-form-group>
</template>


<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { ScientificObjectsService } from "../../../lib/api/scientificObjects.service";
import { ScientificObjectDTO } from "../../../lib/model/scientificObjectDTO";

@Component
export default class ObjectSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  @Prop()
  selectedExperiment:any;
  alias:any=undefined;

  currentPage: number = 0;
  pageSize: number = 8000;
  options = [];
  search = "";
  value = [];
  valueWithURI={};
  selectedValueWithUri={};
  get criteria() {
    // Compute the search criteria
    return this.search.trim().toLowerCase();
  }

  get experiment(){
    return this.selectedExperiment;
  }
  get availableOptions() {
    const criteria = this.criteria;
    // Filter out already selected options
    const options = this.options.filter(opt => this.value.indexOf(opt) === -1);
    if (criteria) {
      // Show only options that match criteria
      return options.filter(opt => opt.toLowerCase().indexOf(criteria) > -1);
    }
    // Show all options available
    return options;
  }
  get searchDesc() {
    if (this.criteria && this.availableOptions.length === 0) {
      return "There are no tags matching your search criteria";
    }
    return "";
  }

  created() {
    this.options= [];
  }
  onOptionClick({ option, addTag }) {
    addTag(option);
    this.search = "";
  }
  onChange(selectedValue) {
    this.value.push(selectedValue);
    this.selectedValueWithUri={};
    this.value.forEach(element => {
        this.selectedValueWithUri[element]=this.valueWithURI[element];
      });
    this.search = "";
    this.$emit("searchObjectSelected", this.selectedValueWithUri);
  }
  onRemove(index) {
    this.value.splice(index, 1);
    this.selectedValueWithUri={};
    this.value.forEach(element => {
        this.selectedValueWithUri[element]=this.valueWithURI[element];
      });
    this.$emit("searchObjectSelected", this.selectedValueWithUri);
  }


  onWrite(value) {
    this.alias = value;
    let service: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    const result = service
      .getScientificObjectsBySearch(
        this.user.getAuthorizationHeader(),
        this.pageSize,
        this.currentPage,
        undefined,
        this.experiment,
        this.alias,
        undefined
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>) => {
          const res = http.response.result as any;
          const data = res.data;
          this.options = [];
          data.forEach(element => {
            this.options.push(element.label);
            this.valueWithURI[element.label]=element.uri;
          });
        }
      )
      .catch(error => {
        console.log(error);
      });
  }

  onEnter() {
    console.log("enter");
  }
}
</script>





<style scoped lang="scss">
</style>
