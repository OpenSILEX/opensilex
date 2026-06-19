<template>
  <div>
    <div class="row clearfix" :style="small ? 'padding: 8px 20px;' : ''">
      <div class="d-inline-block w-100 float-left tabsButtons">
        <!-- Zone des boutons -->
        <slot name="buttons"></slot>

        <!-- Barre de navigation avec onglets -->
        <ul v-if="tabs && tabs.length" class="nav nav-tabs">
          <li
            class="nav-item"
            v-for="(tab, index) in tabs"
            :key="index"
            @click="currentTabIndex = index"
          >
            <a
              href="#"
              class="nav-link"
              :class="{ active: currentTabIndex === index }"
              @click.prevent
            >
              {{ tab.label }}
            </a>
          </li>
        </ul>

        <!-- Bouton retour -->
        <router-link
          v-if="returnTo && returnButton"
          :to="returnTo"
          v-slot="{ href, navigate }"
        >
          <a
            class="btn back-button mr-2 h-100"
            :href="href"
            :title="t('pageActions.returnToTitle')"
            @click="navigate; goBack()"
          >
            <opensilex-Icon icon="bi#bi-arrow-90deg-left" class="icon-title back-button-icon" />
          </a>
        </router-link>

        <router-link
          v-else-if="returnButton"
          to="/"
          :title="t('pageActions.returnToTitle')"
          @click.prevent="$router.go(-1)"
        >
          <a class="btn mr-2 h-100 back-button">
            <opensilex-Icon class="icon-title back-button-icon" icon="bi#bi-arrow-90deg-left" />
          </a>
        </router-link>

        <!-- Slot principal -->
        <slot></slot>
      </div>

      <!-- Slot retour derniere page -->
      <div class="card-header-right">
        <slot name="rightHeader"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed }  from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  returnToTitle?: string;
  returnButton?: boolean;
  small?: boolean;
  tabs?: { label: string }[];
  currentTab?: number;
}>();

const emit = defineEmits(['update:currentTab']);

const store = useStore();
const { t } = useI18n();

const returnTo = computed(() => {
  const previousPages = store.state.previousPage;
  return previousPages.length > 0 ? previousPages[previousPages.length - 1] : false;
});

function goBack() {
  //TODO MAX verify nothing changed and replace
  //store.commit('goBack');
}

// gestion du modèle de l’onglet actif
const currentTabIndex = computed({
  get: () => props.currentTab ?? 0,
  set: (value: number) => emit('update:currentTab', value),
});
</script>

<style scoped lang="scss">
.nav {
  margin-left: 14px;
}

.tabsButtons {
  display: flex !important;
}
button {
  margin-left: 1%;
}
li:first-child.nav-item {
  margin-left: 1%;
}
</style>

<i18n>
en:
  pageActions: 
    returnToTitle: Return to the previous page 
            
fr:
  pageActions: 
    returnToTitle: Retourner à la page précédente

</i18n>