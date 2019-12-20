<template>
  <div>
    <b-form-checkbox-group
      v-for="credential in credentialsGroup"
      v-bind:key="credential.id"
      v-model="selectedCredentialsGroups[credentialsGroup.groupId]"
      :options="credentialOptions[credentialsGroup.groupId]"
      switches
    ></b-form-checkbox-group>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ProfileCredentialGroupForm extends Vue {
  selected: [];
  
  
  
  // Must be an array reference!
  options: [
    { text: "Red"; value: "red" },
    { text: "Green"; value: "green" },
    { text: "Yellow (disabled)"; value: "yellow"; disabled: true },
    { text: "Blue"; value: "blue" }
  ];
}
</script>

<style scoped lang="scss">
</style>
