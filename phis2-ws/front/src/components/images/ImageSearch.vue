<template>
  <div>
    <b-form @submit="onSubmit" @reset="onReset">
      <b-form-group id="input-group-1" label="Type:" label-for="type">
        <b-form-select id="type" v-model="form.rdfType" :options="types" required>
          <template v-slot:first>
            <option :value="null" disabled>-- Please select an Image type --</option>
          </template>
        </b-form-select>
      </b-form-group>

      <b-input-group class="mt-3 mb-3" size="sm" label="Scientific Object URI" label-for="soUri">
        <b-form-input
          id="soUri"
          v-model="form.concernedItems"
          type="text"
          debounce="300"
          placeholder="Enter URI"
        ></b-form-input>
        <template v-slot:append>
          <b-btn
            :disabled="!form.concernedItems"
            variant="primary"
            @click="form.concernedItems = ''"
          >
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>
      </b-input-group>

      <b-form inline>
        <label class="mr-sm-2" for="inline-form-custom-select-pref">Start Date</label>
        <b-input-group class="mt-3 mb-3" size="sm" id="inline-form-custom-select-pref">
          <datePicker
            v-model="form.startDate"
            input-class="form-control"
            placeholder="Select a date"
            :clear-button="true"
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
            @cleared="onEndDateCleared"
          ></datePicker>
        </b-input-group>
      </b-form>

      <b-btn type="submit" variant="primary">Submit</b-btn>
      <b-btn type="reset" variant="danger">Reset</b-btn>
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

  $router: VueRouter;
  form: any = {
    rdfType: "",
    concernedItems: "",
    startDate: "",
    endDate: ""
  };
  types: any = [];

  imageTypeUri: string = "http://www.opensilex.org/vocabulary/oeso#Image";

  onSubmit(evt) {
    evt.preventDefault();
    this.$emit("onSearchFormSubmit", this.form);
  }

  onReset(evt) {
    evt.preventDefault();
    // Reset our form values
  }

  onStartDateCleared() {
    console.log("clear date");
    this.form.startDate = "";
  }
  onEndDateCleared() {
    console.log("clear date");
    this.form.endDate = "";
  }

  created() {
    let query: any = this.$route.query;
    if (query.concernedItems) {
      this.form.concernedItems = query.concernedItems;
    }
    if (query.startDate) {
      this.form.startDate = query.startDate;
    }
    if (query.endDate) {
      this.form.endDate = query.endDate;
    }
    if (query.rdfType) {
      this.form.rdfType = query.rdfType;
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
        console.log(http.response.result);
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
