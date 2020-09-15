<template>
  <span v-if="date" class="static-field-line">{{ $d(date, 'short') }}</span>
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
export default class XSDDateView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  date = null;

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
     this.date = Date.parse(this.value);
    }
  }
}
</script>

<style scoped lang="scss">
</style>

