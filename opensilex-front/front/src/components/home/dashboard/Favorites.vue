<template>
  <div>
        <opensilex-Card class="fav-card" :no-footer="true" icon="bi-star-half" :label="t('Favorites.title')">
        
      <template #rightHeader>
        <opensilex-HelpButton
          label="component.common.help-button"
          class="helpButton"
          icon="fa#question"
          @click="favoritesHelpModal?.show()"
          :small="true"
        />
      </template>

      <template #body>
        <ul class="fav-list">
          <li v-for="(item, index) in favorites" :key="index" class="fav-entry">
            <slot name="icon">
                <opensilex-Icon v-if="getIcon(item.type).startsWith('fa#')" :icon="getIcon(item.type)" class="fav-icon" />
                <i v-else :class="['fav-icon', getIcon(item.type)]"></i>
            </slot>
            <opensilex-UriLink
              class="fav-link"
              :allowCopy="false"
              :uri="item.uri"
              :value="item.defaultName"
              :to="getLink(item.type, item.uri)"
            />

            <opensilex-Button
              :label="t('Favorites.delete')"
              icon="bi-x-lg"
              @click="removeFavorite(item.uri)"
              class="deleteButton"
              :small="true"
            />
          </li>
        </ul>
      </template>

    </opensilex-Card>

    <opensilex-FavoritesHelp ref="favoritesHelpModal" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from "vue";
import { SecurityService } from "opensilex-security/api/security.service";
import { OpenSilexResponse } from "opensilex-core/HttpResponse";
import FavoritesHelp from "./FavoritesHelp.vue";
import HelpButton from "@/components/HelpButton.vue";
import Oeso from "../../../ontologies/Oeso";
import { FavoriteGetDTO } from "opensilex-security/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { useI18n } from 'vue-i18n';

const FAVORITE_TYPES = [Oeso.DEVICE_TYPE_URI, Oeso.SCIENTIFIC_OBJECT_TYPE_URI, Oeso.EXPERIMENT_TYPE_URI];

const favorites = ref<FavoriteGetDTO[]>([]);
const favoritesHelpModal = ref<InstanceType<typeof FavoritesHelp> | null>(null);

const $opensilex= inject<OpenSilexVuePlugin>("$opensilex");

const { t } = useI18n();

const getFavorites = async () => {


        const service = $opensilex.getService<SecurityService>(
        "opensilex-security.SecurityService");

  const response = await service.getFavorites(FAVORITE_TYPES);
  favorites.value = response.response.result;
};

const getIcon = (type: string) => {
  switch (type) {
    case Oeso.DEVICE_TYPE_URI:
      return "bi-thermometer-half"; // Équivalent de ik#ik-thermometer
    case Oeso.SCIENTIFIC_OBJECT_TYPE_URI:
      return "bi-bullseye"; // Équivalent de ik#ik-target
    case Oeso.EXPERIMENT_TYPE_URI:
      return "bi-layers"; // Équivalent de ik#ik-layers
    default:
      return "fa#question-circle"; // Garde FontAwesome pour le cas par défaut
  }
};


const getLink = (type: string, uri: string) => {
  let basePath = "";
  switch (type) {
    case Oeso.DEVICE_TYPE_URI:
      basePath = "device/details/";
      break;
    case Oeso.SCIENTIFIC_OBJECT_TYPE_URI:
      basePath = "scientific-objects/details/";
      break;
    case Oeso.EXPERIMENT_TYPE_URI:
      basePath = "experiment/details/";
      break;
  }
  return { path: basePath + encodeURIComponent(uri) };
};

const removeFavorite = async (item: string) => {
  const service: SecurityService = $opensilex.value.getService("opensilex.SecurityService");
  await service.deleteFavorite(item);
  getFavorites();
};

onMounted(() => {
  getFavorites();
});
</script>


<style scoped lang="css"> /* css used here to get the /deep/ selector working */
.fav-card {
  min-height: 29vh;
}
.fav-list {
  padding-left: 0;
  height: 60vh;
  overflow: auto;

}
.fav-entry {
  font-size: 14px;
  list-style-type: none;
  margin: 0;
  padding-bottom: 2px;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  text-overflow: ellipsis;

}
.fav-link {
  width: 78%;
  text-overflow: ellipsis;
  overflow: hidden;
}

.fav-link, .fav-icon {
  margin-top:8px
}

::v-deep(.uri) { /* target css from opensilex-UriLink */
  padding-left: 5px;
  max-width: 98%;
}
::v-deep(.uri span) {
  max-width: 100%;
}

/* opensilex colors style */
.helpButton {
  color: #00A28C;
  /* font-size: 1.2em; */
  border: none
}
  
.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}

.deleteButton{
  border: none;
  padding: 1px 8px;
  margin: 4px;
  font-size: 1.5em;
  color:rgba(101, 101, 101, 0.5);
  font-weight: bolder;
  cursor: pointer;
  background: none;
}

.deleteButton:hover{
  color : #00A28C;
}

@media (max-width: 1400px) {
  .fav-list {
    height: 46vh;
  }
  .fav-link {
    width: 95%;
  }
  .deleteButton{
    font-size: 1.4em;
  }

}
</style>

<i18n>
en:
  Favorites:
    title: Favorites
    delete: Delete favorite

fr:
  Favorites:
    title: Favoris
    delete: Supprimer le favoris
</i18n>
