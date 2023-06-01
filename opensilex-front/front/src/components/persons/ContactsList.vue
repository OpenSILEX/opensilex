<template>
  <div class="static-field">

    <span class="field-view-title">{{ $t(label) }}</span>

    <span class="static-field-line">
      <span :key="index" v-for="(value, index) in list">
        <opensilex-PersonContact
            :personContact="value"
        ></opensilex-PersonContact>
        <span v-if="index + 1 < list.length">, </span>
      </span>
    </span>

  </div>
</template>
<script lang="ts">
import { Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {PersonDTO} from "opensilex-security/index";

@Component
export default class ContactsList extends Vue {
  @Prop()
  label: string;

  @Prop()
  list: Array<PersonDTO>;

}
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}

</style>

