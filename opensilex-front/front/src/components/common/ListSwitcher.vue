<template>
  <div>
    <b-list-group>
      <b-list-group-item
        button
        v-for="initialItem in initialList"
        v-bind:key="initialItem.id"
        v-bind:active="currentInitialSelection.indexOf(initialItem.id) >= 0"
      >{{initialItem.label}}</b-list-group-item>
    </b-list-group>
    <b-list-group>
      <b-list-group-item
        button
        v-for="selectedItem in selectedList"
        v-bind:key="selectedItem.id"
        v-bind:active="currentSelectedSelection.indexOf(selectedItem.id) >= 0"
      >{{selectedItem.label}}<b-form-select v-model="selectedItem.profileId" :options="userProfiles"></b-form-select></b-list-group-item>
    </b-list-group>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ListSwitcher extends Vue {
  @Prop()
  initialList: Array<any>;

  currentInitialSelection: Array<string> = [];

  @Prop()
  selectedList: Array<any>;

  currentSelectedSelection: Array<string> = [];

  @Prop()
  userProfiles: Array<any>;

}
</script>

<style scoped lang="scss">
</style>

