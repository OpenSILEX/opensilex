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

@Component
export default class XSDDateTimeView extends Vue {
  $opensilex: any;
  $store: any;

  @Prop()
  value;

  date: string = undefined;

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
      this.date = new Date(this.value).toISOString();
    }
  }
}
</script>

<style scoped lang="scss">
</style>

