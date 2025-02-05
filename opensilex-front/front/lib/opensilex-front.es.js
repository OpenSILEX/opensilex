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
var fe;
function Ye() {
  if (fe) return ue;
  fe = 1;
  var e;
  return function(t) {
    (function(n) {
      var s = typeof ae == "object" ? ae : typeof self == "object" ? self : typeof this == "object" ? this : Function("return this;")(), a = f(t);
      typeof s.Reflect > "u" ? s.Reflect = t : a = f(s.Reflect, a), n(a);
      function f(d, A) {
        return function(y, m) {
          typeof d[y] != "function" && Object.defineProperty(d, y, { configurable: !0, writable: !0, value: m }), A && A(y, m);
        };
      }
    })(function(n) {
      var s = Object.prototype.hasOwnProperty, a = typeof Symbol == "function", f = a && typeof Symbol.toPrimitive < "u" ? Symbol.toPrimitive : "@@toPrimitive", d = a && typeof Symbol.iterator < "u" ? Symbol.iterator : "@@iterator", A = typeof Object.create == "function", y = { __proto__: [] } instanceof Array, m = !A && !y, E = {
        // create an object in dictionary mode (a.k.a. "slow" mode in v8)
        create: A ? function() {
          return q(/* @__PURE__ */ Object.create(null));
        } : y ? function() {
          return q({ __proto__: null });
        } : function() {
          return q({});
        },
        has: m ? function(r, o) {
          return s.call(r, o);
        } : function(r, o) {
          return o in r;
        },
        get: m ? function(r, o) {
          return s.call(r, o) ? r[o] : void 0;
        } : function(r, o) {
          return r[o];
        }
      }, j = Object.getPrototypeOf(Function), F = typeof process == "object" && process.env && process.env.REFLECT_METADATA_USE_MAP_POLYFILL === "true", N = !F && typeof Map == "function" && typeof Map.prototype.entries == "function" ? Map : Je(), Te = !F && typeof Set == "function" && typeof Set.prototype.entries == "function" ? Set : We(), Pe = !F && typeof WeakMap == "function" ? WeakMap : ze(), k = new Pe();
      function Ae(r, o, i, u) {
        if (w(i)) {
          if (!ne(r))
            throw new TypeError();
          if (!oe(o))
            throw new TypeError();
          return Me(r, o);
        } else {
          if (!ne(r))
            throw new TypeError();
          if (!T(o))
            throw new TypeError();
          if (!T(u) && !w(u) && !x(u))
            throw new TypeError();
          return x(u) && (u = void 0), i = C(i), Ue(r, o, i, u);
        }
      }
      n("decorate", Ae);
      function Ee(r, o) {
        function i(u, l) {
          if (!T(u))
            throw new TypeError();
          if (!w(l) && !Le(l))
            throw new TypeError();
          K(r, o, u, l);
        }
        return i;
      }
      n("metadata", Ee);
      function Re(r, o, i, u) {
        if (!T(i))
          throw new TypeError();
        return w(u) || (u = C(u)), K(r, o, i, u);
      }
      n("defineMetadata", Re);
      function Oe(r, o, i) {
        if (!T(o))
          throw new TypeError();
        return w(i) || (i = C(i)), X(r, o, i);
      }
      n("hasMetadata", Oe);
      function Ce(r, o, i) {
        if (!T(o))
          throw new TypeError();
        return w(i) || (i = C(i)), L(r, o, i);
      }
      n("hasOwnMetadata", Ce);
      function je(r, o, i) {
        if (!T(o))
          throw new TypeError();
        return w(i) || (i = C(i)), Z(r, o, i);
      }
      n("getMetadata", je);
      function Ie(r, o, i) {
        if (!T(o))
          throw new TypeError();
        return w(i) || (i = C(i)), Q(r, o, i);
      }
      n("getOwnMetadata", Ie);
      function Se(r, o) {
        if (!T(r))
          throw new TypeError();
        return w(o) || (o = C(o)), ee(r, o);
      }
      n("getMetadataKeys", Se);
      function De(r, o) {
        if (!T(r))
          throw new TypeError();
        return w(o) || (o = C(o)), te(r, o);
      }
      n("getOwnMetadataKeys", De);
      function xe(r, o, i) {
        if (!T(o))
          throw new TypeError();
        w(i) || (i = C(i));
        var u = M(
          o,
          i,
          /*Create*/
          !1
        );
        if (w(u) || !u.delete(r))
          return !1;
        if (u.size > 0)
          return !0;
        var l = k.get(o);
        return l.delete(i), l.size > 0 || k.delete(o), !0;
      }
      n("deleteMetadata", xe);
      function Me(r, o) {
        for (var i = r.length - 1; i >= 0; --i) {
          var u = r[i], l = u(o);
          if (!w(l) && !x(l)) {
            if (!oe(l))
              throw new TypeError();
            o = l;
          }
        }
        return o;
      }
      function Ue(r, o, i, u) {
        for (var l = r.length - 1; l >= 0; --l) {
          var g = r[l], h = g(o, i, u);
          if (!w(h) && !x(h)) {
            if (!T(h))
              throw new TypeError();
            u = h;
          }
        }
        return u;
      }
      function M(r, o, i) {
        var u = k.get(r);
        if (w(u)) {
          if (!i)
            return;
          u = new N(), k.set(r, u);
        }
        var l = u.get(o);
        if (w(l)) {
          if (!i)
            return;
          l = new N(), u.set(o, l);
        }
        return l;
      }
      function X(r, o, i) {
        var u = L(r, o, i);
        if (u)
          return !0;
        var l = G(o);
        return x(l) ? !1 : X(r, l, i);
      }
      function L(r, o, i) {
        var u = M(
          o,
          i,
          /*Create*/
          !1
        );
        return w(u) ? !1 : Fe(u.has(r));
      }
      function Z(r, o, i) {
        var u = L(r, o, i);
        if (u)
          return Q(r, o, i);
        var l = G(o);
        if (!x(l))
          return Z(r, l, i);
      }
      function Q(r, o, i) {
        var u = M(
          o,
          i,
          /*Create*/
          !1
        );
        if (!w(u))
          return u.get(r);
      }
      function K(r, o, i, u) {
        var l = M(
          i,
          u,
          /*Create*/
          !0
        );
        l.set(r, o);
      }
      function ee(r, o) {
        var i = te(r, o), u = G(r);
        if (u === null)
          return i;
        var l = ee(u, o);
        if (l.length <= 0)
          return i;
        if (i.length <= 0)
          return l;
        for (var g = new Te(), h = [], p = 0, c = i; p < c.length; p++) {
          var v = c[p], b = g.has(v);
          b || (g.add(v), h.push(v));
        }
        for (var I = 0, se = l; I < se.length; I++) {
          var v = se[I], b = g.has(v);
          b || (g.add(v), h.push(v));
        }
        return h;
      }
      function te(r, o) {
        var i = [], u = M(
          r,
          o,
          /*Create*/
          !1
        );
        if (w(u))
          return i;
        for (var l = u.keys(), g = Ge(l), h = 0; ; ) {
          var p = He(g);
          if (!p)
            return i.length = h, i;
          var c = qe(p);
          try {
            i[h] = c;
          } catch (v) {
            try {
              Ve(g);
            } finally {
              throw v;
            }
          }
          h++;
        }
      }
      function re(r) {
        if (r === null)
          return 1;
        switch (typeof r) {
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
            return r === null ? 1 : 6;
          default:
            return 6;
        }
      }
      function w(r) {
        return r === void 0;
      }
      function x(r) {
        return r === null;
      }
      function ke(r) {
        return typeof r == "symbol";
      }
      function T(r) {
        return typeof r == "object" ? r !== null : typeof r == "function";
      }
      function Be(r, o) {
        switch (re(r)) {
          case 0:
            return r;
          case 1:
            return r;
          case 2:
            return r;
          case 3:
            return r;
          case 4:
            return r;
          case 5:
            return r;
        }
        var i = "string", u = ie(r, f);
        if (u !== void 0) {
          var l = u.call(r, i);
          if (T(l))
            throw new TypeError();
          return l;
        }
        return $e(r);
      }
      function $e(r, o) {
        var i, u, l;
        {
          var g = r.toString;
          if (B(g)) {
            var u = g.call(r);
            if (!T(u))
              return u;
          }
          var i = r.valueOf;
          if (B(i)) {
            var u = i.call(r);
            if (!T(u))
              return u;
          }
        }
        throw new TypeError();
      }
      function Fe(r) {
        return !!r;
      }
      function Ne(r) {
        return "" + r;
      }
      function C(r) {
        var o = Be(r);
        return ke(o) ? o : Ne(o);
      }
      function ne(r) {
        return Array.isArray ? Array.isArray(r) : r instanceof Object ? r instanceof Array : Object.prototype.toString.call(r) === "[object Array]";
      }
      function B(r) {
        return typeof r == "function";
      }
      function oe(r) {
        return typeof r == "function";
      }
      function Le(r) {
        switch (re(r)) {
          case 3:
            return !0;
          case 4:
            return !0;
          default:
            return !1;
        }
      }
      function ie(r, o) {
        var i = r[o];
        if (i != null) {
          if (!B(i))
            throw new TypeError();
          return i;
        }
      }
      function Ge(r) {
        var o = ie(r, d);
        if (!B(o))
          throw new TypeError();
        var i = o.call(r);
        if (!T(i))
          throw new TypeError();
        return i;
      }
      function qe(r) {
        return r.value;
      }
      function He(r) {
        var o = r.next();
        return o.done ? !1 : o;
      }
      function Ve(r) {
        var o = r.return;
        o && o.call(r);
      }
      function G(r) {
        var o = Object.getPrototypeOf(r);
        if (typeof r != "function" || r === j || o !== j)
          return o;
        var i = r.prototype, u = i && Object.getPrototypeOf(i);
        if (u == null || u === Object.prototype)
          return o;
        var l = u.constructor;
        return typeof l != "function" || l === r ? o : l;
      }
      function Je() {
        var r = {}, o = [], i = (
          /** @class */
          function() {
            function h(p, c, v) {
              this._index = 0, this._keys = p, this._values = c, this._selector = v;
            }
            return h.prototype["@@iterator"] = function() {
              return this;
            }, h.prototype[d] = function() {
              return this;
            }, h.prototype.next = function() {
              var p = this._index;
              if (p >= 0 && p < this._keys.length) {
                var c = this._selector(this._keys[p], this._values[p]);
                return p + 1 >= this._keys.length ? (this._index = -1, this._keys = o, this._values = o) : this._index++, { value: c, done: !1 };
              }
              return { value: void 0, done: !0 };
            }, h.prototype.throw = function(p) {
              throw this._index >= 0 && (this._index = -1, this._keys = o, this._values = o), p;
            }, h.prototype.return = function(p) {
              return this._index >= 0 && (this._index = -1, this._keys = o, this._values = o), { value: p, done: !0 };
            }, h;
          }()
        );
        return (
          /** @class */
          function() {
            function h() {
              this._keys = [], this._values = [], this._cacheKey = r, this._cacheIndex = -2;
            }
            return Object.defineProperty(h.prototype, "size", {
              get: function() {
                return this._keys.length;
              },
              enumerable: !0,
              configurable: !0
            }), h.prototype.has = function(p) {
              return this._find(
                p,
                /*insert*/
                !1
              ) >= 0;
            }, h.prototype.get = function(p) {
              var c = this._find(
                p,
                /*insert*/
                !1
              );
              return c >= 0 ? this._values[c] : void 0;
            }, h.prototype.set = function(p, c) {
              var v = this._find(
                p,
                /*insert*/
                !0
              );
              return this._values[v] = c, this;
            }, h.prototype.delete = function(p) {
              var c = this._find(
                p,
                /*insert*/
                !1
              );
              if (c >= 0) {
                for (var v = this._keys.length, b = c + 1; b < v; b++)
                  this._keys[b - 1] = this._keys[b], this._values[b - 1] = this._values[b];
                return this._keys.length--, this._values.length--, p === this._cacheKey && (this._cacheKey = r, this._cacheIndex = -2), !0;
              }
              return !1;
            }, h.prototype.clear = function() {
              this._keys.length = 0, this._values.length = 0, this._cacheKey = r, this._cacheIndex = -2;
            }, h.prototype.keys = function() {
              return new i(this._keys, this._values, u);
            }, h.prototype.values = function() {
              return new i(this._keys, this._values, l);
            }, h.prototype.entries = function() {
              return new i(this._keys, this._values, g);
            }, h.prototype["@@iterator"] = function() {
              return this.entries();
            }, h.prototype[d] = function() {
              return this.entries();
            }, h.prototype._find = function(p, c) {
              return this._cacheKey !== p && (this._cacheIndex = this._keys.indexOf(this._cacheKey = p)), this._cacheIndex < 0 && c && (this._cacheIndex = this._keys.length, this._keys.push(p), this._values.push(void 0)), this._cacheIndex;
            }, h;
          }()
        );
        function u(h, p) {
          return h;
        }
        function l(h, p) {
          return p;
        }
        function g(h, p) {
          return [h, p];
        }
      }
      function We() {
        return (
          /** @class */
          function() {
            function r() {
              this._map = new N();
            }
            return Object.defineProperty(r.prototype, "size", {
              get: function() {
                return this._map.size;
              },
              enumerable: !0,
              configurable: !0
            }), r.prototype.has = function(o) {
              return this._map.has(o);
            }, r.prototype.add = function(o) {
              return this._map.set(o, o), this;
            }, r.prototype.delete = function(o) {
              return this._map.delete(o);
            }, r.prototype.clear = function() {
              this._map.clear();
            }, r.prototype.keys = function() {
              return this._map.keys();
            }, r.prototype.values = function() {
              return this._map.values();
            }, r.prototype.entries = function() {
              return this._map.entries();
            }, r.prototype["@@iterator"] = function() {
              return this.keys();
            }, r.prototype[d] = function() {
              return this.keys();
            }, r;
          }()
        );
      }
      function ze() {
        var r = 16, o = E.create(), i = u();
        return (
          /** @class */
          function() {
            function c() {
              this._key = u();
            }
            return c.prototype.has = function(v) {
              var b = l(
                v,
                /*create*/
                !1
              );
              return b !== void 0 ? E.has(b, this._key) : !1;
            }, c.prototype.get = function(v) {
              var b = l(
                v,
                /*create*/
                !1
              );
              return b !== void 0 ? E.get(b, this._key) : void 0;
            }, c.prototype.set = function(v, b) {
              var I = l(
                v,
                /*create*/
                !0
              );
              return I[this._key] = b, this;
            }, c.prototype.delete = function(v) {
              var b = l(
                v,
                /*create*/
                !1
              );
              return b !== void 0 ? delete b[this._key] : !1;
            }, c.prototype.clear = function() {
              this._key = u();
            }, c;
          }()
        );
        function u() {
          var c;
          do
            c = "@@WeakMap@@" + p();
          while (E.has(o, c));
          return o[c] = !0, c;
        }
        function l(c, v) {
          if (!s.call(c, i)) {
            if (!v)
              return;
            Object.defineProperty(c, i, { value: E.create() });
          }
          return c[i];
        }
        function g(c, v) {
          for (var b = 0; b < v; ++b)
            c[b] = Math.random() * 255 | 0;
          return c;
        }
        function h(c) {
          return typeof Uint8Array == "function" ? typeof crypto < "u" ? crypto.getRandomValues(new Uint8Array(c)) : typeof msCrypto < "u" ? msCrypto.getRandomValues(new Uint8Array(c)) : g(new Uint8Array(c), c) : g(new Array(c), c);
        }
        function p() {
          var c = h(r);
          c[6] = c[6] & 79 | 64, c[8] = c[8] & 191 | 128;
          for (var v = "", b = 0; b < r; ++b) {
            var I = c[b];
            (b === 4 || b === 6 || b === 8) && (v += "-"), I < 16 && (v += "0"), v += I.toString(16).toLowerCase();
          }
          return v;
        }
      }
      function q(r) {
        return r.__ = void 0, delete r.__, r;
      }
    });
  }(e || (e = {})), ue;
}
Ye();
var Xe = "named", Ze = "inject", Qe = "inversify:tagged", Ke = "inversify:tagged_props", ce = "inversify:paramtypes", et = "design:paramtypes", tt = "Cannot apply @injectable decorator multiple times.", rt = "Metadata key was used more than once in a parameter:", nt = function(e) {
  return "@inject called with undefined this could mean that the class " + e + " has a circular dependency problem. You can use a LazyServiceIdentifer to  overcome this limitation.";
}, ot = "The @inject @multiInject @tagged and @named decorators must be applied to the parameters of a class constructor or a class property.", it = function() {
  function e(t, n) {
    this.key = t, this.value = n;
  }
  return e.prototype.toString = function() {
    return this.key === Xe ? "named: " + this.value.toString() + " " : "tagged: { key:" + this.key.toString() + ", value: " + this.value + " }";
  }, e;
}();
function st(e, t, n, s) {
  var a = Qe;
  we(a, e, t, s, n);
}
function at(e, t, n) {
  var s = Ke;
  we(s, e.constructor, t, n);
}
function we(e, t, n, s, a) {
  var f = {}, d = typeof a == "number", A = a !== void 0 && d ? a.toString() : n;
  if (d && n !== void 0)
    throw new Error(ot);
  Reflect.hasOwnMetadata(e, t) && (f = Reflect.getMetadata(e, t));
  var y = f[A];
  if (!Array.isArray(y))
    y = [];
  else
    for (var m = 0, E = y; m < E.length; m++) {
      var j = E[m];
      if (j.key === s.key)
        throw new Error(rt + " " + j.key.toString());
    }
  y.push(s), f[A] = y, Reflect.defineMetadata(e, f, t);
}
function $(e) {
  return function(t, n, s) {
    if (e === void 0)
      throw new Error(nt(t.name));
    var a = new it(Ze, e);
    typeof s == "number" ? st(t, n, s, a) : at(t, n, a);
  };
}
function W() {
  return function(e) {
    if (Reflect.hasOwnMetadata(ce, e))
      throw new Error(tt);
    var t = Reflect.getMetadata(et, e) || [];
    return Reflect.defineMetadata(ce, t, e), e;
  };
}
var ut = Object.getOwnPropertyDescriptor, ft = (e, t, n, s) => {
  for (var a = s > 1 ? void 0 : s ? ut(t, n) : t, f = e.length - 1, d; f >= 0; f--)
    (d = e[f]) && (a = d(a) || a);
  return a;
}, le = (e, t) => (n, s) => t(n, s, e);
let V = class {
  constructor(e, t) {
    this.httpClient = e, this.APIConfiguration = t, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Return the current configuration
   * 
   * @param Accept_Language Request accepted language
   
   */
  getConfigPath() {
    return "/vuejs/config";
  }
  getConfig(e = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/config`, t);
  }
  /**
   * Return the front Vue JS extension file to include
   * 
   * @param module Module identifier
   
   */
  getExtensionPath() {
    return "/vuejs/extension/js/${encodeURIComponent(String(module))}.js";
  }
  getExtension(e, t = "body", n = {}) {
    return n.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/vuejs/extension/js/${encodeURIComponent(String(e))}.js`, n);
  }
  /**
   * Return the front Vue JS extension css file to include
   * 
   * @param module Module identifier
   
   */
  getExtensionStylePath() {
    return "/vuejs/extension/css/${encodeURIComponent(String(module))}.css";
  }
  getExtensionStyle(e, t = "body", n = {}) {
    return n.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/vuejs/extension/css/${encodeURIComponent(String(e))}.css`, n);
  }
  /**
   * Return the front Vue JS theme configuration
   * 
   * @param moduleId Module identifier
   * @param themeId Theme identifier
   
   */
  getThemeConfigPath() {
    return "/vuejs/theme/${encodeURIComponent(String(moduleId))}/${encodeURIComponent(String(themeId))}/config";
  }
  getThemeConfig(e, t, n = "body", s = {}) {
    return s.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/theme/${encodeURIComponent(String(e))}/${encodeURIComponent(String(t))}/config`, s);
  }
  /**
   * Return the theme css file
   * 
   * @param moduleId Module identifier
   * @param themeId Theme identifier
   
   */
  getThemeCssPath() {
    return "/vuejs/theme/${encodeURIComponent(String(moduleId))}/${encodeURIComponent(String(themeId))}/style.css";
  }
  getThemeCss(e, t, n = "body", s = {}) {
    return s.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/vuejs/theme/${encodeURIComponent(String(e))}/${encodeURIComponent(String(t))}/style.css`, s);
  }
  /**
   * Return the theme requested resource
   * 
   * @param moduleId Module identifier
   * @param themeId Theme identifier
   * @param filePath Resource path
   * @param acceptedExtensions List of supported file extensions
   
   */
  getThemeResourcePath() {
    return "/vuejs/theme/${encodeURIComponent(String(moduleId))}/${encodeURIComponent(String(themeId))}/resource";
  }
  getThemeResource(e, t, n, s, a = "body", f = {}) {
    let d = [];
    return n !== void 0 && d.push("filePath=" + encodeURIComponent(String(n))), s && s.forEach((y) => {
      d.push("acceptedExtensions=" + encodeURIComponent(String(y)));
    }), f.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/vuejs/theme/${encodeURIComponent(String(e))}/${encodeURIComponent(String(t))}/resource${d.length > 0 ? "?" + d.join("&") : ""}`, f);
  }
  /**
   * Return the user-specific configuration
   * 
   * @param Accept_Language Request accepted language
   
   */
  getUserConfigPath() {
    return "/vuejs/user_config";
  }
  getUserConfig(e = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/user_config`, t);
  }
};
V = ft([
  W(),
  le(0, $("IApiHttpClient")),
  le(1, $("IAPIConfiguration"))
], V);
var ct = Object.getOwnPropertyDescriptor, lt = (e, t, n, s) => {
  for (var a = s > 1 ? void 0 : s ? ct(t, n) : t, f = e.length - 1, d; f >= 0; f--)
    (d = e[f]) && (a = d(a) || a);
  return a;
}, he = (e, t) => (n, s) => t(n, s, e);
let J = class {
  constructor(e, t) {
    this.httpClient = e, this.APIConfiguration = t, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Create a custom class
   * 
   * @param Authorization Authentication token
   * @param body Class description
   * @param Accept_Language Request accepted language
   
   */
  createRDFTypePath() {
    return "/vuejs/owl_extension/rdf_type";
  }
  createRDFType(e, t = "body", n = {}) {
    return n.Accept = "application/json", n["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/vuejs/owl_extension/rdf_type`, e, n);
  }
  /**
   * Delete a RDF type
   * 
   * @param uri RDF type
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteRDFTypePath() {
    return "/vuejs/owl_extension/rdf_type/${encodeURIComponent(String(uri))}";
  }
  deleteRDFType(e, t = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.delete(`${this.basePath}/vuejs/owl_extension/rdf_type/${encodeURIComponent(String(e))}`, n);
  }
  /**
   * Return literal datatypes definition
   * 
   
   */
  getDataTypesPath() {
    return "/vuejs/owl_extension/data_types";
  }
  getDataTypes(e = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/owl_extension/data_types`, t);
  }
  /**
   * Return object types definition
   * 
   
   */
  getObjectTypesPath() {
    return "/vuejs/owl_extension/object_types";
  }
  getObjectTypes(e = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/owl_extension/object_types`, t);
  }
  /**
   * Return rdf type model definition with properties
   * 
   * @param rdf_type RDF type URI
   * @param Authorization Authentication token
   * @param parentType Parent RDF class URI
   * @param Accept_Language Request accepted language
   
   */
  getRDFTypePath() {
    return "/vuejs/owl_extension/rdf_type";
  }
  getRDFType(e, t, n = "body", s = {}) {
    let a = [];
    if (!e)
      throw new Error("Required parameter rdf_type was null or undefined when calling getRDFType.");
    return e !== void 0 && a.push("rdf_type=" + encodeURIComponent(String(e))), t !== void 0 && a.push("parentType=" + encodeURIComponent(String(t))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/owl_extension/rdf_type${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Return class model properties definitions
   * 
   * @param rdf_type RDF class URI
   * @param parent_type Parent RDF class URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getRDFTypePropertiesPath() {
    return "/vuejs/owl_extension/rdf_type_properties";
  }
  getRDFTypeProperties(e, t, n = "body", s = {}) {
    let a = [];
    if (!e)
      throw new Error("Required parameter rdf_type was null or undefined when calling getRDFTypeProperties.");
    if (e !== void 0 && a.push("rdf_type=" + encodeURIComponent(String(e))), !t)
      throw new Error("Required parameter parent_type was null or undefined when calling getRDFTypeProperties.");
    return t !== void 0 && a.push("parent_type=" + encodeURIComponent(String(t))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/owl_extension/rdf_type_properties${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Return RDF types parameters for Vue.js application
   * 
   
   */
  getRDFTypesParametersPath() {
    return "/vuejs/owl_extension/rdf_types_parameters";
  }
  getRDFTypesParameters(e = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/vuejs/owl_extension/rdf_types_parameters`, t);
  }
  /**
   * Define properties order
   * 
   * @param rdf_type RDF type
   * @param Authorization Authentication token
   * @param body Array of properties
   * @param Accept_Language Request accepted language
   
   */
  setRDFTypePropertiesOrderPath() {
    return "/vuejs/owl_extension/properties_order";
  }
  setRDFTypePropertiesOrder(e, t, n = "body", s = {}) {
    let a = [];
    if (!e)
      throw new Error("Required parameter rdf_type was null or undefined when calling setRDFTypePropertiesOrder.");
    return e !== void 0 && a.push("rdf_type=" + encodeURIComponent(String(e))), s.Accept = "application/json", s["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/vuejs/owl_extension/properties_order${a.length > 0 ? "?" + a.join("&") : ""}`, t, s);
  }
  /**
   * Update a custom class
   * 
   * @param Authorization Authentication token
   * @param body RDF type definition
   * @param Accept_Language Request accepted language
   
   */
  updateRDFTypePath() {
    return "/vuejs/owl_extension/rdf_type";
  }
  updateRDFType(e, t = "body", n = {}) {
    return n.Accept = "application/json", n["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/vuejs/owl_extension/rdf_type`, e, n);
  }
};
J = lt([
  W(),
  he(0, $("IApiHttpClient")),
  he(1, $("IAPIConfiguration"))
], J);
var pe;
((e) => {
  e.VersionLabelEnum = {
    DEVELOP: "DEVELOP",
    RELEASE: "RELEASE"
  };
})(pe || (pe = {}));
var de;
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(de || (de = {}));
const Rt = "", Ot = {
  csv: ",",
  tsv: "   ",
  ssv: " ",
  pipes: "|"
};
class Ct {
  static with(t) {
    t.bind("VueJsService").to(V).inSingletonScope(), t.bind("VueJsOntologyExtensionService").to(J).inSingletonScope();
  }
}
var P = typeof globalThis < "u" && globalThis || typeof self < "u" && self || typeof P < "u" && P, R = {
  searchParams: "URLSearchParams" in P,
  iterable: "Symbol" in P && "iterator" in Symbol,
  blob: "FileReader" in P && "Blob" in P && function() {
    try {
      return new Blob(), !0;
    } catch {
      return !1;
    }
  }(),
  formData: "FormData" in P,
  arrayBuffer: "ArrayBuffer" in P
};
function ht(e) {
  return e && DataView.prototype.isPrototypeOf(e);
}
if (R.arrayBuffer)
  var pt = [
    "[object Int8Array]",
    "[object Uint8Array]",
    "[object Uint8ClampedArray]",
    "[object Int16Array]",
    "[object Uint16Array]",
    "[object Int32Array]",
    "[object Uint32Array]",
    "[object Float32Array]",
    "[object Float64Array]"
  ], dt = ArrayBuffer.isView || function(e) {
    return e && pt.indexOf(Object.prototype.toString.call(e)) > -1;
  };
function U(e) {
  if (typeof e != "string" && (e = String(e)), /[^a-z0-9\-#$%&'*+.^_`|~!]/i.test(e) || e === "")
    throw new TypeError("Invalid character in header field name");
  return e.toLowerCase();
}
function z(e) {
  return typeof e != "string" && (e = String(e)), e;
}
function Y(e) {
  var t = {
    next: function() {
      var n = e.shift();
      return { done: n === void 0, value: n };
    }
  };
  return R.iterable && (t[Symbol.iterator] = function() {
    return t;
  }), t;
}
function _(e) {
  this.map = {}, e instanceof _ ? e.forEach(function(t, n) {
    this.append(n, t);
  }, this) : Array.isArray(e) ? e.forEach(function(t) {
    this.append(t[0], t[1]);
  }, this) : e && Object.getOwnPropertyNames(e).forEach(function(t) {
    this.append(t, e[t]);
  }, this);
}
_.prototype.append = function(e, t) {
  e = U(e), t = z(t);
  var n = this.map[e];
  this.map[e] = n ? n + ", " + t : t;
};
_.prototype.delete = function(e) {
  delete this.map[U(e)];
};
_.prototype.get = function(e) {
  return e = U(e), this.has(e) ? this.map[e] : null;
};
_.prototype.has = function(e) {
  return this.map.hasOwnProperty(U(e));
};
_.prototype.set = function(e, t) {
  this.map[U(e)] = z(t);
};
_.prototype.forEach = function(e, t) {
  for (var n in this.map)
    this.map.hasOwnProperty(n) && e.call(t, this.map[n], n, this);
};
_.prototype.keys = function() {
  var e = [];
  return this.forEach(function(t, n) {
    e.push(n);
  }), Y(e);
};
_.prototype.values = function() {
  var e = [];
  return this.forEach(function(t) {
    e.push(t);
  }), Y(e);
};
_.prototype.entries = function() {
  var e = [];
  return this.forEach(function(t, n) {
    e.push([n, t]);
  }), Y(e);
};
R.iterable && (_.prototype[Symbol.iterator] = _.prototype.entries);
function H(e) {
  if (e.bodyUsed)
    return Promise.reject(new TypeError("Already read"));
  e.bodyUsed = !0;
}
function _e(e) {
  return new Promise(function(t, n) {
    e.onload = function() {
      t(e.result);
    }, e.onerror = function() {
      n(e.error);
    };
  });
}
function yt(e) {
  var t = new FileReader(), n = _e(t);
  return t.readAsArrayBuffer(e), n;
}
function vt(e) {
  var t = new FileReader(), n = _e(t);
  return t.readAsText(e), n;
}
function bt(e) {
  for (var t = new Uint8Array(e), n = new Array(t.length), s = 0; s < t.length; s++)
    n[s] = String.fromCharCode(t[s]);
  return n.join("");
}
function ye(e) {
  if (e.slice)
    return e.slice(0);
  var t = new Uint8Array(e.byteLength);
  return t.set(new Uint8Array(e)), t.buffer;
}
function me() {
  return this.bodyUsed = !1, this._initBody = function(e) {
    this.bodyUsed = this.bodyUsed, this._bodyInit = e, e ? typeof e == "string" ? this._bodyText = e : R.blob && Blob.prototype.isPrototypeOf(e) ? this._bodyBlob = e : R.formData && FormData.prototype.isPrototypeOf(e) ? this._bodyFormData = e : R.searchParams && URLSearchParams.prototype.isPrototypeOf(e) ? this._bodyText = e.toString() : R.arrayBuffer && R.blob && ht(e) ? (this._bodyArrayBuffer = ye(e.buffer), this._bodyInit = new Blob([this._bodyArrayBuffer])) : R.arrayBuffer && (ArrayBuffer.prototype.isPrototypeOf(e) || dt(e)) ? this._bodyArrayBuffer = ye(e) : this._bodyText = e = Object.prototype.toString.call(e) : this._bodyText = "", this.headers.get("content-type") || (typeof e == "string" ? this.headers.set("content-type", "text/plain;charset=UTF-8") : this._bodyBlob && this._bodyBlob.type ? this.headers.set("content-type", this._bodyBlob.type) : R.searchParams && URLSearchParams.prototype.isPrototypeOf(e) && this.headers.set("content-type", "application/x-www-form-urlencoded;charset=UTF-8"));
  }, R.blob && (this.blob = function() {
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
      return this.blob().then(yt);
  }), this.text = function() {
    var e = H(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return vt(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(bt(this._bodyArrayBuffer));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as text");
    return Promise.resolve(this._bodyText);
  }, R.formData && (this.formData = function() {
    return this.text().then(mt);
  }), this.json = function() {
    return this.text().then(JSON.parse);
  }, this;
}
var wt = ["DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT"];
function _t(e) {
  var t = e.toUpperCase();
  return wt.indexOf(t) > -1 ? t : e;
}
function D(e, t) {
  if (!(this instanceof D))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  t = t || {};
  var n = t.body;
  if (e instanceof D) {
    if (e.bodyUsed)
      throw new TypeError("Already read");
    this.url = e.url, this.credentials = e.credentials, t.headers || (this.headers = new _(e.headers)), this.method = e.method, this.mode = e.mode, this.signal = e.signal, !n && e._bodyInit != null && (n = e._bodyInit, e.bodyUsed = !0);
  } else
    this.url = String(e);
  if (this.credentials = t.credentials || this.credentials || "same-origin", (t.headers || !this.headers) && (this.headers = new _(t.headers)), this.method = _t(t.method || this.method || "GET"), this.mode = t.mode || this.mode || null, this.signal = t.signal || this.signal, this.referrer = null, (this.method === "GET" || this.method === "HEAD") && n)
    throw new TypeError("Body not allowed for GET or HEAD requests");
  if (this._initBody(n), (this.method === "GET" || this.method === "HEAD") && (t.cache === "no-store" || t.cache === "no-cache")) {
    var s = /([?&])_=[^&]*/;
    if (s.test(this.url))
      this.url = this.url.replace(s, "$1_=" + (/* @__PURE__ */ new Date()).getTime());
    else {
      var a = /\?/;
      this.url += (a.test(this.url) ? "&" : "?") + "_=" + (/* @__PURE__ */ new Date()).getTime();
    }
  }
}
D.prototype.clone = function() {
  return new D(this, { body: this._bodyInit });
};
function mt(e) {
  var t = new FormData();
  return e.trim().split("&").forEach(function(n) {
    if (n) {
      var s = n.split("="), a = s.shift().replace(/\+/g, " "), f = s.join("=").replace(/\+/g, " ");
      t.append(decodeURIComponent(a), decodeURIComponent(f));
    }
  }), t;
}
function gt(e) {
  var t = new _(), n = e.replace(/\r?\n[\t ]+/g, " ");
  return n.split(/\r?\n/).forEach(function(s) {
    var a = s.split(":"), f = a.shift().trim();
    if (f) {
      var d = a.join(":").trim();
      t.append(f, d);
    }
  }), t;
}
me.call(D.prototype);
function O(e, t) {
  if (!(this instanceof O))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  t || (t = {}), this.type = "default", this.status = t.status === void 0 ? 200 : t.status, this.ok = this.status >= 200 && this.status < 300, this.statusText = "statusText" in t ? t.statusText : "", this.headers = new _(t.headers), this.url = t.url || "", this._initBody(e);
}
me.call(O.prototype);
O.prototype.clone = function() {
  return new O(this._bodyInit, {
    status: this.status,
    statusText: this.statusText,
    headers: new _(this.headers),
    url: this.url
  });
};
O.error = function() {
  var e = new O(null, { status: 0, statusText: "" });
  return e.type = "error", e;
};
var Tt = [301, 302, 303, 307, 308];
O.redirect = function(e, t) {
  if (Tt.indexOf(t) === -1)
    throw new RangeError("Invalid status code");
  return new O(null, { status: t, headers: { location: e } });
};
var S = P.DOMException;
try {
  new S();
} catch {
  S = function(t, n) {
    this.message = t, this.name = n;
    var s = Error(t);
    this.stack = s.stack;
  }, S.prototype = Object.create(Error.prototype), S.prototype.constructor = S;
}
function ge(e, t) {
  return new Promise(function(n, s) {
    var a = new D(e, t);
    if (a.signal && a.signal.aborted)
      return s(new S("Aborted", "AbortError"));
    var f = new XMLHttpRequest();
    function d() {
      f.abort();
    }
    f.onload = function() {
      var y = {
        status: f.status,
        statusText: f.statusText,
        headers: gt(f.getAllResponseHeaders() || "")
      };
      y.url = "responseURL" in f ? f.responseURL : y.headers.get("X-Request-URL");
      var m = "response" in f ? f.response : f.responseText;
      setTimeout(function() {
        n(new O(m, y));
      }, 0);
    }, f.onerror = function() {
      setTimeout(function() {
        s(new TypeError("Network request failed"));
      }, 0);
    }, f.ontimeout = function() {
      setTimeout(function() {
        s(new TypeError("Network request failed"));
      }, 0);
    }, f.onabort = function() {
      setTimeout(function() {
        s(new S("Aborted", "AbortError"));
      }, 0);
    };
    function A(y) {
      try {
        return y === "" && P.location.href ? P.location.href : y;
      } catch {
        return y;
      }
    }
    f.open(a.method, A(a.url), !0), a.credentials === "include" ? f.withCredentials = !0 : a.credentials === "omit" && (f.withCredentials = !1), "responseType" in f && (R.blob ? f.responseType = "blob" : R.arrayBuffer && a.headers.get("Content-Type") && a.headers.get("Content-Type").indexOf("application/octet-stream") !== -1 && (f.responseType = "arraybuffer")), t && typeof t.headers == "object" && !(t.headers instanceof _) ? Object.getOwnPropertyNames(t.headers).forEach(function(y) {
      f.setRequestHeader(y, z(t.headers[y]));
    }) : a.headers.forEach(function(y, m) {
      f.setRequestHeader(m, y);
    }), a.signal && (a.signal.addEventListener("abort", d), f.onreadystatechange = function() {
      f.readyState === 4 && a.signal.removeEventListener("abort", d);
    }), f.send(typeof a._bodyInit > "u" ? null : a._bodyInit);
  });
}
ge.polyfill = !0;
P.fetch || (P.fetch = ge, P.Headers = _, P.Request = D, P.Response = O);
class Pt {
  constructor(t, n, s) {
    this.response = t, this.status = n, this.headers = s;
  }
}
class ve {
  constructor(t, n, s, a) {
    this.message = t, this.level = n, this.translationKey = s, this.translationValues = a;
  }
}
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(ve || (ve = {}));
var At = Object.getOwnPropertyDescriptor, Et = (e, t, n, s) => {
  for (var a = s > 1 ? void 0 : s ? At(t, n) : t, f = e.length - 1, d; f >= 0; f--)
    (d = e[f]) && (a = d(a) || a);
  return a;
};
let be = class {
  get(e, t) {
    return this.performNetworkCall(e, "get", void 0, t);
  }
  post(e, t, n) {
    return this.performNetworkCall(e, "post", this.getJsonBody(t), this.addJsonHeaders(n));
  }
  put(e, t, n) {
    return this.performNetworkCall(e, "put", this.getJsonBody(t), this.addJsonHeaders(n));
  }
  delete(e, t) {
    return this.performNetworkCall(e, "delete", void 0, t);
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
  performNetworkCall(e, t, n, s) {
    let a;
    return typeof window > "u" ? a = require("node-fetch") : a = window.fetch, a(e, {
      method: t,
      body: n,
      mode: "cors",
      headers: s
    }).then((d) => {
      let A = {};
      return d.headers.forEach((y, m) => {
        A[m.toString().toLowerCase()] = y;
      }), d.text().then((y) => {
        let m = A["content-type"] || "", E;
        m.match("application/json") ? (E = JSON.parse(y), E.metadata && E.metadata) : E = y;
        let j = new Pt(E, d.status, A);
        if (d.status >= 400)
          throw j;
        return j;
      });
    });
  }
};
be = Et([
  W()
], be);
export {
  Ct as ApiServiceBinder,
  Ot as COLLECTION_FORMATS,
  pe as FrontConfigDTO,
  de as StatusDTO,
  J as VueJsOntologyExtensionService,
  V as VueJsService,
  Rt as __fakeExport
};
