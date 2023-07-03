<template>
  <span class="static-field-line">
    <opensilex-UriLink
      v-if="label"
      :uri="value"
      :value="label"
      :to="{path: '/germplasm/details/'+ encodeURIComponent(value)}"
      :target="target"
    ></opensilex-UriLink>
  </span>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Watch
} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService} from "opensilex-core/api/ontology.service";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GermplasmPropertyView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  value;

  @Prop({
    default: "_self"
  })
  target: string;

  label = "";

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
      this.$opensilex.disableLoader();
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
        })
        .finally(() => {
          this.$opensilex.enableLoader();
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>

