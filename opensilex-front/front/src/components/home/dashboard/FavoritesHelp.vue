<template>
  <div v-if="isVisible" class="modal-overlay" @click.self="hide">
    <div class="modal-content">
      <header class="modal-header">
        <h4><i>{{ t("FavoritesHelp.title") }}</i></h4>
      </header>

      <p>{{ t("FavoritesHelp.body") }}
      <opensilex-Icon icon="fa#star" class="sm" /></p>

      <div class="modal-footer">
        <button type="button" class="btn greenThemeColor" @click="hide">
          {{ t("component.common.ok") }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineExpose } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const isVisible = ref(false);

const show = () => {
    console.log("SHOW")
  isVisible.value = true;
};

const hide = () => {
  isVisible.value = false;
};

// Exposer `show()` et `hide()` pour les appeler depuis un autre composant
defineExpose({ show, hide });
</script>

<style scoped>
/* Fond semi-transparent */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* Contenu de la modale */
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  min-width: 300px;
  max-width: 500px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

/* En-tête */
.modal-header {
  margin-bottom: 15px;
  font-size: 1.2em;
  font-weight: bold;
}

/* Bouton en bas */
.modal-footer {
  text-align: right;
  margin-top: 20px;
}
</style>

<i18n>
en:
  FavoritesHelp:
    title: About favorites
    body: You can add a scientific object, an experiment or a device as favorite by going to its description and clicking on the icon

fr:
  FavoritesHelp:
    title: À propos des favoris
    body: Vous pouvez ajouter un objet scientifique, une expérimentation ou un appareil comme favori en vous rendant sur sa description et en cliquant sur l'icône 
</i18n>
