import Vue from 'vue'
import Vuex, { Store } from 'vuex'
import { User } from '@/users/User'

Vue.use(Vuex)

let expireTimeout: any = undefined;
let loaderCount: number = 0;

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false
  },
  mutations: {
    login(state, user: User) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      expireTimeout = setTimeout(() => {
        let method: any = "logout";
        this.commit(method);
      }, user.getExpirationMs());
      state.user = user;
    },
    logout(state) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      state.user = User.logout();
    },
    showLoader(state) {
      if (loaderCount == 0) {
        state.loaderVisible = true;
      }
      loaderCount++;
    },
    hideLoader(state) {
      loaderCount--;
      if (loaderCount == 0) {
        state.loaderVisible = false
      }
    }
  },
  actions: {
  },
  modules: {
  }
})

