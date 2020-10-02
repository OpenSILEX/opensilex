<template>
  <opensilex-Button
    @click="deleteAction"
    variant="outline-danger"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#trash-alt"
  >
  </opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DeleteButton extends Vue {
  @Prop()
  label: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;

  deleteAction() {
    this.$bvModal
      .msgBoxConfirm(
        this.$t("component.common.delete-confirmation").toString(),
        {
          cancelTitle: this.$t("component.common.cancel").toString(),
          okTitle: this.$t("component.common.delete").toString(),
          okVariant: "danger",
          centered: true
        }
      )
      .then(confirmation => {
        if (confirmation) {
          this.$emit("click");
        }
      });
  }
}
</script>

<style scoped lang="scss">
</style>

