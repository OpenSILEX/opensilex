<template>
  <b-modal
      v-model="isVisible"
      :title="$t('Annotation.details')"
      hide-footer
      no-close-on-backdrop
      no-close-on-esc
      centered
  >
    <div class="details-container">
      <p><strong>URI :</strong> {{ annotationDetails.uri || "Non spécifié" }}</p>
      <p><strong>Motivation :</strong> {{ annotationDetails.motivation && annotationDetails.motivation.name ? annotationDetails.motivation.name : "Non spécifié" }}</p>
    </div>
    <div class="text-right">
      <b-button variant="primary" class="helpButton" @click="close">OK</b-button>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";

@Component
export default class AnnotationDetails extends Vue {
  @Prop({ default: () => ({}) }) annotationDetails!: { uri?: string; motivation?: { name?: string } };
  @Prop({ default: false }) value!: boolean;

  get isVisible() {
    return this.value;
  }

  set isVisible(value: boolean) {
    this.$emit("input", value); // Synchronisation avec le parent
  }

  close() {
    this.$emit("input", false); // Ferme la modale correctement
    this.$emit("close"); // Envoie un événement "close" au parent si besoin
  }
}
</script>

<style scoped>

.details-container {
  text-align: left;
  padding: 10px 20px;
}

.text-right {
  margin-top: 15px;
}

.helpButton {
  margin-left: 25px;
  color: #f1f1f1;
  background-color: #00A28C;
  border: none;
}

/* Hover effect */
.helpButton:hover {
  background-color: #00A2B0;
  color: #f1f1f1;
  border: none;
}

</style>
<i18n>
en:
  Annotation:
    details: Details annotation
fr:
  Annotation:
    details: Détails de l'annotation
</i18n>

