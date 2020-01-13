<template>
  <div>
    <b-form @submit="onSubmit" @reset="onReset">
      <b-form-group id="input-group-3" label="Type:" label-for="type">
        <b-form-select id="type" v-model="form.rdfType" :options="types" required>
          <template v-slot:first>
            <option :value="null" disabled>-- Please select an Image type --</option>
          </template>
        </b-form-select>
      </b-form-group>

      <b-button type="submit" variant="primary">Submit</b-button>
      <b-button type="reset" variant="danger">Reset</b-button>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component} from "vue-property-decorator";
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
    this.$emit("onSearchFormSubmit",this.form);
  }

  onReset(evt) {
    evt.preventDefault();
    // Reset our form values
    this.form.food = null;
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
