<template>
  <span v-if="date" class="static-field-line">{{date }}</span>
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
import moment from "moment";

@Component
export default class XSDDateTimeView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  date = null;

  private langUnwatcher;

  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.onValueChange();
      }
    );
    this.onValueChange();
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
      this.date = moment
        .parseZone(this.value)
        .format(this.$t("dateTimeFormat"));
    }
  }
}
</script>

<style scoped lang="scss">
</style>

