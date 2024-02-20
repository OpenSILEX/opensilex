<template>
  <opensilex-FormField
      :rules="rules"
      :required="required"
      :label="label ||'component.common.type'"
      :helpMessage="helpMessage ||'component.common.type'"
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
        :multiple="multiple"
        @select="field.validator && field.validator.validate(); $emit('select',$event)"
        @close="field.validator && field.validator.validate()"
        @input="$emit('input', $event)"
        @open="$emit('open', $event)"
        @keyup.enter.native="onEnter"
      />
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";
import { OntologyService, ResourceTreeDTO } from "opensilex-core/index";
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
  helpMessage: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop({default: false})
  multiple: boolean;

  @Prop()
  rules: string | Function;

  @Prop({
    default: true
  })
  ignoreRoot: boolean;

  /**
   * If we want to make some types hidden from the possible choices
   */
  @Prop({
    default: []
  })
  unselectableTypes: Array<string>;

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
    this.$opensilex.disableLoader();
    this.service
      .getSubClassesOf(this.baseType, this.ignoreRoot)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        this.$opensilex.enableLoader();
        this.typesOptions = this.$opensilex.buildTreeListOptions(
          http.response.result,
          {
            expanded: null,
            disableSubTree: null,
            nodesToIgnoreList: this.unselectableTypes.map(e => this.$opensilex.getLongUri(e))
          }
        );
        if (callback) {
          callback();
        }
      })
      .catch((error) => {
        this.$opensilex.enableLoader();
        this.$opensilex.errorHandler;
        });
  }
  onEnter() {
    this.$emit("handlingEnterKey")
  }
}
</script>

<style scoped lang="scss">
</style>

