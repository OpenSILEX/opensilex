<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-input-group class="mt-3">
        <b-input-group-prepend>
          <country-flag :country="locale" size="normal" v-b-tooltip.hover :title="locale" />
        </b-input-group-prepend>
        <b-form-input
          :id="field.id"
          v-model="stringValue[locale]"
          :disabled="disabled"
          :type="type"
          :required="required"
          :placeholder="$t(placeholder)"
          :autocomplete="autocomplete"
        ></b-form-input>
      </b-input-group>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class LocalNameInputForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

  @Prop({
    default: "text"
  })
  type: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop()
  autocomplete: string;

  internalLocale: string;

  get locale(): string {
    if (this.internalLocale == null) {
      let availableLocalesFiltered = this.$i18n.availableLocales.filter(
        function(value, index, arr) {
          return value != "en";
        }
      );
      let locale = availableLocalesFiltered[0];
      return locale;
    } else {
      return "en";
    }
  }
}
</script>

<style scoped lang="scss">
</style>

