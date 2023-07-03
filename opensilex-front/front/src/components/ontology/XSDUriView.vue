<template>
  <span class="static-field-line">
    <opensilex-UriLink v-if="label" :uri="value" :value="label"></opensilex-UriLink>
  </span>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Watch
} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";

@Component
export default class XSDUriView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  value;

  label = "";

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
      this.$opensilex
        .getService<OntologyService>("opensilex.OntologyService")
        .getURILabel(this.value)
        .then(http => {
          this.label = http.response.result;
        })
        .catch((http: HttpResponse<OpenSilexResponse<string>>) => {
          if (http.status === 404) {
            this.label = this.value;
          } else {
            this.$opensilex.errorHandler(http);
          }
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>

