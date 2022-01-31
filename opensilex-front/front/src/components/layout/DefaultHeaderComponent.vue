<template>
  <div class="header-top" header-theme="light">
    <div class="app-logo">
      <div class="header-brand" to="/">
        <div class="logo-img">
          <img
            v-bind:src="$opensilex.getResourceURI('images/logo-phis.svg')"
            class="header-brand-img"
            alt="lavalite"
          />
        </div>
        <span class="text">
          PHIS
        </span>
      </div>
    </div>

    <div class="container-fluid boxed-layout">
<!------------------------------------------------------->

<h5 class="header-title">
  <opensilex-Icon :icon.sync="iconvalue" class="title-icon"/>
  <!-- <slot name="icon"> {{ $t(iconvalue) }}</slot> -->
  <slot name="title">&nbsp;{{ $t(titlevalue) }}</slot>
</h5>
<span class="title-description"><slot name="description" >{{ $t(descriptionevalue) }}</slot></span>

<!------------------------------------------------------->
      <div class="d-flex justify-content-end">
        <div class="top-menu d-flex align-items-center">

          <b-dropdown
            id="AddDropdown"
            class="top-menu-add-btn"
            :title="user.getAddMessage()"
            variant="link"
            right
          >
            <template v-slot:button-content>
              <i class="icon ik ik-plus"></i>
            </template>

            <b-dropdown-item href="#">
              <i class="ik ik-share dropdown-icon"></i>
              {{ $t("component.header.user.addObject") }}
            </b-dropdown-item>
            <b-dropdown-item href="#" @click.prevent="logout">
              <i class="ik ik-download dropdown-icon"></i>
              {{ $t("component.header.user.csvImport") }}
            </b-dropdown-item>
          </b-dropdown>
<!------------------------------------------------------->
          <opensilex-HelpButton
            class="topbarBtnHelp"
            @click="$opensilex.getGuideFile()"
            label="component.header.user-guide"
          ></opensilex-HelpButton>
<!------------------------------------------------------->
          <b-dropdown
            id="langDropdown" 
            :title="`language - ${this.language}`"
            variant="link"
            right
          >
            <template v-slot:button-content>
              <i class="icon ik ik-globe"></i>
              <!-- <span class="hidden-phone">{{ $t("component.header.language." + language) }}</span>
              <span class="show-phone">{{ $t("component.header.language." + language).substring(0,2) }}</span>
              <i class="ik ik-chevron-down"></i> -->
            </template>

            <b-dropdown-item
              v-for="item in languages"
              :key="`language-${item}`"
              href="#"
              @click.prevent="setLanguage(item)"
              >{{ $t("component.header.language." + item) }}
            </b-dropdown-item>
          </b-dropdown>
<!------------------------------------------------------->
          <b-dropdown
            v-if="user.isLoggedIn()"
            id="userDropdown"
            :title="user.getEmail()"
            variant="link"
            right
          >
            <template v-slot:button-content class="userIcon">
              <i class="icon ik ik-user"></i>
              <!-- <span class="hidden-phone">
              {{ user.getFirstName() }} {{ user.getLastName() }}
              <strong v-if="user.isAdmin()"
                >({{ $t("component.header.user.admin") }})</strong
              >
              </span>
              <i class="ik ik-chevron-down"></i> -->
            </template>
            <b-dropdown-item href="#" @click.prevent="logout">
              <i class="ik ik-log-out dropdown-icon"></i>
              {{ $t("component.header.user.logout") }}
            </b-dropdown-item>
          </b-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Watch} from "vue-property-decorator";
import Vue from "vue";
import { User } from "../../models/User";
import { Menu } from "../../models/Menu";


@Component
export default class DefaultHeaderComponent extends Vue {
  $i18n: any;
  $store: any;
  $opensilex: any;
  $route: any;
  $t: any;

  icon: any;

  title: any;

  description: any;

  // @Prop({default: false})
  // isExperimentalFeature: boolean;


//   @Watch('$route', { immediate: true, deep: true })
// onUrlChange(newVal: any) {
  //   this.icon = this.$store.state.openSilexRouter.sectionAttributes[newVal.path].icon;
//   this.$forceUpdate();
//   console.log(this.icon)
//     console.log(newVal);
//     // console.log(this.$store.state.openSilexRouter);
// }

  /**
   * Return the current connected user
   */
  get user() {
    return this.$store.state.user;
  }

  /**
   * Return the section path icon
   */
  get iconvalue() {
    let pathicon = this.$store.state.openSilexRouter.sectionAttributes[this.$route.path];
    if (!pathicon) {
      
      return ""
    }
    else {
      return pathicon.icon;
    }
  }

  /**
   * Return the section path title
   */
    get titlevalue() {
    let pathtitle = this.$store.state.openSilexRouter.sectionAttributes[this.$route.path];
    if (!pathtitle) {
      return undefined
    }
    else {
      return pathtitle.title;
    }
  }

  /**
   * Return the section path description
   */
      get descriptionevalue() {
    let pathdescription = this.$store.state.openSilexRouter.sectionAttributes[this.$route.path];
    if (!pathdescription) {
      return undefined
    }
    else {
      return pathdescription.description;
    }
  }

  /**
   * Return the current i18n language
   */
  get language() {
    return this.$i18n.locale;
  }

  /**
   * Return all available languages
   */
  get languages() {
    return Object.keys(this.$i18n.messages);
  }

  /**
   * Set the current i18n language
   */
  setLanguage(lang: string) {
    this.$i18n.locale = lang;
    this.$store.commit("lang", lang);
  }

  /**
   * Logout the current connected user -> have to redirected to the login page
   */
  logout() {
    this.$store.commit("logout");
  }

  width;
  created() {
    window.addEventListener("resize", this.handleResize);
    this.handleResize();
    console.log("la route :");
    console.log(this.$store.state.openSilexRouter.sectionAttributes);
    console.log(this.$router.currentRoute.fullPath)
  }

  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
  }

  handleResize() {
    const minSize = 768;
    if (
      document.body.clientWidth <= minSize &&
      (this.width == null || this.width > minSize)
    ) {
      this.width = document.body.clientWidth;
      this.$store.commit("hideMenu");
    } else if (
      document.body.clientWidth > minSize &&
      (this.width == null || this.width <= minSize)
    ) {
      this.width = document.body.clientWidth;
      this.$store.commit("showMenu");
    }
  }
}

</script>

<style scoped lang="scss">

.app-logo {
  text-align: left;
  position: absolute;
  width: 180px;
  height:60px;
  top: 0;
  left: 60px;
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

.header-brand .text {
    margin-left: 16px;
    color: #fff;
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

#menu-container {
  top: 60px!important;
}

.container-fluid {
  margin-left: 260px;
  width: 85%;
}

.top-menu {
  position: absolute;
  float: right;
  // margin-right: 36px;
  // margin-right: 20px;
  top: 10px;
}

.top-menu-add-btn {
  margin-right: 10px;
}

@media (min-width: 1380px) and (max-width: 1650px) {
  .top-menu {
    margin-right:35px;
    transition: 1s;
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

}
@media (min-width: 250px) and (max-width: 1150px) {

  .topbarBtnHelp { 
    height: 25px;
    width: 25px;
    font-size: 85%;
    line-height: 9px;
    padding: 5px;
  }
    .header-brand .text {
    font-size: 90%;
    margin-left: 45px;
  }
  .header-brand-img {
    width: 35px;
    height: 35px;
    transition: 1s;
    margin-top: 8px;
    margin-left: 40px;
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
    margin-right: 150px;
    transition: 1s;
  }
  .title-icon{
    display: none;
  }
  .container-fluid {
  margin-left: 240px;
  transition: 1s;
}
    .header-brand .text {
    margin-left: 5px;
  }
  .header-brand-img {
    margin-left: 0px;
  }

}
</style>
