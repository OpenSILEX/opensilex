<template>
  <div>
    <b-form-group
      id="input-group-3"
      label="Scientific object type:"
      label-for="sciObjectTypeSearch"
      label-class="required"
    >
      <b-form-select id="sciObjectTypeSearch" v-model="type" :options="soTypes" @input="update">
        <template v-slot:first>
          <b-form-select-option :value="null">--No object Type selected--</b-form-select-option>
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
import { EventBus } from "./../event-bus";

@Component
export default class SciObjectTypeSearch extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }

  soTypes: any = [];
  type: string = null;

  sciObjectUri: string =
    "http://www.opensilex.org/vocabulary/oeso#ScientificObject";

  update() {
    EventBus.$emit("soTypeHasChanged", this.type);
  }

  created() {
    let service: UriService = this.$opensilex.getService(
      "opensilex.UriService"
    );
    const result = service
      .getDescendants(
        this.user.getAuthorizationHeader(),
        this.sciObjectUri,
        100,
        0
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<Uri>>>) => {
        const res = http.response.result as any;
        res.data.forEach(element => {
          this.soTypes.push({
            value: element.uri,
            text: element.uri.split("#")[1]
          });
        });

      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped >
</style>
