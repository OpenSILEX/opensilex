import Vue from 'vue'
import Vuex from 'vuex'
import { User } from '@/users/User'

Vue.use(Vuex)

let expireTimeout: any = undefined;

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS()
  },
  mutations: {
    login(state, user: User) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      expireTimeout = setTimeout(() => {
        this.commit("logout");
      }, user.getExpirationMs());
      state.user = user;
    },
    logout(state) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      state.user = User.logout();
    }
  },
  actions: {
  },
  modules: {
  }
})


