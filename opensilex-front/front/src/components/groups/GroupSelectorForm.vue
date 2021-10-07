<template>
  <b-form>
    <opensilex-GroupSelector
      label="component.infrastructure.groups"
      :groups.sync="form.groups"
      :multiple="true"
      @select="select"
      @deselect="deselect"
    ></opensilex-GroupSelector>
  </b-form>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
// @ts-ignore
import { InfrastructureUpdateDTO } from "opensilex-core/index";
// @ts-ignore
import { SecurityService } from "opensilex-security/index";

@Component
export default class GroupSelectorForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;

  @Prop({
    default: () => {
      return {
        groups: []
      };
    }
  })
  form: { groups: string };

  getEmptyForm() {
    return {
      groups: []
    };
  }

  create(form) {
    console.log("[GroupSelectorForm] create");
    console.log(form);
  }

  //Test
  select(value) {
    console.log("select");
    console.log(value);
    console.log(this.form);
  }

  deselect(value) {
    console.log("deselect");
    console.log(value);
    console.log(this.form);
  }
}
</script>

<style scoped lang="scss">
</style>

