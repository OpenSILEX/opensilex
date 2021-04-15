<template>
  <div class="static-field">
    <span class="field-view-title">{{ $t(title) }}:</span>
    <span class="static-field-line">
      <opensilex-UriLink
        v-if="label"
        :uri="value"
        :to="{ path: '/infrastructure/details/' + encodeURIComponent(uri) }"
        :value="label"
      ></opensilex-UriLink>
    </span>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Watch,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class InfrastructureUriView extends Vue {
  $opensilex: any;

  @Prop({
    default: "InfrastructureUriView.title",
  })
  title;

  @Prop()
  uri;

  label = "";

  mounted() {
    this.onValueChange();
  }

  @Watch("uri")
  onValueChange() {
    if (this.uri) {
      this.$opensilex
        .getService("opensilex.OntologyService")
        .getURILabel(this.uri)
        .then((http) => {
          this.label = http.response.result;
        })
        .catch(() => {
          this.label = this.uri;
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  InfrastructureUriView:
    title: Organization
fr:
  InfrastructureUriView:
    title: Organisation
</i18n>
