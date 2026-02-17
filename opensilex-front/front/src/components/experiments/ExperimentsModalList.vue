<template>
  <div>
    <div v-if="!experiments || experiments.length === 0" id="no-experiment">
      {{ t("ExperimentsModalList.no_experiments") }}
    </div>

    <button
      v-else
      id="number"
      type="button"
      class="btn btn-link p-0 "
      @click="showPopup"
    >
      {{ experiments.length }}
    </button>

    <!-- modal -->
    <teleport to="body">
      <div
        v-if="isOpen"
        class="modal fade show"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        :aria-label="t('ExperimentsModalList.title', [currentFacility?.name])"
        style="display: block;"
        @click.self="closePopup"
      >
        <div class="modal-dialog modal-dialog-centered" role="document">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">
                {{ t("ExperimentsModalList.title", [currentFacility?.name]) }}
              </h5>
              <button type="button" class="btn-close" aria-label="Close" @click="closePopup" />
            </div>

            <div class="modal-body">
                <opensilex-ExperimentSimpleList :experimentList="experiments" />
            </div>
          </div>
        </div>
      </div>

      <!-- modal backdrop -->
      <div v-if="isOpen" class="modal-backdrop fade show" />
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { NamedResourceDTO } from "opensilex-core/model/namedResourceDTO";
import type { ExperimentGetListDTO } from "opensilex-core/model/experimentGetListDTO";

const { t } = useI18n();

const props = defineProps<{
  experiments: ExperimentGetListDTO[];
  currentFacility?: NamedResourceDTO | null;
}>();

const isOpen = ref(false);

function showPopup() {
  isOpen.value = true;
}

function closePopup() {
  isOpen.value = false;
}

// pouvoir fermer avec echap
function onKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") closePopup();
}

watch(isOpen, (open) => {
  if (open) window.addEventListener("keydown", onKeydown);
  else window.removeEventListener("keydown", onKeydown);
});
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentsModalList:
    title: "Current experiments hosted in {0}"
    no_experiments: "No current experiments"
fr:
  ExperimentsModalList:
    title: "Expériences en cours hébergées dans {0}"
    no_experiments: "Pas d'expériences en cours"
</i18n>
