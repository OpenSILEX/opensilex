<template>
  <opensilex-FormField 
    :rules="rules" 
    :required="required" 
    :label="label ||'component.common.type'"
    :validationDisabled="validationDisabled"
    >
    <!-- helpMessage="component.common.type.help-message" -->
    <template v-slot:field="field">
      <treeselect
        :id="field.id"
        :options="typesOptions"
        :load-options="initTypes"
        :placeholder="$t(placeholder)"
        :disabled="disabled"
        :allow-clearing-disabled="true"
        :allow-selecting-disabled-descendants="true"
        :flat="true"
        v-model="typeURI"
        @select="field.validator && field.validator.validate()"
        @close="field.validator && field.validator.validate()"
        @input="$emit('input', $event)"
      />
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { OntologyService, ResourceTreeDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class TypeForm extends Vue {
  $opensilex: any;
  $store: any;
  service: OntologyService;

  @PropSync("type")
  typeURI: string;

  @Prop()
  baseType: string;

  @Prop()
  label: string;

  @Prop()
  placeholder: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop({
    default: true
  })
  ignoreRoot: boolean;

  @Prop({
    default: false
  })
  validationDisabled: boolean;

  typesOptions = null;

  id: string;

  created() {
    this.service = this.$opensilex.getService("opensilex.OntologyService");
    this.id = this.$opensilex.generateID();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadTypes();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  initTypes({ action, parentNode, callback }) {
    this.loadTypes(callback);
  }

  loadTypes(callback?) {
    this.service
      .getSubClassesOf(this.baseType, this.ignoreRoot)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        this.typesOptions = this.$opensilex.buildTreeListOptions(
          http.response.result
        );
        if (callback) {
          callback();
        }
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

