<template>
  <!--@submit prevent refresh page on submit -->
  <form ref="formRef"
    @submit="(event) => {event.preventDefault()}"
  >
    <b-input-group size="sm">
      <opensilex-FormInputLabelHelper
        class="mt-2 mr-2"
        v-if="label"
        :label="label"
        :helpMessage="helpMessage"
        :labelFor="id"
      ></opensilex-FormInputLabelHelper>

      <b-form-input
        :id="id"
        class="filter"
        :disabled="disabled"
        :value="filterValue"
        :type="type"
        :debounce="debounce"
        :lazy="lazy"
        :number="type=='number'"
        :placeholder="$t(placeholder)"
        @update="update($event)"
        @keyup.enter="onEnter"
      ></b-form-input>

      <template v-slot:append>
        <b-btn class="clear-btn" variant="outline-light" @click="clear()">
          <opensilex-Icon icon="fa#times" />
        </b-btn>
      </template>
    </b-input-group>
  </form>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class StringFilter extends Vue {
  $opensilex: any;

  @PropSync("filter")
  filterValue: string;

  @Prop()
  placeholder: string;

  @Prop()
  label: string;

  @Prop({ default: false })
  disabled: boolean;

  @Prop()
  helpMessage: string;

  @Prop({ default: "text" })
  type: string;

  @Prop()
  max: number;

  @Prop()
  min: number;

  @Prop({ default: 0 })
  debounce: number;

  @Prop({ default : true })
  lazy: boolean;

  @Ref("formRef") readonly formRef!: any;

  update(value) {
    if (value != this.filterValue) {
      if (this.type == "number") {
        let valid = true;
        if (value) {
          let numberValue = Number.parseInt(value);
          if (!Number.isNaN(numberValue)) {
            if (this.min) {
              valid = numberValue >= this.min;
            }
            if (this.max) {
              valid = valid && numberValue <= this.max;
            }
          } else {
            valid = false;
          }
        }
        if (valid || value == "") {
          this.filterValue = value;
          this.$emit("update", this.filterValue);
        }
      } else {
        this.filterValue = value;
        this.$emit("update", this.filterValue);
      }
    }
  }

  clear() {
    this.formRef.reset();
    this.update("");
  }
  id: string;

  created() {
    this.id = this.$opensilex.generateID();
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }

}
</script>

<style scoped lang="scss">
.filter {
  font-size: 13px;
  border-right: none;
}

.stringFilterLabel {
  margin-bottom: 0px !important;
}

.clear-btn {
  color: rgb(229, 227, 227) !important;
  border-color: rgb(229, 227, 227) !important;
  border-left: none !important;
}

.clear-btn:hover,
.clear-btn:focus,
.clear-btn:active {
  box-shadow: none !important;
  color: rgb(229, 57, 53) !important;
  background-color: transparent !important;
  border-left: none !important;
}

// Remove arrows on number filter
/* Chrome, Safari, Edge, Opera */
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* Firefox */
input[type=number] {
  -moz-appearance: textfield;
}

</style>

