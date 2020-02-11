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
import { ScientificObjectsService } from "../../../lib/api/scientificObjects.service";
import { ScientificObjectDTO } from "../../../lib/model/scientificObjectDTO";
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
  allTypes: any = [];
  scientificObjectService: ScientificObjectsService = this.$opensilex.getService(
    "opensilex.ScientificObjectsService"
  );
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
        this.allTypes = this.soTypes;
      })
      .catch(this.$opensilex.errorHandler);

    EventBus.$on("imageTypeSelected", type => {
      this.soTypes = this.allTypes;
      this.type = null;
    });

    EventBus.$on("experienceHasChanged", experience => {
      if (experience === null) {
        this.soTypes = this.allTypes;
        this.type = null;
      } else {
        this.findTypeByExperimentObjects(experience);
        this.type = null;
      }
    });
  }

  findTypeByExperimentObjects(experience) {
    const result = this.scientificObjectService
      .getScientificObjectsBySearch(
        this.user.getAuthorizationHeader(),
        8000,
        0,
        undefined,
        experience,
        undefined,
        undefined
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>) => {
          const res = http.response.result as any;
          const data = res.data;
          let uriTypes = [];
          data.forEach(element => {
            uriTypes.push(element.rdfType);
          });
          uriTypes = [...new Set(uriTypes)];
          const types = [];
          uriTypes.forEach(element => {
            types.push({
              value: element,
              text: element.split("#")[1]
            });
          });
          this.soTypes = types;
        }
      )
      .catch(error => {
        console.log(error);
      });
  }
}
</script>

<style scoped >
</style>
