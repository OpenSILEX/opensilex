<template>
  <div>
    <opensilex-Card class="fav-card" :no-footer="true" icon="fa#star" label="Favorites.title">
      <!-- Favorites Help -->
      <template v-slot:rightHeader>
        <opensilex-HelpButton
            label="component.common.help-button"
            class="helpButton"
            icon="fa#question"
            @click="favoritesHelp.show()"
        ></opensilex-HelpButton>
      </template>
      <!-- Favorites List -->
      <template v-slot:body>

        <ul class="fav-list">
          <li class="fav-entry" v-for="(item, index) in favorites" :key="index">
            <opensilex-Icon
                :icon="getIcon(item.type)"
                class="fav-icon"
            ></opensilex-Icon>
            <opensilex-UriLink
                class="fav-link"
                :allowCopy="false"
                :uri="item.uri"
                :value="item.defaultName"
                :to="getLink(item.type, item.uri)"
            ></opensilex-UriLink>
              <opensilex-Button
                label="Favorites.delete"
                icon="ik#ik-x"
                @click="removeFavorite(item.uri)"
                class="deleteButton"
              ></opensilex-Button>
          </li>
        </ul>

      </template>
    </opensilex-Card>
    <!-- Favorites Help Modal Component -->
    <opensilex-FavoritesHelp
        ref="favoritesHelp"
    ></opensilex-FavoritesHelp>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from 'vue-property-decorator';
import Vue from 'vue';
import {SecurityService} from "opensilex-security/api/security.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import FavoritesHelp from "./FavoritesHelp.vue";
import Oeso from "../../../ontologies/Oeso";
import { FavoriteGetDTO } from 'opensilex-security/index';

export const FAVORITE_TYPES = [
    Oeso.DEVICE_TYPE_URI,
    Oeso.SCIENTIFIC_OBJECT_TYPE_URI,
    Oeso.EXPERIMENT_TYPE_URI
];

@Component
export default class Favorites extends Vue {
  $opensilex: any;

  favorites: Array<FavoriteGetDTO> = [];

  @Ref("favoritesHelp") readonly favoritesHelp!: FavoritesHelp;

  created() {
    this.getFavorites()
  }

  getFavorites() {
    const service: SecurityService = this.$opensilex.getService("opensilex.SecurityService");
    service
        .getFavorites(FAVORITE_TYPES)
        .then((http: HttpResponse<OpenSilexResponse<any[]>>) => {
      this.favorites = http.response.result;
    });
  }

  getIcon(type) {
    switch(type) {
      case Oeso.DEVICE_TYPE_URI: return "ik#ik-thermometer";
      case Oeso.SCIENTIFIC_OBJECT_TYPE_URI: return "ik#ik-target";
      case Oeso.EXPERIMENT_TYPE_URI: return "ik#ik-layers";
      default: return "fa#question-circle"
    }
  }

  getLink(type, uri) {
    let basePath: String;
    switch(type) {
      case Oeso.DEVICE_TYPE_URI: basePath = 'device/details/'; break
      case Oeso.SCIENTIFIC_OBJECT_TYPE_URI: basePath = 'scientific-objects/details/'; break
      case Oeso.EXPERIMENT_TYPE_URI: basePath = 'experiment/details/'; break
      default: return ''
    }
    return { path: basePath + encodeURIComponent(uri) }
  }

  removeFavorite(item) {
    const service: SecurityService = this.$opensilex.getService("opensilex.SecurityService");

    service.deleteFavorite(item).then(()=> {
          this.getFavorites()
    })
  }
}
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

/deep/ .uri { /* target css from opensilex-UriLink */
  padding-left: 5px;
  max-width: 98%;
}
/deep/ .uri span {
  max-width: 100%;
}

/* opensilex colors style */
.helpButton {
  color: #00A28C;
  font-size: 1.2em;
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
