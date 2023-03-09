<template>
  <b-button
      @click.prevent="$emit('click')"
 
      variant="outline-warning"
      :pressed.sync="favoriteState"
      @click="favoriteState ? addFavorite() : removeFavorite()"
      :title="toggleTitle()"
  >
    <slot name="icon">
      <opensilex-Icon icon="fa#star" />
    </slot>
  </b-button>
</template>

<script lang="ts">
import {Component, Prop, Watch} from "vue-property-decorator";
import Vue from "vue";
import {SecurityService} from "opensilex-security/api/security.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import { FAVORITE_TYPES } from "../../home/dashboard/Favorites.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class FavoriteButton extends Vue {
  $opensilex: OpenSilexVuePlugin;

  favoriteState = false;
  userURI: string;

  @Prop()
  uri: string;

  created() {
    this.userURI = this.$store.state.user.tokenData.sub;
    this.getfavoriteState();
  }

  @Watch("uri")
  getfavoriteState() {
    const service: SecurityService = this.$opensilex.getService("opensilex.SecurityService");
    service.getFavorites(FAVORITE_TYPES).then((http: HttpResponse<OpenSilexResponse<any[]>>) => {
      this.favoriteState = http.response.result.some(
          fav => fav.uri === this.uri
      )
    });
  }

  addFavorite() {
    const service: SecurityService = this.$opensilex.getService("opensilex.SecurityService");
    const favorite = {
      uri: this.uri
    };
    service
        .addFavorite(favorite)
        .catch(this.$opensilex.errorHandler);
  }

  removeFavorite() {
    const service: SecurityService = this.$opensilex.getService("opensilex.SecurityService");
    service.deleteFavorite(this.uri);
  }
  
  toggleTitle() {
    if (this.favoriteState === false) {
      return this.$t("FavoriteButton.labelAdd")
    }
    else {
      return this.$t("FavoriteButton.labelRemove")
    }
  }
}
</script>

<style scoped lang="scss">
.button-label {
  margin-left: 5px;
  padding-right: 13px;
}
</style>

<i18n>
en:
  FavoriteButton:
    labelAdd: Add to favorites
    labelRemove: Remove from favorites

fr:
  FavoriteButton:
    labelAdd: Ajouter aux favoris
    labelRemove: Retirer des favoris
</i18n>
