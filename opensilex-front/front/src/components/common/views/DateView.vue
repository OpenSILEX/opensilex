<template>
  <div class="static-field"  v-if="label">
    <span class="field-view-title">{{ $t(label) }}</span>
    <span
        :title="formattedISODate"
    >
      {{ formattedDate }}
    </span>
  </div>
  <span
      v-else
      :title="formattedISODate"
  >
    {{ formattedDate }}
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class DateView extends Vue {
  $i18n: any;
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  value: string;

  @Prop()
  label: string;

  @Prop()
  isDatetime: boolean;

  @Prop()
  dateTimeFormatOptions: Intl.DateTimeFormatOptions;

  @Prop({default: false})
  useLocaleFormat: boolean;

  get formattedDate(): string {
    if (this.useLocaleFormat) {
      return this.formattedLocaleDate;
    }
    return this.formattedISODate;
  }

  get formattedISODate(): string {
    if(!this.value) {
      return undefined;
    }
    if (this.isDatetime) {
      return this.$opensilex.$dateTimeFormatter.formatISODateTime(this.value);
    }
    return this.$opensilex.$dateTimeFormatter.formatISODate(this.value);
  }

  get formattedLocaleDate(): string {
    if(!this.value) {
      return undefined;
    }
    if (this.isDatetime) {
      return this.$opensilex.$dateTimeFormatter.formatLocaleDateTime(this.value, this.dateTimeFormatOptions);
    }
    return this.$opensilex.$dateTimeFormatter.formatLocaleDate(this.value, this.dateTimeFormatOptions);
  }
}
</script>

<style scoped lang="scss">
</style>

