var ae = typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : {}, ue = {};
/*! *****************************************************************************
Copyright (C) Microsoft. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache Version 2.0 License for specific language governing permissions
and limitations under the License.
***************************************************************************** */
var ce;
function ze() {
  if (ce) return ue;
  ce = 1;
  var e;
  return function(r) {
    (function(t) {
      var o = typeof ae == "object" ? ae : typeof self == "object" ? self : typeof this == "object" ? this : Function("return this;")(), u = a(r);
      typeof o.Reflect > "u" ? o.Reflect = r : u = a(o.Reflect, u), t(u);
      function a(f, g) {
        return function(d, w) {
          typeof f[d] != "function" && Object.defineProperty(f, d, { configurable: !0, writable: !0, value: w }), g && g(d, w);
        };
      }
    })(function(t) {
      var o = Object.prototype.hasOwnProperty, u = typeof Symbol == "function", a = u && typeof Symbol.toPrimitive < "u" ? Symbol.toPrimitive : "@@toPrimitive", f = u && typeof Symbol.iterator < "u" ? Symbol.iterator : "@@iterator", g = typeof Object.create == "function", d = { __proto__: [] } instanceof Array, w = !g && !d, R = {
        // create an object in dictionary mode (a.k.a. "slow" mode in v8)
        create: g ? function() {
          return L(/* @__PURE__ */ Object.create(null));
        } : d ? function() {
          return L({ __proto__: null });
        } : function() {
          return L({});
        },
        has: w ? function(n, s) {
          return o.call(n, s);
        } : function(n, s) {
          return s in n;
        },
        get: w ? function(n, s) {
          return o.call(n, s) ? n[s] : void 0;
        } : function(n, s) {
          return n[s];
        }
      }, E = Object.getPrototypeOf(Function), G = typeof process == "object" && process.env && process.env.REFLECT_METADATA_USE_MAP_POLYFILL === "true", F = !G && typeof Map == "function" && typeof Map.prototype.entries == "function" ? Map : Je(), Pe = !G && typeof Set == "function" && typeof Set.prototype.entries == "function" ? Set : Ve(), _e = !G && typeof WeakMap == "function" ? WeakMap : We(), k = new _e();
      function Ce(n, s, i, c) {
        if (m(i)) {
          if (!ne(n))
            throw new TypeError();
          if (!oe(s))
            throw new TypeError();
          return $e(n, s);
        } else {
          if (!ne(n))
            throw new TypeError();
          if (!C(s))
            throw new TypeError();
          if (!C(c) && !m(c) && !$(c))
            throw new TypeError();
          return $(c) && (c = void 0), i = U(i), Me(n, s, i, c);
        }
      }
      t("decorate", Ce);
      function Ae(n, s) {
        function i(c, h) {
          if (!C(c))
            throw new TypeError();
          if (!m(h) && !Fe(h))
            throw new TypeError();
          K(n, s, c, h);
        }
        return i;
      }
      t("metadata", Ae);
      function Re(n, s, i, c) {
        if (!C(i))
          throw new TypeError();
        return m(c) || (c = U(c)), K(n, s, i, c);
      }
      t("defineMetadata", Re);
      function Ie(n, s, i) {
        if (!C(s))
          throw new TypeError();
        return m(i) || (i = U(i)), X(n, s, i);
      }
      t("hasMetadata", Ie);
      function Se(n, s, i) {
        if (!C(s))
          throw new TypeError();
        return m(i) || (i = U(i)), q(n, s, i);
      }
      t("hasOwnMetadata", Se);
      function Ue(n, s, i) {
        if (!C(s))
          throw new TypeError();
        return m(i) || (i = U(i)), Z(n, s, i);
      }
      t("getMetadata", Ue);
      function Ee(n, s, i) {
        if (!C(s))
          throw new TypeError();
        return m(i) || (i = U(i)), Q(n, s, i);
      }
      t("getOwnMetadata", Ee);
      function Te(n, s) {
        if (!C(n))
          throw new TypeError();
        return m(s) || (s = U(s)), ee(n, s);
      }
      t("getMetadataKeys", Te);
      function je(n, s) {
        if (!C(n))
          throw new TypeError();
        return m(s) || (s = U(s)), te(n, s);
      }
      t("getOwnMetadataKeys", je);
      function Oe(n, s, i) {
        if (!C(s))
          throw new TypeError();
        m(i) || (i = U(i));
        var c = M(
          s,
          i,
          /*Create*/
          !1
        );
        if (m(c) || !c.delete(n))
          return !1;
        if (c.size > 0)
          return !0;
        var h = k.get(s);
        return h.delete(i), h.size > 0 || k.delete(s), !0;
      }
      t("deleteMetadata", Oe);
      function $e(n, s) {
        for (var i = n.length - 1; i >= 0; --i) {
          var c = n[i], h = c(s);
          if (!m(h) && !$(h)) {
            if (!oe(h))
              throw new TypeError();
            s = h;
          }
        }
        return s;
      }
      function Me(n, s, i, c) {
        for (var h = n.length - 1; h >= 0; --h) {
          var _ = n[h], l = _(s, i, c);
          if (!m(l) && !$(l)) {
            if (!C(l))
              throw new TypeError();
            c = l;
          }
        }
        return c;
      }
      function M(n, s, i) {
        var c = k.get(n);
        if (m(c)) {
          if (!i)
            return;
          c = new F(), k.set(n, c);
        }
        var h = c.get(s);
        if (m(h)) {
          if (!i)
            return;
          h = new F(), c.set(s, h);
        }
        return h;
      }
      function X(n, s, i) {
        var c = q(n, s, i);
        if (c)
          return !0;
        var h = N(s);
        return $(h) ? !1 : X(n, h, i);
      }
      function q(n, s, i) {
        var c = M(
          s,
          i,
          /*Create*/
          !1
        );
        return m(c) ? !1 : xe(c.has(n));
      }
      function Z(n, s, i) {
        var c = q(n, s, i);
        if (c)
          return Q(n, s, i);
        var h = N(s);
        if (!$(h))
          return Z(n, h, i);
      }
      function Q(n, s, i) {
        var c = M(
          s,
          i,
          /*Create*/
          !1
        );
        if (!m(c))
          return c.get(n);
      }
      function K(n, s, i, c) {
        var h = M(
          i,
          c,
          /*Create*/
          !0
        );
        h.set(n, s);
      }
      function ee(n, s) {
        var i = te(n, s), c = N(n);
        if (c === null)
          return i;
        var h = ee(c, s);
        if (h.length <= 0)
          return i;
        if (i.length <= 0)
          return h;
        for (var _ = new Pe(), l = [], y = 0, p = i; y < p.length; y++) {
          var b = p[y], v = _.has(b);
          v || (_.add(b), l.push(b));
        }
        for (var T = 0, ie = h; T < ie.length; T++) {
          var b = ie[T], v = _.has(b);
          v || (_.add(b), l.push(b));
        }
        return l;
      }
      function te(n, s) {
        var i = [], c = M(
          n,
          s,
          /*Create*/
          !1
        );
        if (m(c))
          return i;
        for (var h = c.keys(), _ = qe(h), l = 0; ; ) {
          var y = Le(_);
          if (!y)
            return i.length = l, i;
          var p = Ne(y);
          try {
            i[l] = p;
          } catch (b) {
            try {
              He(_);
            } finally {
              throw b;
            }
          }
          l++;
        }
      }
      function re(n) {
        if (n === null)
          return 1;
        switch (typeof n) {
          case "undefined":
            return 0;
          case "boolean":
            return 2;
          case "string":
            return 3;
          case "symbol":
            return 4;
          case "number":
            return 5;
          case "object":
            return n === null ? 1 : 6;
          default:
            return 6;
        }
      }
      function m(n) {
        return n === void 0;
      }
      function $(n) {
        return n === null;
      }
      function Be(n) {
        return typeof n == "symbol";
      }
      function C(n) {
        return typeof n == "object" ? n !== null : typeof n == "function";
      }
      function ke(n, s) {
        switch (re(n)) {
          case 0:
            return n;
          case 1:
            return n;
          case 2:
            return n;
          case 3:
            return n;
          case 4:
            return n;
          case 5:
            return n;
        }
        var i = "string", c = se(n, a);
        if (c !== void 0) {
          var h = c.call(n, i);
          if (C(h))
            throw new TypeError();
          return h;
        }
        return De(n);
      }
      function De(n, s) {
        var i, c, h;
        {
          var _ = n.toString;
          if (D(_)) {
            var c = _.call(n);
            if (!C(c))
              return c;
          }
          var i = n.valueOf;
          if (D(i)) {
            var c = i.call(n);
            if (!C(c))
              return c;
          }
        }
        throw new TypeError();
      }
      function xe(n) {
        return !!n;
      }
      function Ge(n) {
        return "" + n;
      }
      function U(n) {
        var s = ke(n);
        return Be(s) ? s : Ge(s);
      }
      function ne(n) {
        return Array.isArray ? Array.isArray(n) : n instanceof Object ? n instanceof Array : Object.prototype.toString.call(n) === "[object Array]";
      }
      function D(n) {
        return typeof n == "function";
      }
      function oe(n) {
        return typeof n == "function";
      }
      function Fe(n) {
        switch (re(n)) {
          case 3:
            return !0;
          case 4:
            return !0;
          default:
            return !1;
        }
      }
      function se(n, s) {
        var i = n[s];
        if (i != null) {
          if (!D(i))
            throw new TypeError();
          return i;
        }
      }
      function qe(n) {
        var s = se(n, f);
        if (!D(s))
          throw new TypeError();
        var i = s.call(n);
        if (!C(i))
          throw new TypeError();
        return i;
      }
      function Ne(n) {
        return n.value;
      }
      function Le(n) {
        var s = n.next();
        return s.done ? !1 : s;
      }
      function He(n) {
        var s = n.return;
        s && s.call(n);
      }
      function N(n) {
        var s = Object.getPrototypeOf(n);
        if (typeof n != "function" || n === E || s !== E)
          return s;
        var i = n.prototype, c = i && Object.getPrototypeOf(i);
        if (c == null || c === Object.prototype)
          return s;
        var h = c.constructor;
        return typeof h != "function" || h === n ? s : h;
      }
      function Je() {
        var n = {}, s = [], i = (
          /** @class */
          function() {
            function l(y, p, b) {
              this._index = 0, this._keys = y, this._values = p, this._selector = b;
            }
            return l.prototype["@@iterator"] = function() {
              return this;
            }, l.prototype[f] = function() {
              return this;
            }, l.prototype.next = function() {
              var y = this._index;
              if (y >= 0 && y < this._keys.length) {
                var p = this._selector(this._keys[y], this._values[y]);
                return y + 1 >= this._keys.length ? (this._index = -1, this._keys = s, this._values = s) : this._index++, { value: p, done: !1 };
              }
              return { value: void 0, done: !0 };
            }, l.prototype.throw = function(y) {
              throw this._index >= 0 && (this._index = -1, this._keys = s, this._values = s), y;
            }, l.prototype.return = function(y) {
              return this._index >= 0 && (this._index = -1, this._keys = s, this._values = s), { value: y, done: !0 };
            }, l;
          }()
        );
        return (
          /** @class */
          function() {
            function l() {
              this._keys = [], this._values = [], this._cacheKey = n, this._cacheIndex = -2;
            }
            return Object.defineProperty(l.prototype, "size", {
              get: function() {
                return this._keys.length;
              },
              enumerable: !0,
              configurable: !0
            }), l.prototype.has = function(y) {
              return this._find(
                y,
                /*insert*/
                !1
              ) >= 0;
            }, l.prototype.get = function(y) {
              var p = this._find(
                y,
                /*insert*/
                !1
              );
              return p >= 0 ? this._values[p] : void 0;
            }, l.prototype.set = function(y, p) {
              var b = this._find(
                y,
                /*insert*/
                !0
              );
              return this._values[b] = p, this;
            }, l.prototype.delete = function(y) {
              var p = this._find(
                y,
                /*insert*/
                !1
              );
              if (p >= 0) {
                for (var b = this._keys.length, v = p + 1; v < b; v++)
                  this._keys[v - 1] = this._keys[v], this._values[v - 1] = this._values[v];
                return this._keys.length--, this._values.length--, y === this._cacheKey && (this._cacheKey = n, this._cacheIndex = -2), !0;
              }
              return !1;
            }, l.prototype.clear = function() {
              this._keys.length = 0, this._values.length = 0, this._cacheKey = n, this._cacheIndex = -2;
            }, l.prototype.keys = function() {
              return new i(this._keys, this._values, c);
            }, l.prototype.values = function() {
              return new i(this._keys, this._values, h);
            }, l.prototype.entries = function() {
              return new i(this._keys, this._values, _);
            }, l.prototype["@@iterator"] = function() {
              return this.entries();
            }, l.prototype[f] = function() {
              return this.entries();
            }, l.prototype._find = function(y, p) {
              return this._cacheKey !== y && (this._cacheIndex = this._keys.indexOf(this._cacheKey = y)), this._cacheIndex < 0 && p && (this._cacheIndex = this._keys.length, this._keys.push(y), this._values.push(void 0)), this._cacheIndex;
            }, l;
          }()
        );
        function c(l, y) {
          return l;
        }
        function h(l, y) {
          return y;
        }
        function _(l, y) {
          return [l, y];
        }
      }
      function Ve() {
        return (
          /** @class */
          function() {
            function n() {
              this._map = new F();
            }
            return Object.defineProperty(n.prototype, "size", {
              get: function() {
                return this._map.size;
              },
              enumerable: !0,
              configurable: !0
            }), n.prototype.has = function(s) {
              return this._map.has(s);
            }, n.prototype.add = function(s) {
              return this._map.set(s, s), this;
            }, n.prototype.delete = function(s) {
              return this._map.delete(s);
            }, n.prototype.clear = function() {
              this._map.clear();
            }, n.prototype.keys = function() {
              return this._map.keys();
            }, n.prototype.values = function() {
              return this._map.values();
            }, n.prototype.entries = function() {
              return this._map.entries();
            }, n.prototype["@@iterator"] = function() {
              return this.keys();
            }, n.prototype[f] = function() {
              return this.keys();
            }, n;
          }()
        );
      }
      function We() {
        var n = 16, s = R.create(), i = c();
        return (
          /** @class */
          function() {
            function p() {
              this._key = c();
            }
            return p.prototype.has = function(b) {
              var v = h(
                b,
                /*create*/
                !1
              );
              return v !== void 0 ? R.has(v, this._key) : !1;
            }, p.prototype.get = function(b) {
              var v = h(
                b,
                /*create*/
                !1
              );
              return v !== void 0 ? R.get(v, this._key) : void 0;
            }, p.prototype.set = function(b, v) {
              var T = h(
                b,
                /*create*/
                !0
              );
              return T[this._key] = v, this;
            }, p.prototype.delete = function(b) {
              var v = h(
                b,
                /*create*/
                !1
              );
              return v !== void 0 ? delete v[this._key] : !1;
            }, p.prototype.clear = function() {
              this._key = c();
            }, p;
          }()
        );
        function c() {
          var p;
          do
            p = "@@WeakMap@@" + y();
          while (R.has(s, p));
          return s[p] = !0, p;
        }
        function h(p, b) {
          if (!o.call(p, i)) {
            if (!b)
              return;
            Object.defineProperty(p, i, { value: R.create() });
          }
          return p[i];
        }
        function _(p, b) {
          for (var v = 0; v < b; ++v)
            p[v] = Math.random() * 255 | 0;
          return p;
        }
        function l(p) {
          return typeof Uint8Array == "function" ? typeof crypto < "u" ? crypto.getRandomValues(new Uint8Array(p)) : typeof msCrypto < "u" ? msCrypto.getRandomValues(new Uint8Array(p)) : _(new Uint8Array(p), p) : _(new Array(p), p);
        }
        function y() {
          var p = l(n);
          p[6] = p[6] & 79 | 64, p[8] = p[8] & 191 | 128;
          for (var b = "", v = 0; v < n; ++v) {
            var T = p[v];
            (v === 4 || v === 6 || v === 8) && (b += "-"), T < 16 && (b += "0"), b += T.toString(16).toLowerCase();
          }
          return b;
        }
      }
      function L(n) {
        return n.__ = void 0, delete n.__, n;
      }
    });
  }(e || (e = {})), ue;
}
ze();
var Ye = "named", Xe = "inject", Ze = "inversify:tagged", Qe = "inversify:tagged_props", fe = "inversify:paramtypes", Ke = "design:paramtypes", et = "Cannot apply @injectable decorator multiple times.", tt = "Metadata key was used more than once in a parameter:", rt = function(e) {
  return "@inject called with undefined this could mean that the class " + e + " has a circular dependency problem. You can use a LazyServiceIdentifer to  overcome this limitation.";
}, nt = "The @inject @multiInject @tagged and @named decorators must be applied to the parameters of a class constructor or a class property.", ot = function() {
  function e(r, t) {
    this.key = r, this.value = t;
  }
  return e.prototype.toString = function() {
    return this.key === Ye ? "named: " + this.value.toString() + " " : "tagged: { key:" + this.key.toString() + ", value: " + this.value + " }";
  }, e;
}();
function st(e, r, t, o) {
  var u = Ze;
  ve(u, e, r, o, t);
}
function it(e, r, t) {
  var o = Qe;
  ve(o, e.constructor, r, t);
}
function ve(e, r, t, o, u) {
  var a = {}, f = typeof u == "number", g = u !== void 0 && f ? u.toString() : t;
  if (f && t !== void 0)
    throw new Error(nt);
  Reflect.hasOwnMetadata(e, r) && (a = Reflect.getMetadata(e, r));
  var d = a[g];
  if (!Array.isArray(d))
    d = [];
  else
    for (var w = 0, R = d; w < R.length; w++) {
      var E = R[w];
      if (E.key === o.key)
        throw new Error(tt + " " + E.key.toString());
    }
  d.push(o), a[g] = d, Reflect.defineMetadata(e, a, r);
}
function x(e) {
  return function(r, t, o) {
    if (e === void 0)
      throw new Error(rt(r.name));
    var u = new ot(Xe, e);
    typeof o == "number" ? st(r, t, o, u) : it(r, t, u);
  };
}
function W() {
  return function(e) {
    if (Reflect.hasOwnMetadata(fe, e))
      throw new Error(et);
    var r = Reflect.getMetadata(Ke, e) || [];
    return Reflect.defineMetadata(fe, r, e), e;
  };
}
var at = Object.getOwnPropertyDescriptor, ut = (e, r, t, o) => {
  for (var u = o > 1 ? void 0 : o ? at(r, t) : r, a = e.length - 1, f; a >= 0; a--)
    (f = e[a]) && (u = f(u) || u);
  return u;
}, pe = (e, r) => (t, o) => r(t, o, e);
let J = class {
  constructor(e, r) {
    this.httpClient = e, this.APIConfiguration = r, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Authenticate a user and return an access token
   * 
   * @param body User authentication informations
   
   */
  authenticatePath() {
    return "/security/authenticate";
  }
  authenticate(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/authenticate`, e, t);
  }
  /**
   * Authenticate a user and return an access token
   * 
   * @param code Authorization code
   
   */
  authenticateOpenIDPath() {
    return "/security/openid";
  }
  authenticateOpenID(e, r = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("code=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/openid${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Authenticate a user and return an access token from SAML response
   * 
   
   */
  authenticateSAMLPath() {
    return "/security/saml";
  }
  authenticateSAML(e = "body", r = {}) {
    return r.Accept = "text/plain", this.httpClient.get(`${this.basePath}/security/saml`, r);
  }
  /**
   * Send an e-mail confirmation
   * 
   * @param identifier User e-mail or uri
   
   */
  forgotPasswordPath() {
    return "/security/forgot-password";
  }
  forgotPassword(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter identifier was null or undefined when calling forgotPassword.");
    return e !== void 0 && o.push("identifier=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.post(`${this.basePath}/security/forgot-password${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get list of existing credentials indexed by Swagger @API concepts in the application
   * 
   
   */
  getCredentialsGroupsPath() {
    return "/security/credentials";
  }
  getCredentialsGroups(e = "body", r = {}) {
    return r.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/credentials`, r);
  }
  /**
   * Logout by discarding a user token
   * 
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  logoutPath() {
    return "/security/logout";
  }
  logout(e = "body", r = {}) {
    return r.Accept = "application/json", this.httpClient.delete(`${this.basePath}/security/logout`, r);
  }
  /**
   * Update user password
   * 
   * @param renew_token User renew token
   * @param check_only Check only renew token
   * @param password User password
   
   */
  renewPasswordPath() {
    return "/security/renew-password";
  }
  renewPassword(e, r, t, o = "body", u = {}) {
    let a = [];
    if (!e)
      throw new Error("Required parameter renew_token was null or undefined when calling renewPassword.");
    return e !== void 0 && a.push("renew_token=" + encodeURIComponent(String(e))), r !== void 0 && a.push("check_only=" + encodeURIComponent(String(r))), t !== void 0 && a.push("password=" + encodeURIComponent(String(t))), u.Accept = "application/json", this.httpClient.put(`${this.basePath}/security/renew-password${a.length > 0 ? "?" + a.join("&") : ""}`, u);
  }
  /**
   * Send back a new token if the provided one is still valid
   * 
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  renewTokenPath() {
    return "/security/renew-token";
  }
  renewToken(e = "body", r = {}) {
    return r.Accept = "application/json", this.httpClient.put(`${this.basePath}/security/renew-token`, r);
  }
};
J = ut([
  W(),
  pe(0, x("IApiHttpClient")),
  pe(1, x("IAPIConfiguration"))
], J);
var ct = Object.getOwnPropertyDescriptor, ft = (e, r, t, o) => {
  for (var u = o > 1 ? void 0 : o ? ct(r, t) : r, a = e.length - 1, f; a >= 0; a--)
    (f = e[a]) && (u = f(u) || u);
  return u;
}, he = (e, r) => (t, o) => r(t, o, e);
let V = class {
  constructor(e, r) {
    this.httpClient = e, this.APIConfiguration = r, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add a favorite
   * 
   * @param Authorization Authentication token
   * @param body Favorite object URI
   * @param Accept_Language Request accepted language
   
   */
  addFavoritePath() {
    return "/security/accounts/favorites";
  }
  addFavorite(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/accounts/favorites`, e, t);
  }
  /**
   * Add a favorite
   * 
   * @param Authorization Authentication token
   * @param body Favorite object URI
   * @param Accept_Language Request accepted language
   
   */
  addFavorite1Path() {
    return "/security/users/favorites";
  }
  addFavorite1(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/users/favorites`, e, t);
  }
  /**
   * Add an account
   * 
   * @param Authorization Authentication token
   * @param body Account description
   * @param Accept_Language Request accepted language
   
   */
  createAccountPath() {
    return "/security/accounts";
  }
  createAccount(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/accounts`, e, t);
  }
  /**
   * Add a group
   * 
   * @param Authorization Authentication token
   * @param body Group description
   * @param Accept_Language Request accepted language
   
   */
  createGroupPath() {
    return "/security/groups";
  }
  createGroup(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/groups`, e, t);
  }
  /**
   * Add a person
   * 
   * @param Authorization Authentication token
   * @param body Person description
   * @param Accept_Language Request accepted language
   
   */
  createPersonPath() {
    return "/security/persons";
  }
  createPerson(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/persons`, e, t);
  }
  /**
   * Add a profile
   * 
   * @param Authorization Authentication token
   * @param body Profile description
   * @param Accept_Language Request accepted language
   
   */
  createProfilePath() {
    return "/security/profiles";
  }
  createProfile(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/profiles`, e, t);
  }
  /**
   * Add a user
   * 
   * @param Authorization Authentication token
   * @param body User description
   * @param Accept_Language Request accepted language
   
   */
  createUserPath() {
    return "/security/users";
  }
  createUser(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/security/users`, e, t);
  }
  /**
   * Delete an account
   * 
   * @param accountURI Account URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteAccountPath() {
    return "/security/accounts/${encodeURIComponent(String(accountURI))}";
  }
  deleteAccount(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/security/accounts/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a favorite
   * 
   * @param uriFavorite Favorite URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteFavoritePath() {
    return "/security/accounts/favorites/${encodeURIComponent(String(uriFavorite))}";
  }
  deleteFavorite(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/security/accounts/favorites/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a favorite
   * 
   * @param uriFavorite Favorite URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteFavorite1Path() {
    return "/security/users/favorites/${encodeURIComponent(String(uriFavorite))}";
  }
  deleteFavorite1(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/security/users/favorites/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a group
   * 
   * @param uri Group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteGroupPath() {
    return "/security/groups/${encodeURIComponent(String(uri))}";
  }
  deleteGroup(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/security/groups/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a profile
   * 
   * @param uri Profile URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteProfilePath() {
    return "/security/profiles/${encodeURIComponent(String(uri))}";
  }
  deleteProfile(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/security/profiles/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get an account
   * 
   * @param uri Account URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAccountPath() {
    return "/security/accounts/${encodeURIComponent(String(uri))}";
  }
  getAccount(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/accounts/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get accounts by their URIs
   * 
   * @param uris Accounts URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAccountsByURIPath() {
    return "/security/accounts/by_uris";
  }
  getAccountsByURI(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getAccountsByURI.");
    return e && e.forEach((a) => {
      o.push("uris=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/accounts/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get all profiles
   * 
   * @param Authorization Authentication token
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param Accept_Language Request accepted language
   
   */
  getAllProfilesPath() {
    return "/security/profiles/all";
  }
  getAllProfiles(e, r = "body", t = {}) {
    let o = [];
    return e && e.forEach((a) => {
      o.push("order_by=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/profiles/all${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get list of favorites for a user
   * 
   * @param types Types
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFavoritesPath() {
    return "/security/accounts/favorites";
  }
  getFavorites(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter types was null or undefined when calling getFavorites.");
    return e && e.forEach((a) => {
      o.push("types=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/accounts/favorites${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get list of favorites for a user
   * 
   * @param types Types
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFavorites1Path() {
    return "/security/users/favorites";
  }
  getFavorites1(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter types was null or undefined when calling getFavorites1.");
    return e && e.forEach((a) => {
      o.push("types=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/users/favorites${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get RGPD PDF
   * 
   * @param language preferred language of the file
   
   */
  getGdprFilePath() {
    return "/security/persons/GDPR";
  }
  getGdprFile(e, r = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("language=" + encodeURIComponent(String(e))), t.Accept = "application/pdf", this.httpClient.get(`${this.basePath}/security/persons/GDPR${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get a group
   * 
   * @param uri Group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGroupPath() {
    return "/security/groups/${encodeURIComponent(String(uri))}";
  }
  getGroup(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/groups/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get groups by their URIs
   * 
   * @param uris Groups URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGroupsByURIPath() {
    return "/security/groups/by_uris";
  }
  getGroupsByURI(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getGroupsByURI.");
    return e && e.forEach((a) => {
      o.push("uris=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/groups/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get infos from an ORCID
   * 
   * @param orcid orcid
   
   */
  getOrcidRecordPath() {
    return "/security/persons/orcid_record";
  }
  getOrcidRecord(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter orcid was null or undefined when calling getOrcidRecord.");
    return e !== void 0 && o.push("orcid=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/persons/orcid_record${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get a Person
   * 
   * @param uri Person URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getPersonPath() {
    return "/security/persons/${encodeURIComponent(String(uri))}";
  }
  getPerson(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/persons/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get persons by their URIs
   * 
   * @param uris Persons URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getPersonsByURIPath() {
    return "/security/persons/by_uris";
  }
  getPersonsByURI(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getPersonsByURI.");
    return e && e.forEach((a) => {
      o.push("uris=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/persons/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get a profile
   * 
   * @param uri Profile URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getProfilePath() {
    return "/security/profiles/${encodeURIComponent(String(uri))}";
  }
  getProfile(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/profiles/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a user
   * 
   * @param uri User URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getUserPath() {
    return "/security/users/${encodeURIComponent(String(uri))}";
  }
  getUser(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/users/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get groups of a user
   * 
   * @param uri User URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getUserGroupsPath() {
    return "/security/accounts/${encodeURIComponent(String(uri))}/groups";
  }
  getUserGroups(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/accounts/${encodeURIComponent(String(e))}/groups`, t);
  }
  /**
   * Get groups of a user
   * 
   * @param uri User URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getUserGroups1Path() {
    return "/security/users/${encodeURIComponent(String(uri))}/groups";
  }
  getUserGroups1(e, r = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/users/${encodeURIComponent(String(e))}/groups`, t);
  }
  /**
   * Get users by their URIs
   * 
   * @param uris Users URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getUsersByURIPath() {
    return "/security/users/by_uris";
  }
  getUsersByURI(e, r = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getUsersByURI.");
    return e && e.forEach((a) => {
      o.push("uris=" + encodeURIComponent(String(a)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/users/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search accounts
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering list by name or email
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchAccountsPath() {
    return "/security/accounts";
  }
  searchAccounts(e, r, t, o, u = "body", a = {}) {
    let f = [];
    return e !== void 0 && f.push("name=" + encodeURIComponent(String(e))), r && r.forEach((d) => {
      f.push("order_by=" + encodeURIComponent(String(d)));
    }), t !== void 0 && f.push("page=" + encodeURIComponent(String(t))), o !== void 0 && f.push("page_size=" + encodeURIComponent(String(o))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/accounts${f.length > 0 ? "?" + f.join("&") : ""}`, a);
  }
  /**
   * Search groups
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering list by name
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchGroupsPath() {
    return "/security/groups";
  }
  searchGroups(e, r, t, o, u = "body", a = {}) {
    let f = [];
    return e !== void 0 && f.push("name=" + encodeURIComponent(String(e))), r && r.forEach((d) => {
      f.push("order_by=" + encodeURIComponent(String(d)));
    }), t !== void 0 && f.push("page=" + encodeURIComponent(String(t))), o !== void 0 && f.push("page_size=" + encodeURIComponent(String(o))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/groups${f.length > 0 ? "?" + f.join("&") : ""}`, a);
  }
  /**
   * Search persons
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering list by name or email
   * @param only_without_account set &#39;true&#39; if you want to select only persons without account
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchPersonsPath() {
    return "/security/persons";
  }
  searchPersons(e, r, t, o, u, a = "body", f = {}) {
    let g = [];
    return e !== void 0 && g.push("name=" + encodeURIComponent(String(e))), r !== void 0 && g.push("only_without_account=" + encodeURIComponent(String(r))), t && t.forEach((w) => {
      g.push("order_by=" + encodeURIComponent(String(w)));
    }), o !== void 0 && g.push("page=" + encodeURIComponent(String(o))), u !== void 0 && g.push("page_size=" + encodeURIComponent(String(u))), f.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/persons${g.length > 0 ? "?" + g.join("&") : ""}`, f);
  }
  /**
   * Search profiles
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering list by name
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchProfilesPath() {
    return "/security/profiles";
  }
  searchProfiles(e, r, t, o, u = "body", a = {}) {
    let f = [];
    return e !== void 0 && f.push("name=" + encodeURIComponent(String(e))), r && r.forEach((d) => {
      f.push("order_by=" + encodeURIComponent(String(d)));
    }), t !== void 0 && f.push("page=" + encodeURIComponent(String(t))), o !== void 0 && f.push("page_size=" + encodeURIComponent(String(o))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/profiles${f.length > 0 ? "?" + f.join("&") : ""}`, a);
  }
  /**
   * Search users
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering list by name or email
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchUsersPath() {
    return "/security/users";
  }
  searchUsers(e, r, t, o, u = "body", a = {}) {
    let f = [];
    return e !== void 0 && f.push("name=" + encodeURIComponent(String(e))), r && r.forEach((d) => {
      f.push("order_by=" + encodeURIComponent(String(d)));
    }), t !== void 0 && f.push("page=" + encodeURIComponent(String(t))), o !== void 0 && f.push("page_size=" + encodeURIComponent(String(o))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/security/users${f.length > 0 ? "?" + f.join("&") : ""}`, a);
  }
  /**
   * Update an account
   * 
   * @param Authorization Authentication token
   * @param body Account description
   * @param Accept_Language Request accepted language
   
   */
  updateAccountPath() {
    return "/security/accounts";
  }
  updateAccount(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/security/accounts`, e, t);
  }
  /**
   * Update a group
   * 
   * @param Authorization Authentication token
   * @param body Group description
   * @param Accept_Language Request accepted language
   
   */
  updateGroupPath() {
    return "/security/groups";
  }
  updateGroup(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/security/groups`, e, t);
  }
  /**
   * Update a person
   * 
   * @param Authorization Authentication token
   * @param body Person description
   * @param Accept_Language Request accepted language
   
   */
  updatePersonPath() {
    return "/security/persons";
  }
  updatePerson(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/security/persons`, e, t);
  }
  /**
   * Update a profile
   * 
   * @param Authorization Authentication token
   * @param body Profile description
   * @param Accept_Language Request accepted language
   
   */
  updateProfilePath() {
    return "/security/profiles";
  }
  updateProfile(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/security/profiles`, e, t);
  }
  /**
   * Update a user
   * 
   * @param Authorization Authentication token
   * @param body User description
   * @param Accept_Language Request accepted language
   
   */
  updateUserPath() {
    return "/security/users";
  }
  updateUser(e, r = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/security/users`, e, t);
  }
};
V = ft([
  W(),
  he(0, x("IApiHttpClient")),
  he(1, x("IAPIConfiguration"))
], V);
var le;
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(le || (le = {}));
const Rt = "", It = {
  csv: ",",
  tsv: "   ",
  ssv: " ",
  pipes: "|"
};
class St {
  static with(r) {
    r.bind("AuthenticationService").to(J).inSingletonScope(), r.bind("SecurityService").to(V).inSingletonScope();
  }
}
var A = typeof globalThis < "u" && globalThis || typeof self < "u" && self || typeof A < "u" && A, I = {
  searchParams: "URLSearchParams" in A,
  iterable: "Symbol" in A && "iterator" in Symbol,
  blob: "FileReader" in A && "Blob" in A && function() {
    try {
      return new Blob(), !0;
    } catch {
      return !1;
    }
  }(),
  formData: "FormData" in A,
  arrayBuffer: "ArrayBuffer" in A
};
function pt(e) {
  return e && DataView.prototype.isPrototypeOf(e);
}
if (I.arrayBuffer)
  var ht = [
    "[object Int8Array]",
    "[object Uint8Array]",
    "[object Uint8ClampedArray]",
    "[object Int16Array]",
    "[object Uint16Array]",
    "[object Int32Array]",
    "[object Uint32Array]",
    "[object Float32Array]",
    "[object Float64Array]"
  ], lt = ArrayBuffer.isView || function(e) {
    return e && ht.indexOf(Object.prototype.toString.call(e)) > -1;
  };
function B(e) {
  if (typeof e != "string" && (e = String(e)), /[^a-z0-9\-#$%&'*+.^_`|~!]/i.test(e) || e === "")
    throw new TypeError("Invalid character in header field name");
  return e.toLowerCase();
}
function z(e) {
  return typeof e != "string" && (e = String(e)), e;
}
function Y(e) {
  var r = {
    next: function() {
      var t = e.shift();
      return { done: t === void 0, value: t };
    }
  };
  return I.iterable && (r[Symbol.iterator] = function() {
    return r;
  }), r;
}
function P(e) {
  this.map = {}, e instanceof P ? e.forEach(function(r, t) {
    this.append(t, r);
  }, this) : Array.isArray(e) ? e.forEach(function(r) {
    this.append(r[0], r[1]);
  }, this) : e && Object.getOwnPropertyNames(e).forEach(function(r) {
    this.append(r, e[r]);
  }, this);
}
P.prototype.append = function(e, r) {
  e = B(e), r = z(r);
  var t = this.map[e];
  this.map[e] = t ? t + ", " + r : r;
};
P.prototype.delete = function(e) {
  delete this.map[B(e)];
};
P.prototype.get = function(e) {
  return e = B(e), this.has(e) ? this.map[e] : null;
};
P.prototype.has = function(e) {
  return this.map.hasOwnProperty(B(e));
};
P.prototype.set = function(e, r) {
  this.map[B(e)] = z(r);
};
P.prototype.forEach = function(e, r) {
  for (var t in this.map)
    this.map.hasOwnProperty(t) && e.call(r, this.map[t], t, this);
};
P.prototype.keys = function() {
  var e = [];
  return this.forEach(function(r, t) {
    e.push(t);
  }), Y(e);
};
P.prototype.values = function() {
  var e = [];
  return this.forEach(function(r) {
    e.push(r);
  }), Y(e);
};
P.prototype.entries = function() {
  var e = [];
  return this.forEach(function(r, t) {
    e.push([t, r]);
  }), Y(e);
};
I.iterable && (P.prototype[Symbol.iterator] = P.prototype.entries);
function H(e) {
  if (e.bodyUsed)
    return Promise.reject(new TypeError("Already read"));
  e.bodyUsed = !0;
}
function ge(e) {
  return new Promise(function(r, t) {
    e.onload = function() {
      r(e.result);
    }, e.onerror = function() {
      t(e.error);
    };
  });
}
function dt(e) {
  var r = new FileReader(), t = ge(r);
  return r.readAsArrayBuffer(e), t;
}
function yt(e) {
  var r = new FileReader(), t = ge(r);
  return r.readAsText(e), t;
}
function bt(e) {
  for (var r = new Uint8Array(e), t = new Array(r.length), o = 0; o < r.length; o++)
    t[o] = String.fromCharCode(r[o]);
  return t.join("");
}
function de(e) {
  if (e.slice)
    return e.slice(0);
  var r = new Uint8Array(e.byteLength);
  return r.set(new Uint8Array(e)), r.buffer;
}
function we() {
  return this.bodyUsed = !1, this._initBody = function(e) {
    this.bodyUsed = this.bodyUsed, this._bodyInit = e, e ? typeof e == "string" ? this._bodyText = e : I.blob && Blob.prototype.isPrototypeOf(e) ? this._bodyBlob = e : I.formData && FormData.prototype.isPrototypeOf(e) ? this._bodyFormData = e : I.searchParams && URLSearchParams.prototype.isPrototypeOf(e) ? this._bodyText = e.toString() : I.arrayBuffer && I.blob && pt(e) ? (this._bodyArrayBuffer = de(e.buffer), this._bodyInit = new Blob([this._bodyArrayBuffer])) : I.arrayBuffer && (ArrayBuffer.prototype.isPrototypeOf(e) || lt(e)) ? this._bodyArrayBuffer = de(e) : this._bodyText = e = Object.prototype.toString.call(e) : this._bodyText = "", this.headers.get("content-type") || (typeof e == "string" ? this.headers.set("content-type", "text/plain;charset=UTF-8") : this._bodyBlob && this._bodyBlob.type ? this.headers.set("content-type", this._bodyBlob.type) : I.searchParams && URLSearchParams.prototype.isPrototypeOf(e) && this.headers.set("content-type", "application/x-www-form-urlencoded;charset=UTF-8"));
  }, I.blob && (this.blob = function() {
    var e = H(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return Promise.resolve(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(new Blob([this._bodyArrayBuffer]));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as blob");
    return Promise.resolve(new Blob([this._bodyText]));
  }, this.arrayBuffer = function() {
    if (this._bodyArrayBuffer) {
      var e = H(this);
      return e || (ArrayBuffer.isView(this._bodyArrayBuffer) ? Promise.resolve(
        this._bodyArrayBuffer.buffer.slice(
          this._bodyArrayBuffer.byteOffset,
          this._bodyArrayBuffer.byteOffset + this._bodyArrayBuffer.byteLength
        )
      ) : Promise.resolve(this._bodyArrayBuffer));
    } else
      return this.blob().then(dt);
  }), this.text = function() {
    var e = H(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return yt(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(bt(this._bodyArrayBuffer));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as text");
    return Promise.resolve(this._bodyText);
  }, I.formData && (this.formData = function() {
    return this.text().then(wt);
  }), this.json = function() {
    return this.text().then(JSON.parse);
  }, this;
}
var vt = ["DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT"];
function gt(e) {
  var r = e.toUpperCase();
  return vt.indexOf(r) > -1 ? r : e;
}
function O(e, r) {
  if (!(this instanceof O))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  r = r || {};
  var t = r.body;
  if (e instanceof O) {
    if (e.bodyUsed)
      throw new TypeError("Already read");
    this.url = e.url, this.credentials = e.credentials, r.headers || (this.headers = new P(e.headers)), this.method = e.method, this.mode = e.mode, this.signal = e.signal, !t && e._bodyInit != null && (t = e._bodyInit, e.bodyUsed = !0);
  } else
    this.url = String(e);
  if (this.credentials = r.credentials || this.credentials || "same-origin", (r.headers || !this.headers) && (this.headers = new P(r.headers)), this.method = gt(r.method || this.method || "GET"), this.mode = r.mode || this.mode || null, this.signal = r.signal || this.signal, this.referrer = null, (this.method === "GET" || this.method === "HEAD") && t)
    throw new TypeError("Body not allowed for GET or HEAD requests");
  if (this._initBody(t), (this.method === "GET" || this.method === "HEAD") && (r.cache === "no-store" || r.cache === "no-cache")) {
    var o = /([?&])_=[^&]*/;
    if (o.test(this.url))
      this.url = this.url.replace(o, "$1_=" + (/* @__PURE__ */ new Date()).getTime());
    else {
      var u = /\?/;
      this.url += (u.test(this.url) ? "&" : "?") + "_=" + (/* @__PURE__ */ new Date()).getTime();
    }
  }
}
O.prototype.clone = function() {
  return new O(this, { body: this._bodyInit });
};
function wt(e) {
  var r = new FormData();
  return e.trim().split("&").forEach(function(t) {
    if (t) {
      var o = t.split("="), u = o.shift().replace(/\+/g, " "), a = o.join("=").replace(/\+/g, " ");
      r.append(decodeURIComponent(u), decodeURIComponent(a));
    }
  }), r;
}
function mt(e) {
  var r = new P(), t = e.replace(/\r?\n[\t ]+/g, " ");
  return t.split(/\r?\n/).forEach(function(o) {
    var u = o.split(":"), a = u.shift().trim();
    if (a) {
      var f = u.join(":").trim();
      r.append(a, f);
    }
  }), r;
}
we.call(O.prototype);
function S(e, r) {
  if (!(this instanceof S))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  r || (r = {}), this.type = "default", this.status = r.status === void 0 ? 200 : r.status, this.ok = this.status >= 200 && this.status < 300, this.statusText = "statusText" in r ? r.statusText : "", this.headers = new P(r.headers), this.url = r.url || "", this._initBody(e);
}
we.call(S.prototype);
S.prototype.clone = function() {
  return new S(this._bodyInit, {
    status: this.status,
    statusText: this.statusText,
    headers: new P(this.headers),
    url: this.url
  });
};
S.error = function() {
  var e = new S(null, { status: 0, statusText: "" });
  return e.type = "error", e;
};
var Pt = [301, 302, 303, 307, 308];
S.redirect = function(e, r) {
  if (Pt.indexOf(r) === -1)
    throw new RangeError("Invalid status code");
  return new S(null, { status: r, headers: { location: e } });
};
var j = A.DOMException;
try {
  new j();
} catch {
  j = function(r, t) {
    this.message = r, this.name = t;
    var o = Error(r);
    this.stack = o.stack;
  }, j.prototype = Object.create(Error.prototype), j.prototype.constructor = j;
}
function me(e, r) {
  return new Promise(function(t, o) {
    var u = new O(e, r);
    if (u.signal && u.signal.aborted)
      return o(new j("Aborted", "AbortError"));
    var a = new XMLHttpRequest();
    function f() {
      a.abort();
    }
    a.onload = function() {
      var d = {
        status: a.status,
        statusText: a.statusText,
        headers: mt(a.getAllResponseHeaders() || "")
      };
      d.url = "responseURL" in a ? a.responseURL : d.headers.get("X-Request-URL");
      var w = "response" in a ? a.response : a.responseText;
      setTimeout(function() {
        t(new S(w, d));
      }, 0);
    }, a.onerror = function() {
      setTimeout(function() {
        o(new TypeError("Network request failed"));
      }, 0);
    }, a.ontimeout = function() {
      setTimeout(function() {
        o(new TypeError("Network request failed"));
      }, 0);
    }, a.onabort = function() {
      setTimeout(function() {
        o(new j("Aborted", "AbortError"));
      }, 0);
    };
    function g(d) {
      try {
        return d === "" && A.location.href ? A.location.href : d;
      } catch {
        return d;
      }
    }
    a.open(u.method, g(u.url), !0), u.credentials === "include" ? a.withCredentials = !0 : u.credentials === "omit" && (a.withCredentials = !1), "responseType" in a && (I.blob ? a.responseType = "blob" : I.arrayBuffer && u.headers.get("Content-Type") && u.headers.get("Content-Type").indexOf("application/octet-stream") !== -1 && (a.responseType = "arraybuffer")), r && typeof r.headers == "object" && !(r.headers instanceof P) ? Object.getOwnPropertyNames(r.headers).forEach(function(d) {
      a.setRequestHeader(d, z(r.headers[d]));
    }) : u.headers.forEach(function(d, w) {
      a.setRequestHeader(w, d);
    }), u.signal && (u.signal.addEventListener("abort", f), a.onreadystatechange = function() {
      a.readyState === 4 && u.signal.removeEventListener("abort", f);
    }), a.send(typeof u._bodyInit > "u" ? null : u._bodyInit);
  });
}
me.polyfill = !0;
A.fetch || (A.fetch = me, A.Headers = P, A.Request = O, A.Response = S);
class _t {
  constructor(r, t, o) {
    this.response = r, this.status = t, this.headers = o;
  }
}
class ye {
  constructor(r, t, o, u) {
    this.message = r, this.level = t, this.translationKey = o, this.translationValues = u;
  }
}
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(ye || (ye = {}));
var Ct = Object.getOwnPropertyDescriptor, At = (e, r, t, o) => {
  for (var u = o > 1 ? void 0 : o ? Ct(r, t) : r, a = e.length - 1, f; a >= 0; a--)
    (f = e[a]) && (u = f(u) || u);
  return u;
};
let be = class {
  get(e, r) {
    return this.performNetworkCall(e, "get", void 0, r);
  }
  post(e, r, t) {
    return this.performNetworkCall(e, "post", this.getJsonBody(r), this.addJsonHeaders(t));
  }
  put(e, r, t) {
    return this.performNetworkCall(e, "put", this.getJsonBody(r), this.addJsonHeaders(t));
  }
  delete(e, r) {
    return this.performNetworkCall(e, "delete", void 0, r);
  }
  getJsonBody(e) {
    return JSON.stringify(e);
  }
  addJsonHeaders(e) {
    return Object.assign({}, {
      Accept: "application/json",
      "Content-Type": "application/json"
    }, e);
  }
  performNetworkCall(e, r, t, o) {
    let u;
    return typeof window > "u" ? u = require("node-fetch") : u = window.fetch, u(e, {
      method: r,
      body: t,
      mode: "cors",
      headers: o
    }).then((f) => {
      let g = {};
      return f.headers.forEach((d, w) => {
        g[w.toString().toLowerCase()] = d;
      }), f.text().then((d) => {
        let w = g["content-type"] || "", R;
        w.match("application/json") ? (R = JSON.parse(d), R.metadata && R.metadata) : R = d;
        let E = new _t(R, f.status, g);
        if (f.status >= 400)
          throw E;
        return E;
      });
    });
  }
};
be = At([
  W()
], be);
export {
  St as ApiServiceBinder,
  J as AuthenticationService,
  It as COLLECTION_FORMATS,
  V as SecurityService,
  le as StatusDTO,
  Rt as __fakeExport
};
