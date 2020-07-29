<template>
  <div>
    <opensilex-CreateButton @click="show" label="OntologyPropertySelector.add-property"></opensilex-CreateButton>
    <b-modal ref="propertySelector" @ok.prevent="update" size="md" :static="true">
      <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
      <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

      <template v-slot:modal-title>
        <i>
          <slot name="icon">
            <opensilex-Icon icon="fa#eye" class="icon-title" />
          </slot>
          <span>{{$t("OntologyPropertySelector.add-property")}}</span>
        </i>
      </template>

      <opensilex-SelectForm
        :selected.sync="selected"
        :options="facilititesInfraTree"
        :multiple="true"
        label="ExperimentFacilitySelector.label"
        placeholder="ExperimentFacilitySelector.placeholder"
      >
        <template v-slot:option-label="{node}">
          <opensilex-Icon :icon="$opensilex.getRDFIcon(node.raw.type)" />&nbsp;
          <span class="capitalize-first-letter">{{node.label}}</span>&nbsp;
          (
          <span class="capitalize-first-letter">{{node.raw.typeLabel}}</span>)
        </template>
        <template v-slot:value-label="{node}">
          <opensilex-Icon :icon="$opensilex.getRDFIcon(node.raw.type)" />&nbsp;
          <span class="capitalize-first-letter">{{node.label}}</span>
        </template>
      </opensilex-SelectForm>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class OntologyPropertySelector extends Vue {
  $opensilex: any;

  @Prop()
  domainURI;
  
}
</script>

<style scoped lang="scss">
</style>


<i18n>
</i18n>
