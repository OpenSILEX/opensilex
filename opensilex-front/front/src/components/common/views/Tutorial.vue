<template>
  <v-tour
    v-if="!this.editMode"
    ref="tutorial"
    :steps="steps"
    :options="options"
    :callbacks="callbacks"
  ></v-tour>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class Tutorial extends Vue {
  $opensilex: any;
  $i18n: any;

  @Ref("tutorial") readonly tutorial!: any;

  @Prop({ default: false })
  editMode: boolean;

  get options(): any {
    return {
      useKeyboardNavigation: false,
      labels: {
        buttonSkip: this.$i18n.t("component.tutorial.skip-tour"),
        buttonPrevious: this.$i18n.t("component.tutorial.previous"),
        buttonNext: this.$i18n.t("component.tutorial.next"),
        buttonStop: this.$i18n.t("component.tutorial.finish"),
      },
    };
  }

  @Prop()
  steps: any[];

  get callbacks(): any {
    return {
      onPreviousStep: () => {
        this.$emit("onPreviousStep");
      },
      onNextStep: () => {
        this.$emit("onNextStep");
      },
      onFinish: () => {
        this.$emit("onFinish");
      },
      onSkip: () => {
        this.$emit("onSkip");
      },
      onStart: () => {
        this.$emit("onStart");
      },
    };
  }

  start() {
    this.tutorial.start();
  }

  skip() {
    this.tutorial.skip();
  }

  stop() {
    this.tutorial.stop();
  }

  finish() {
    this.tutorial.finish();
  }
}
</script>

<style  lang="scss">
// text color and base color
$v-tour-base-color: #00a38d !important;
$v-tour-base-text-color: black !important;
$v-tour-background-color: #fff !important;

.v-step__header {
  background-color: $v-tour-base-color;
  font-weight: bold;
}
.v-step {
  background-color: $v-tour-background-color;
}
.v-step__content {
  color: $v-tour-base-text-color;
}
.v-step__button {
  color: $v-tour-base-text-color;
  border: 0.05rem solid $v-tour-base-text-color;
}
</style>
<i18n>
  en:
    component:
      tutorial:
        next : Next
        skip-tour : Skip Tutorial
        previous : Previous
        finish : Finish
        label: Explain me by example
        name: Form Tutorial
  fr:
    component:
      tutorial:
        next : Suivant
        skip-tour : Passer tutoriel
        previous : Précédent
        finish : Fin
        label: Expliquez moi avec un exemple
        name: Tutoriel du formulaire
</i18n>
