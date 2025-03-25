<template>
  <div>
    <div class="header-top" header-theme="light">
      <router-link :to="{ path: '/' }" :title="t('component.menu.backToDashboard')">
        <div class="app-logo">
          <div class="header-brand">
            <div class="logo-img">
              <slot name="headerLogo">
                <img
                  :src="$opensilex.getResourceURI('images/logo-opensilex_miniature.png')"
                  class="header-brand-img"
                  alt="lavalite"
                />
              </slot>
            </div>
            <span class="text">
              {{ applicationName }}
            </span>
          </div>
        </div>
      </router-link>

      <div class="container-fluid boxed-layout">
        <h5 v-if="iconvalue" class="header-title">
          <opensilex-Icon :icon="iconvalue" class="title-icon" />
          <!-- <opensilex-Icon :icon.sync="iconvalue" class="title-icon" /> -->
          <slot name="title">&nbsp;{{ t(titlevalue) }}</slot>
        </h5>
        <span v-else> <br> </span>
        <span class="title-description">
          <slot name="description">{{ descriptionevalue ? t(descriptionevalue) : '' }}</slot>
        </span>

        <div class="d-flex justify-content-end">
          <div class="top-menu d-flex align-items-center">
            <div v-if="versionLabel" class="version-label-box" :class="[versionLabelClass]">
              {{ versionLabel }}
            </div>

            <!-- Burger menu start -->
            <button
              class="hamburger headerburger"
              type="button"
              @click="HeaderBurgerToggle = !HeaderBurgerToggle"
            >
              <span class="hamburger-box">
                <span class="hamburger-inner"></span>
              </span>
            </button>

            <Transition>
              <div v-show="HeaderBurgerToggle" class="burgerMenuContainer"><br>
                <div>
                  <opensilex-HelpButton
                    class="burgerMenuHelp"
                    @click="$opensilex.getGuideFile()"
                    :label="t('component.header.user-guide')"
                  ></opensilex-HelpButton>
                </div>
                <div>
                  <div class="dropdown">
                    <button class="btn btn-link dropdown-toggle" type="button" id="languageDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                      <i class="icon ik ik-globe"></i> {{ `language - ${language}` }}
                    </button>
                    <ul class="dropdown-menu" aria-labelledby="languageDropdown">
                      <li v-for="item in languages" :key="`language-${item}`">
                        <a class="dropdown-item" href="#" @click.prevent="setLanguage(item)">
                          {{ t('component.header.language.' + item) }}
                        </a>
                      </li>
                    </ul>
                  </div>
                </div>
                <div v-if="user.isLoggedIn()">
                  <div class="dropdown">
                    <button class="btn btn-link dropdown-toggle" type="button" id="userDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                      <i class="icon ik ik-user"></i> {{ user.getEmail() }}
                    </button>
                    <ul class="dropdown-menu" aria-labelledby="userDropdown">
                      <li>
                        <a class="dropdown-item" href="#" @click.prevent="logout">
                          <i class="ik ik-log-out dropdown-icon"></i> {{ t('component.header.account.logout') }}
                        </a>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </Transition>
            <!-- Burger menu end -->

            <!-- help button -->
            <opensilex-HelpButton
              class="topbarBtnHelp"
              @click="$opensilex.getGuideFile()"
              :label="t('component.header.user-guide')"
              :small="true"
            ></opensilex-HelpButton>

            <span class="headerMenuIcons">
              <!-- language button -->
              <div class="dropdown btn-group">

                <opensilex-Button 
                  class="btn settingsButton dropdown-toggle"
                  id="languageDropdown2" 
                  data-bs-toggle="dropdown" 
                  aria-expanded="false" 
                  :label="`language - ${language}`"
                  icon="bi-globe" 
                  @click="setLanguage(item)"
                  :small="true" > 
                </opensilex-Button>

                <ul class="dropdown-menu" aria-labelledby="languageDropdown2">
                  <li v-for="item in languages" :key="`language-${item}`">
                    <a class="dropdown-item" href="#" @click.prevent="setLanguage(item)">
                      {{ t('component.header.language.' + item) }}
                    </a>
                  </li>
                </ul>
              </div>

              <!-- dashboard homepage button -->
              <div class="btn-group">
                <router-link :to="{ path: '/' }" :title="t('component.menu.backToDashboard')">
                  <i class="icon bi bi-house settingsButton"></i>
                </router-link>
              </div>

              <!-- user button -->
              <div v-if="user.isLoggedIn()" class="dropdown btn-group">
                <opensilex-Button 
                  class="btn settingsButton dropdown-toggle" 
                  id="userDropdown2" 
                  data-bs-toggle="dropdown" 
                  aria-expanded="false"  
                  :label="user.getEmail()" 
                  icon="bi-person" 
                  :small="true" >
                </opensilex-Button>
                <ul class="dropdown-menu" aria-labelledby="userDropdown2">
                  <li>
                    <a class="dropdown-item" href="#" @click.prevent="logout">
                      <i class="bi bi-box-arrow-right dropdown-icon"></i> {{ t('component.header.account.logout') }}
                    </a>
                  </li>
                </ul>
              </div>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<script lang="ts">
import { defineComponent, ref, onMounted, onBeforeUnmount, computed , inject} from 'vue';
import { useI18n } from 'vue-i18n';
import { User } from "../../models/User";
import { Menu } from "../../models/Menu";
import store from "../../models/Store";
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

export default defineComponent({
    setup() {
      const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
      const { t, locale, availableLocales } = useI18n({
      inheritLocale: true,
      useScope: "local",
    });

        const route = useRoute();

        const HeaderBurgerToggle = ref(false);
        const width = ref<number | undefined>(undefined);

        const description = ref<any>(undefined);
        const title = ref<any>(undefined);
        const icon = ref<any>(undefined);

        const user = computed(() => store.state.user);
       
        const store = useStore();
        const i18n = computed(() => store.state.i18n as any);


    const iconvalue = computed(() => {
      const pathicon = store.state.openSilexRouter.sectionAttributes[route.path];
      console.log("pathicon  :", pathicon)
      return pathicon ? pathicon.icon : '';
    });

    const titlevalue = computed(() => {
      const pathtitle = store.state.openSilexRouter.sectionAttributes[route.path];
      return pathtitle ? pathtitle.title : undefined;
    });

    const descriptionevalue = computed(() => {
      const pathdescription = store.state.openSilexRouter.sectionAttributes[route.path];
      return pathdescription ? pathdescription.description : undefined;
    });


        // const language = computed(() => i18n.value.locale);

        // OU : 
        // const { locale } = useI18n();
        // const language = computed(() => locale.value);

        const language = computed(() => store.state.lang);

          // Gestion des langues
    // const language = ref();


        const { messages } = useI18n();
        const languages = computed(() => Object.keys(messages.value)); 
        // const languages = computed(() => Object.keys(i18n.value.messages));

        const versionLabel = computed(() => {
            const config = opensilex.getConfig();
            if (!config.versionLabel) return undefined;
            return t("component.header.version-label." + config.versionLabel.toLowerCase());
        });

        const versionLabelClass = computed(() => {
            const config = opensilex.getConfig();
            return config.versionLabel ? config.versionLabel.toLowerCase() : undefined;
        });

        const applicationName = computed(() => {
            const config = opensilex.getConfig();
            return config.applicationName || undefined;
        });

        onMounted(() => {
            window.addEventListener('resize', handleResize);
            handleResize();
            console.log("languages : ", languages.value)
        });

        onBeforeUnmount(() => {
            window.removeEventListener('resize', handleResize);
        });

        const handleResize = () => {
            const minSize = 1025;
            if (document.body.clientWidth <= minSize && (width.value == null || width.value > minSize)) {
                width.value = document.body.clientWidth;
                store.commit("hideMenu");
            } else if (document.body.clientWidth > minSize && (width.value == null || width.value <= minSize)) {
                width.value = document.body.clientWidth;
                store.commit("showMenu");
            }
        };


        const logout = () => {
            store.commit("logout");
            store.commit("refresh");
        };

        return {
            HeaderBurgerToggle,
            width,
            description,
            title,
            icon,
            user,
            iconvalue,
            titlevalue,
            descriptionevalue,
            language,
            languages,
            versionLabel,
            versionLabelClass,
            applicationName,
            logout,
            t,
            locale,
            availableLocales,
            opensilex
        };
    },

    methods: {
        setLanguage(lang: string) {
      this.$i18n.locale = lang;
      this.$store.commit("lang", lang);
    },
    }
});
</script>




<style scoped lang="scss">

.header-top {
  height: 65px;
}

.app-logo {
  text-align: left;
  position: absolute;
  width: 203px;
  height:65px;
  top: 0;
  left: 38px;
  background-color: rgb(0, 163, 141);
  padding-top: 6px;
  padding-bottom: 6px;
  padding-left: 35px;
}
.app-logo > a > img {
  height: 50px;
}

.app-title {
  font-family: "Eras Light ITC", Arial, Helvetica, sans-serif;
  margin-left: 10px;
  height: 50px;
  display: inline-block;
}

.header-brand {
  display: flex;
  font-size: 20px;
  font-weight: 700;
}

.header-brand .logo-img {
display: inline-block;
width: 30px;
margin-top: 5px
}
.header-brand .text {
    margin-left: 21px;
    margin-top: 10px;
    color: #fff;
    font-size: 20px;
    font-weight: 700;
}

.title-icon {
  float: left;
  width: 40px;
  height: 40px;
  border-radius: 5px;
  margin-right: 20px;
  vertical-align: middle;
  font-size: 22px;
  color: #fff;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 12px -3px rgba(0, 0, 0, 0.5);
  background-color: #00a38d; 
}

.header-title{
  margin-bottom: 0px;
}
.header-title, .title-description {
  word-wrap: break-word
}
#menu-container {
  top: 60px!important;
}

.version-label-box {
  margin: 0 10px;
  padding: 5px 10px;
  border-radius: 5px;
  font-weight: bold;

  &.develop {
    background-color: #ff7800;
    color: white;
  }

  &.release {
    background-color: #cc338b;
    color: white;
  }
}
.container-fluid {
  margin-left: 240px;
  width: 85%;
}

.top-menu {
  position: absolute;
  float: right;
  top: 10px;
}

.settingsButton {
  font-size: 1.2em;
  font-weight: bold;
  border: none
}

.settingsButton:hover {
color: #00A28C;
}

.btn-group {
  position: relative;
  display: inline-flex;
  display: -webkit-inline-box;
  display: -ms-inline-flexbox;
  vertical-align: middle;
  font-size: 1.2em;
  font-weight: bold;
  // margin: 0 3px
}

.btn-group i {
  font-weight: bold;
}

.topbarBtnHelp {
  height: 36px;
  line-height: 5px;
  border: none;
  width: 29px;
  padding-bottom: 2px;
  margin-bottom: 4px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Burger button*/
.headerburger {
  position: fixed;
  top: 0;
  right:5px;
  z-index: 2000;
}
/* Burger Icon Container*/
.hamburger-box {
  position: absolute;
  top: 3px;
  left: 5px;
}
/* Burger Icon*/
.hamburger-inner, .hamburger-inner:before, .hamburger-inner:after{
  width: 20px;
  height: 3px;
  background-color: #00a38d;
  justify-content: center;
  align-items: center;
  margin-top: 3px;
  margin-bottom: 3px;
}

.burgerMenuContainer {
  position: fixed;
  top: 30px;
  right: 5px;
}
.burgerMenuHelp {
  height: 15px;
  line-height: 5px;
  border: none;
  width: 5px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
}

/* Burger Transition */
.v-enter-active,
.v-leave-active {
  transition: opacity 0.5s ease;
}
.v-enter,
.v-leave-to {
  opacity: 0;
}

.helpButton {
    color: #00A38D;
    border-color: #00A38D;
    background-color: #FFFFFF;
    padding-bottom: 5px
}
.helpButton:hover{
    background-color: #F0F1F5;
    border-color: #00A38D;
    color: #00A38D;
}

.ik-home{
  font-size: 1.3em;
  vertical-align: middle;
  margin: -2px 0 0 12px
}

@media only screen and (min-width: 1380px) {
  .top-menu {
    margin-right:35px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
  .languageIcon, .userIcon{
    margin: -2px 0 0 4px
  }
}
@media (min-width: 1151px) and (max-width: 1379px) {
  .container-fluid {
    width: 80%;
    transition: 1s;
  }
  .top-menu {
    margin-right:20px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
  .languageIcon, .userIcon{
    margin: -2px 0 0 4px
  }
}
@media (min-width: 950px) and (max-width: 1150px) {
  .top-menu{
    margin-right: 110px;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
}
@media (min-width: 800px) and (max-width: 949px) {
  .top-menu{
    margin-right: 120px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
    .title-description{
    width:50%;
    display: block;
    line-height: 1.2;
    overflow: hidden;
    margin-left: -30px;
  }
}
@media (min-width: 676px) and (max-width: 799px) {
  .top-menu{
    margin-right: 150px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
    .title-description{
    width:50%;
    display: block;
    line-height: 1.2;
    overflow: hidden;
    margin-left: -30px;
  }
}
@media (min-width: 250px) and (max-width: 1150px) {
  .topbarBtnHelp { 
    height: 25px;
    width: 25px;
    font-size: 85%;
    line-height: 9px;
  }
  .top-menu {
    margin-top: 10px;
  }
  .header-brand .text {
    font-size: 90%;
    margin-left: 45px;
  }

}
@media (min-width: 950px) and (max-width: 1150px) {
  .top-menu{
    margin-right: 110px;
  }
}
@media (min-width: 800px) and (max-width: 949px) {
  .top-menu{
    margin-right: 120px;
    transition: 1s;
  }
}
@media (min-width: 676px) and (max-width: 799px) {
  .top-menu{
    margin-right: 150px;
    transition: 1s;
  }
}
@media (min-width: 200px) and (max-width: 675px) {
  .app-logo {
    width: 150px;
  }
  .top-menu{
    margin-right: 170px;
    transition: 1s;
  }
  .title-icon{
    display: none;
  }
  .container-fluid {
    margin-left: 240px;
    transition: 1s;
    width: 50%;
  }
  .header-brand .text {
    margin-left: 5px;
  }
  .header-brand-img {
    margin-left: 0px;
  }
  .header-title, .title-description {
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .header-title {
    font-size: 1.1em;
    display: block;
    line-height: 1.2;
    font-weight: 600;
    overflow: hidden;
  }

  .title-description {
    width: 50%;
  }

  .headerMenuIcons, .topbarBtnHelp {
    display: none;
  }
}
</style>
