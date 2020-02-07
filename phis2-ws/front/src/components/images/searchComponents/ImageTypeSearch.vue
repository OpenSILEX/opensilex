<template>
  <div>
    <b-form-group id="input-group-1" label="Image type:" label-for="type" label-class="required">
      <b-form-select id="type" v-model="rdfType" :options="types" @input="update" required>
        <template v-slot:first>
          <b-form-select-option :value="null" disabled>-- Please select an Image type --</b-form-select-option>
        </template>
      </b-form-select>
    </b-form-group>
  </div>
</template>


<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import { UriService } from "../../../lib/api/uri.service";
import { Uri } from "../../../lib/model/uri";
import VueRouter from "vue-router";
import { EventBus } from "./../event-bus";

@Component
export default class ImageTypeSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }
  $router: VueRouter;
  rdfType: string = null;
  types: any = [];

  imageTypeUri: string = "http://www.opensilex.org/vocabulary/oeso#Image";

  update() {
    this.$router
      .push({
        path: this.$route.fullPath,
        query: {
          rdfType: encodeURI(this.rdfType)
        }
      })
      .catch(function() {});
    EventBus.$emit("imageTypeSelected", this.rdfType);
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
        const res = http.response.result as any;
        res.data.forEach(element => {
          this.types.push({
            value: element.uri,
            text: element.uri.split("#")[1]
          });
        });
        let query: any = this.$route.query;
        if (query.rdfType) {
          this.rdfType = query.rdfType;
          EventBus.$emit("imageTypeSelected", this.rdfType);
        }
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped >
div >>> label.required::after {
  content: " * ";
  color: red;
}
</style>
