<template>
  <div class="row">
    <!-- Start Date -->
    <div class="col-lg-6">
      <opensilex-DateForm
        :helpMessage="helpMessageStart"
        :label="labelStart"
        :max-date="dateEnd ? dateEnd : undefined"
        :placeholder="placeholderStart"
        :required="isRequiredStart"
        :value.sync="dateStart"
      ></opensilex-DateForm>
    </div>

    <!-- End Date -->
    <div class="col-lg-6">
      <opensilex-DateForm
        :helpMessage="helpMessageEnd"
        :label="labelEnd"
        :min-date="dateStart ? dateStart : undefined"
        :placeholder="placeholderEnd"
        :required="isRequiredEnd"
        :value.sync="dateEnd"
      ></opensilex-DateForm>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DateRangePickerForm extends Vue {
  $opensilex: any;
  $t: any;

  @Prop()
  labelStart: string;

  @Prop()
  labelEnd: string;

  @Prop()
  helpMessageStart: string;

  @Prop()
  helpMessageEnd: string;

  @Prop()
  placeholderStart: string;

  @Prop()
  placeholderEnd: string;

  @PropSync("requiredStart", { default: false })
  isRequiredStart: boolean;

  @PropSync("requiredEnd", { default: false })
  isRequiredEnd: boolean;

  @Prop({ default: false })
  disabled: boolean;

  @PropSync("start")
  dateStart: string;

  @PropSync("end")
  dateEnd: string;
}
</script>

<style lang="scss" scoped>
</style>

<i18n>
</i18n>