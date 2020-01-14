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
          v-model="form.soUri"
          type="text"
          debounce="300"
          placeholder="Enter URI"
        ></b-form-input>
        <template v-slot:append>
          <b-btn :disabled="!form.soUri" variant="primary" @click="form.soUri = ''">
            <font-awesome-icon icon="times" size="sm" />
          </b-btn>
        </template>

      </b-input-group>
      <b-btn type="submit" variant="primary">
          Submit</b-btn>
      <b-btn type="reset" variant="danger">Reset</b-btn>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { UriService } from "../../lib/api/uri.service";
import { Uri } from "../../lib/model/uri";

@Component
export default class ImageSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  form: any = {
    type: null
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
    this.form.food = null;
    this.form.soUri = null;
  }

  created() {
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
