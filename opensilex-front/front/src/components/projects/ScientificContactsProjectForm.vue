<template>
  <b-form-group >
    
         <label class="mr-sm-2 ">{{label}}:</label>
       
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
          list="alias-input-list"
          id="alias-input-with-list"
          placeholder="Enter contact to search "
          autocomplete="off"
          @input="onWrite($event)"
        ></b-form-input>
        <datalist id="alias-input-list">
          <option v-for="option in options" :key="option">{{ option }}</option>
        </datalist>
      </template>
    </b-form-tags>
  </b-form-group>
</template>

<script lang="ts">
import { Component,Prop } from "vue-property-decorator";
import Vue from "vue";

import { SecurityService, UserGetDTO } from "opensilex-security/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class ScientificContactsProjectForm extends Vue {
  $opensilex: any;
  service: SecurityService;
  $i18n: any;

  @Prop()
  label: any;
  selectedSoType: any;
  alias: any = undefined;
  options = [];
  search = "";
  value = [];
  valueWithURI = {};

  sortBy = "firstName";
  sortDesc = false;
  currentPage: number = 1;
  pageSize = 800;
  created() {
    this.service = this.$opensilex.getService("opensilex.SecurityService");
  }

  onWrite(value) {
    if (this.options.includes(value)) {
      this.onChange(value);
    } else {
      let orderBy = [];
      if (this.sortBy) {
        let orderByText = this.sortBy + "=";
        if (this.sortDesc) {
          orderBy.push(orderByText + "desc");
        } else {
          orderBy.push(orderByText + "asc");
        }
      }
      this.service
        .searchUsers(value, orderBy, this.currentPage - 1, this.pageSize)
        .then((http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) => {
          const res = http.response.result as any;
          console.log(res);
          this.options = [];
          res.forEach(element => {
            this.options.push(element.firstName + " " + element.lastName);
            this.valueWithURI[element.firstName + " " + element.lastName] =
              element.uri;
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  onChange(selectedValue) {
    console.log("onChange");
    this.value.push(selectedValue);
    console.log("Values: " + this.value);
    let uriValues = [];
    this.value.forEach(element => {
      uriValues.push(this.valueWithURI[element]);
    });
    this.search = "";
    this.$emit("onSelect", uriValues);
  }

  onRemove(index) {
    console.log("onRemove");
    console.log("Values before: " + this.value);
    this.value.splice(index, 1);
    console.log("Values: " + this.value);
    let uriValues = [];
    this.value.forEach(element => {
      uriValues.push(this.valueWithURI[element]);
    });
    this.search = "";
  }
}
</script>