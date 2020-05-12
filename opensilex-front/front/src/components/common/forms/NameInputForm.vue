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
          <country-flag
            :country="countryCode"
            size="normal"
            v-b-tooltip.hover
            :title="languageCode"
          />
        </b-input-group-prepend>
        <b-form-input
          :id="field.id"
          v-model="stringMap[languageCode]"
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
export default class NameInputForm extends Vue {
  $opensilex: any;
  $i18n: any;

  @PropSync("value", {
    default: () => {
      return {};
    }
  })
  stringMap: Map<string, string>;

  mounted() {
    console.log(this.stringMap);
  }

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

  @Prop({ default: false })
  localName: boolean;

  countryCode: string;
  
  created() {
    this.countryCode = "gb";
  }

  get languageCode(): string {
    if (this.localName) {
      let localLangCode = this.$opensilex.getLocalLangCode();
      this.countryCode = localLangCode;
      return localLangCode;
    } else {
      this.countryCode = "gb";
      return "en";
    }
  }
}
</script>

<style scoped lang="scss">
</style>

