var De = typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : {}, Oe = {};
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
var Te;
function _t() {
  if (Te) return Oe;
  Te = 1;
  var e;
  return function(n) {
    (function(t) {
      var o = typeof De == "object" ? De : typeof self == "object" ? self : typeof this == "object" ? this : Function("return this;")(), r = i(n);
      typeof o.Reflect > "u" ? o.Reflect = n : r = i(o.Reflect, r), t(r);
      function i(s, a) {
        return function(p, c) {
          typeof s[p] != "function" && Object.defineProperty(s, p, { configurable: !0, writable: !0, value: c }), a && a(p, c);
        };
      }
    })(function(t) {
      var o = Object.prototype.hasOwnProperty, r = typeof Symbol == "function", i = r && typeof Symbol.toPrimitive < "u" ? Symbol.toPrimitive : "@@toPrimitive", s = r && typeof Symbol.iterator < "u" ? Symbol.iterator : "@@iterator", a = typeof Object.create == "function", p = { __proto__: [] } instanceof Array, c = !a && !p, l = {
        // create an object in dictionary mode (a.k.a. "slow" mode in v8)
        create: a ? function() {
          return K(/* @__PURE__ */ Object.create(null));
        } : p ? function() {
          return K({ __proto__: null });
        } : function() {
          return K({});
        },
        has: c ? function(u, h) {
          return o.call(u, h);
        } : function(u, h) {
          return h in u;
        },
        get: c ? function(u, h) {
          return o.call(u, h) ? u[h] : void 0;
        } : function(u, h) {
          return u[h];
        }
      }, g = Object.getPrototypeOf(Function), P = typeof process == "object" && process.env && process.env.REFLECT_METADATA_USE_MAP_POLYFILL === "true", m = !P && typeof Map == "function" && typeof Map.prototype.entries == "function" ? Map : Ut(), y = !P && typeof Set == "function" && typeof Set.prototype.entries == "function" ? Set : $t(), R = !P && typeof WeakMap == "function" ? WeakMap : jt(), f = new R();
      function C(u, h, b, I) {
        if (O(b)) {
          if (!_e(u))
            throw new TypeError();
          if (!Ae(h))
            throw new TypeError();
          return ft(u, h);
        } else {
          if (!_e(u))
            throw new TypeError();
          if (!G(h))
            throw new TypeError();
          if (!G(I) && !O(I) && !W(I))
            throw new TypeError();
          return W(I) && (I = void 0), b = k(b), lt(u, h, b, I);
        }
      }
      t("decorate", C);
      function $(u, h) {
        function b(I, U) {
          if (!G(I))
            throw new TypeError();
          if (!O(U) && !Pt(U))
            throw new TypeError();
          ve(u, h, I, U);
        }
        return b;
      }
      t("metadata", $);
      function d(u, h, b, I) {
        if (!G(b))
          throw new TypeError();
        return O(I) || (I = k(I)), ve(u, h, b, I);
      }
      t("defineMetadata", d);
      function D(u, h, b) {
        if (!G(h))
          throw new TypeError();
        return O(b) || (b = k(b)), Ie(u, h, b);
      }
      t("hasMetadata", D);
      function S(u, h, b) {
        if (!G(h))
          throw new TypeError();
        return O(b) || (b = k(b)), Z(u, h, b);
      }
      t("hasOwnMetadata", S);
      function L(u, h, b) {
        if (!G(h))
          throw new TypeError();
        return O(b) || (b = k(b)), Se(u, h, b);
      }
      t("getMetadata", L);
      function F(u, h, b) {
        if (!G(h))
          throw new TypeError();
        return O(b) || (b = k(b)), Re(u, h, b);
      }
      t("getOwnMetadata", F);
      function ut(u, h) {
        if (!G(u))
          throw new TypeError();
        return O(h) || (h = k(h)), Ue(u, h);
      }
      t("getMetadataKeys", ut);
      function ht(u, h) {
        if (!G(u))
          throw new TypeError();
        return O(h) || (h = k(h)), $e(u, h);
      }
      t("getOwnMetadataKeys", ht);
      function dt(u, h, b) {
        if (!G(h))
          throw new TypeError();
        O(b) || (b = k(b));
        var I = Y(
          h,
          b,
          /*Create*/
          !1
        );
        if (O(I) || !I.delete(u))
          return !1;
        if (I.size > 0)
          return !0;
        var U = f.get(h);
        return U.delete(b), U.size > 0 || f.delete(h), !0;
      }
      t("deleteMetadata", dt);
      function ft(u, h) {
        for (var b = u.length - 1; b >= 0; --b) {
          var I = u[b], U = I(h);
          if (!O(U) && !W(U)) {
            if (!Ae(U))
              throw new TypeError();
            h = U;
          }
        }
        return h;
      }
      function lt(u, h, b, I) {
        for (var U = u.length - 1; U >= 0; --U) {
          var q = u[U], j = q(h, b, I);
          if (!O(j) && !W(j)) {
            if (!G(j))
              throw new TypeError();
            I = j;
          }
        }
        return I;
      }
      function Y(u, h, b) {
        var I = f.get(u);
        if (O(I)) {
          if (!b)
            return;
          I = new m(), f.set(u, I);
        }
        var U = I.get(h);
        if (O(U)) {
          if (!b)
            return;
          U = new m(), I.set(h, U);
        }
        return U;
      }
      function Ie(u, h, b) {
        var I = Z(u, h, b);
        if (I)
          return !0;
        var U = Q(h);
        return W(U) ? !1 : Ie(u, U, b);
      }
      function Z(u, h, b) {
        var I = Y(
          h,
          b,
          /*Create*/
          !1
        );
        return O(I) ? !1 : Ct(I.has(u));
      }
      function Se(u, h, b) {
        var I = Z(u, h, b);
        if (I)
          return Re(u, h, b);
        var U = Q(h);
        if (!W(U))
          return Se(u, U, b);
      }
      function Re(u, h, b) {
        var I = Y(
          h,
          b,
          /*Create*/
          !1
        );
        if (!O(I))
          return I.get(u);
      }
      function ve(u, h, b, I) {
        var U = Y(
          b,
          I,
          /*Create*/
          !0
        );
        U.set(u, h);
      }
      function Ue(u, h) {
        var b = $e(u, h), I = Q(u);
        if (I === null)
          return b;
        var U = Ue(I, h);
        if (U.length <= 0)
          return b;
        if (b.length <= 0)
          return U;
        for (var q = new y(), j = [], _ = 0, v = b; _ < v.length; _++) {
          var w = v[_], E = q.has(w);
          E || (q.add(w), j.push(w));
        }
        for (var H = 0, Ee = U; H < Ee.length; H++) {
          var w = Ee[H], E = q.has(w);
          E || (q.add(w), j.push(w));
        }
        return j;
      }
      function $e(u, h) {
        var b = [], I = Y(
          u,
          h,
          /*Create*/
          !1
        );
        if (O(I))
          return b;
        for (var U = I.keys(), q = It(U), j = 0; ; ) {
          var _ = Rt(q);
          if (!_)
            return b.length = j, b;
          var v = St(_);
          try {
            b[j] = v;
          } catch (w) {
            try {
              vt(q);
            } finally {
              throw w;
            }
          }
          j++;
        }
      }
      function je(u) {
        if (u === null)
          return 1;
        switch (typeof u) {
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
            return u === null ? 1 : 6;
          default:
            return 6;
        }
      }
      function O(u) {
        return u === void 0;
      }
      function W(u) {
        return u === null;
      }
      function gt(u) {
        return typeof u == "symbol";
      }
      function G(u) {
        return typeof u == "object" ? u !== null : typeof u == "function";
      }
      function mt(u, h) {
        switch (je(u)) {
          case 0:
            return u;
          case 1:
            return u;
          case 2:
            return u;
          case 3:
            return u;
          case 4:
            return u;
          case 5:
            return u;
        }
        var b = "string", I = we(u, i);
        if (I !== void 0) {
          var U = I.call(u, b);
          if (G(U))
            throw new TypeError();
          return U;
        }
        return bt(u);
      }
      function bt(u, h) {
        var b, I, U;
        {
          var q = u.toString;
          if (X(q)) {
            var I = q.call(u);
            if (!G(I))
              return I;
          }
          var b = u.valueOf;
          if (X(b)) {
            var I = b.call(u);
            if (!G(I))
              return I;
          }
        }
        throw new TypeError();
      }
      function Ct(u) {
        return !!u;
      }
      function yt(u) {
        return "" + u;
      }
      function k(u) {
        var h = mt(u);
        return gt(h) ? h : yt(h);
      }
      function _e(u) {
        return Array.isArray ? Array.isArray(u) : u instanceof Object ? u instanceof Array : Object.prototype.toString.call(u) === "[object Array]";
      }
      function X(u) {
        return typeof u == "function";
      }
      function Ae(u) {
        return typeof u == "function";
      }
      function Pt(u) {
        switch (je(u)) {
          case 3:
            return !0;
          case 4:
            return !0;
          default:
            return !1;
        }
      }
      function we(u, h) {
        var b = u[h];
        if (b != null) {
          if (!X(b))
            throw new TypeError();
          return b;
        }
      }
      function It(u) {
        var h = we(u, s);
        if (!X(h))
          throw new TypeError();
        var b = h.call(u);
        if (!G(b))
          throw new TypeError();
        return b;
      }
      function St(u) {
        return u.value;
      }
      function Rt(u) {
        var h = u.next();
        return h.done ? !1 : h;
      }
      function vt(u) {
        var h = u.return;
        h && h.call(u);
      }
      function Q(u) {
        var h = Object.getPrototypeOf(u);
        if (typeof u != "function" || u === g || h !== g)
          return h;
        var b = u.prototype, I = b && Object.getPrototypeOf(b);
        if (I == null || I === Object.prototype)
          return h;
        var U = I.constructor;
        return typeof U != "function" || U === u ? h : U;
      }
      function Ut() {
        var u = {}, h = [], b = (
          /** @class */
          function() {
            function j(_, v, w) {
              this._index = 0, this._keys = _, this._values = v, this._selector = w;
            }
            return j.prototype["@@iterator"] = function() {
              return this;
            }, j.prototype[s] = function() {
              return this;
            }, j.prototype.next = function() {
              var _ = this._index;
              if (_ >= 0 && _ < this._keys.length) {
                var v = this._selector(this._keys[_], this._values[_]);
                return _ + 1 >= this._keys.length ? (this._index = -1, this._keys = h, this._values = h) : this._index++, { value: v, done: !1 };
              }
              return { value: void 0, done: !0 };
            }, j.prototype.throw = function(_) {
              throw this._index >= 0 && (this._index = -1, this._keys = h, this._values = h), _;
            }, j.prototype.return = function(_) {
              return this._index >= 0 && (this._index = -1, this._keys = h, this._values = h), { value: _, done: !0 };
            }, j;
          }()
        );
        return (
          /** @class */
          function() {
            function j() {
              this._keys = [], this._values = [], this._cacheKey = u, this._cacheIndex = -2;
            }
            return Object.defineProperty(j.prototype, "size", {
              get: function() {
                return this._keys.length;
              },
              enumerable: !0,
              configurable: !0
            }), j.prototype.has = function(_) {
              return this._find(
                _,
                /*insert*/
                !1
              ) >= 0;
            }, j.prototype.get = function(_) {
              var v = this._find(
                _,
                /*insert*/
                !1
              );
              return v >= 0 ? this._values[v] : void 0;
            }, j.prototype.set = function(_, v) {
              var w = this._find(
                _,
                /*insert*/
                !0
              );
              return this._values[w] = v, this;
            }, j.prototype.delete = function(_) {
              var v = this._find(
                _,
                /*insert*/
                !1
              );
              if (v >= 0) {
                for (var w = this._keys.length, E = v + 1; E < w; E++)
                  this._keys[E - 1] = this._keys[E], this._values[E - 1] = this._values[E];
                return this._keys.length--, this._values.length--, _ === this._cacheKey && (this._cacheKey = u, this._cacheIndex = -2), !0;
              }
              return !1;
            }, j.prototype.clear = function() {
              this._keys.length = 0, this._values.length = 0, this._cacheKey = u, this._cacheIndex = -2;
            }, j.prototype.keys = function() {
              return new b(this._keys, this._values, I);
            }, j.prototype.values = function() {
              return new b(this._keys, this._values, U);
            }, j.prototype.entries = function() {
              return new b(this._keys, this._values, q);
            }, j.prototype["@@iterator"] = function() {
              return this.entries();
            }, j.prototype[s] = function() {
              return this.entries();
            }, j.prototype._find = function(_, v) {
              return this._cacheKey !== _ && (this._cacheIndex = this._keys.indexOf(this._cacheKey = _)), this._cacheIndex < 0 && v && (this._cacheIndex = this._keys.length, this._keys.push(_), this._values.push(void 0)), this._cacheIndex;
            }, j;
          }()
        );
        function I(j, _) {
          return j;
        }
        function U(j, _) {
          return _;
        }
        function q(j, _) {
          return [j, _];
        }
      }
      function $t() {
        return (
          /** @class */
          function() {
            function u() {
              this._map = new m();
            }
            return Object.defineProperty(u.prototype, "size", {
              get: function() {
                return this._map.size;
              },
              enumerable: !0,
              configurable: !0
            }), u.prototype.has = function(h) {
              return this._map.has(h);
            }, u.prototype.add = function(h) {
              return this._map.set(h, h), this;
            }, u.prototype.delete = function(h) {
              return this._map.delete(h);
            }, u.prototype.clear = function() {
              this._map.clear();
            }, u.prototype.keys = function() {
              return this._map.keys();
            }, u.prototype.values = function() {
              return this._map.values();
            }, u.prototype.entries = function() {
              return this._map.entries();
            }, u.prototype["@@iterator"] = function() {
              return this.keys();
            }, u.prototype[s] = function() {
              return this.keys();
            }, u;
          }()
        );
      }
      function jt() {
        var u = 16, h = l.create(), b = I();
        return (
          /** @class */
          function() {
            function v() {
              this._key = I();
            }
            return v.prototype.has = function(w) {
              var E = U(
                w,
                /*create*/
                !1
              );
              return E !== void 0 ? l.has(E, this._key) : !1;
            }, v.prototype.get = function(w) {
              var E = U(
                w,
                /*create*/
                !1
              );
              return E !== void 0 ? l.get(E, this._key) : void 0;
            }, v.prototype.set = function(w, E) {
              var H = U(
                w,
                /*create*/
                !0
              );
              return H[this._key] = E, this;
            }, v.prototype.delete = function(w) {
              var E = U(
                w,
                /*create*/
                !1
              );
              return E !== void 0 ? delete E[this._key] : !1;
            }, v.prototype.clear = function() {
              this._key = I();
            }, v;
          }()
        );
        function I() {
          var v;
          do
            v = "@@WeakMap@@" + _();
          while (l.has(h, v));
          return h[v] = !0, v;
        }
        function U(v, w) {
          if (!o.call(v, b)) {
            if (!w)
              return;
            Object.defineProperty(v, b, { value: l.create() });
          }
          return v[b];
        }
        function q(v, w) {
          for (var E = 0; E < w; ++E)
            v[E] = Math.random() * 255 | 0;
          return v;
        }
        function j(v) {
          return typeof Uint8Array == "function" ? typeof crypto < "u" ? crypto.getRandomValues(new Uint8Array(v)) : typeof msCrypto < "u" ? msCrypto.getRandomValues(new Uint8Array(v)) : q(new Uint8Array(v), v) : q(new Array(v), v);
        }
        function _() {
          var v = j(u);
          v[6] = v[6] & 79 | 64, v[8] = v[8] & 191 | 128;
          for (var w = "", E = 0; E < u; ++E) {
            var H = v[E];
            (E === 4 || E === 6 || E === 8) && (w += "-"), H < 16 && (w += "0"), w += H.toString(16).toLowerCase();
          }
          return w;
        }
      }
      function K(u) {
        return u.__ = void 0, delete u.__, u;
      }
    });
  }(e || (e = {})), Oe;
}
_t();
var At = "named", wt = "inject", Et = "inversify:tagged", Dt = "inversify:tagged_props", xe = "inversify:paramtypes", Ot = "design:paramtypes", Tt = "Cannot apply @injectable decorator multiple times.", xt = "Metadata key was used more than once in a parameter:", qt = function(e) {
  return "@inject called with undefined this could mean that the class " + e + " has a circular dependency problem. You can use a LazyServiceIdentifer to  overcome this limitation.";
}, Bt = "The @inject @multiInject @tagged and @named decorators must be applied to the parameters of a class constructor or a class property.", Gt = function() {
  function e(n, t) {
    this.key = n, this.value = t;
  }
  return e.prototype.toString = function() {
    return this.key === At ? "named: " + this.value.toString() + " " : "tagged: { key:" + this.key.toString() + ", value: " + this.value + " }";
  }, e;
}();
function Mt(e, n, t, o) {
  var r = Et;
  st(r, e, n, o, t);
}
function Ft(e, n, t) {
  var o = Dt;
  st(o, e.constructor, n, t);
}
function st(e, n, t, o, r) {
  var i = {}, s = typeof r == "number", a = r !== void 0 && s ? r.toString() : t;
  if (s && t !== void 0)
    throw new Error(Bt);
  Reflect.hasOwnMetadata(e, n) && (i = Reflect.getMetadata(e, n));
  var p = i[a];
  if (!Array.isArray(p))
    p = [];
  else
    for (var c = 0, l = p; c < l.length; c++) {
      var g = l[c];
      if (g.key === o.key)
        throw new Error(xt + " " + g.key.toString());
    }
  p.push(o), i[a] = p, Reflect.defineMetadata(e, i, n);
}
function A(e) {
  return function(n, t, o) {
    if (e === void 0)
      throw new Error(qt(n.name));
    var r = new Gt(wt, e);
    typeof o == "number" ? Mt(n, t, o, r) : Ft(n, t, r);
  };
}
function x() {
  return function(e) {
    if (Reflect.hasOwnMetadata(xe, e))
      throw new Error(Tt);
    var n = Reflect.getMetadata(Ot, e) || [];
    return Reflect.defineMetadata(xe, n, e), e;
  };
}
var Lt = Object.getOwnPropertyDescriptor, Vt = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Lt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, qe = (e, n) => (t, o) => n(t, o, e);
let te = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count annotations
   * 
   * @param Authorization Authentication token
   * @param target Target URI
   * @param Accept_Language Request accepted language
   
   */
  countAnnotationsPath() {
    return "/core/annotations/count";
  }
  countAnnotations(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("target=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/annotations/count${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Create an annotation
   * 
   * @param Authorization Authentication token
   * @param body 
   * @param Accept_Language Request accepted language
   
   */
  createAnnotationPath() {
    return "/core/annotations";
  }
  createAnnotation(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/annotations`, e, t);
  }
  /**
   * Delete an annotation
   * 
   * @param uri Annotation URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteAnnotationPath() {
    return "/core/annotations/${encodeURIComponent(String(uri))}";
  }
  deleteAnnotation(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/annotations/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get an annotation
   * 
   * @param uri Event URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAnnotationPath() {
    return "/core/annotations/${encodeURIComponent(String(uri))}";
  }
  getAnnotation(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/annotations/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Search annotations
   * 
   * @param Authorization Authentication token
   * @param description Description (regex)
   * @param target Target URI
   * @param motivation Motivation URI
   * @param author Author URI
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchAnnotationsPath() {
    return "/core/annotations";
  }
  searchAnnotations(e, n, t, o, r, i, s, a = "body", p = {}) {
    let c = [];
    return e !== void 0 && c.push("description=" + encodeURIComponent(String(e))), n !== void 0 && c.push("target=" + encodeURIComponent(String(n))), t !== void 0 && c.push("motivation=" + encodeURIComponent(String(t))), o !== void 0 && c.push("author=" + encodeURIComponent(String(o))), r && r.forEach((g) => {
      c.push("order_by=" + encodeURIComponent(String(g)));
    }), i !== void 0 && c.push("page=" + encodeURIComponent(String(i))), s !== void 0 && c.push("page_size=" + encodeURIComponent(String(s))), p.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/annotations${c.length > 0 ? "?" + c.join("&") : ""}`, p);
  }
  /**
   * Search motivations
   * 
   * @param Authorization Authentication token
   * @param name Motivation name regex pattern
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchMotivationsPath() {
    return "/core/annotations/motivations";
  }
  searchMotivations(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e !== void 0 && s.push("name=" + encodeURIComponent(String(e))), n && n.forEach((p) => {
      s.push("order_by=" + encodeURIComponent(String(p)));
    }), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/annotations/motivations${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Update an annotation
   * 
   * @param Authorization Authentication token
   * @param body Annotation description
   * @param Accept_Language Request accepted language
   
   */
  updateAnnotationPath() {
    return "/core/annotations";
  }
  updateAnnotation(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/annotations`, e, t);
  }
};
te = Vt([
  x(),
  qe(0, A("IApiHttpClient")),
  qe(1, A("IAPIConfiguration"))
], te);
var kt = Object.getOwnPropertyDescriptor, Ht = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? kt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Be = (e, n) => (t, o) => n(t, o, e);
let ne = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add an area
   * 
   * @param Authorization Authentication token
   * @param body Area description
   * @param Accept_Language Request accepted language
   
   */
  createAreaPath() {
    return "/core/area";
  }
  createArea(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/area`, e, t);
  }
  /**
   * Delete an area
   * 
   * @param uri Area URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteAreaPath() {
    return "/core/area/${encodeURIComponent(String(uri))}";
  }
  deleteArea(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/area/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Export a given list of areas URIs to shapefile
   * 
   * @param Authorization Authentication token
   * @param body Areas
   * @param selected_props properties selected
   * @param format export format (shp/geojson)
   * @param pageSize Page size limited to 10,000 objects
   * @param Accept_Language Request accepted language
   
   */
  exportGeospatialPath() {
    return "/core/area/export_geospatial";
  }
  exportGeospatial(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e && e.forEach((p) => {
      s.push("selected_props=" + encodeURIComponent(String(p)));
    }), n !== void 0 && s.push("format=" + encodeURIComponent(String(n))), t !== void 0 && s.push("pageSize=" + encodeURIComponent(String(t))), i.Accept = "application/octet-stream", i["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/area/export_geospatial${s.length > 0 ? "?" + s.join("&") : ""}`, o, i);
  }
  /**
   * Get an area
   * 
   * @param uri area URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getByURIPath() {
    return "/core/area/${encodeURIComponent(String(uri))}";
  }
  getByURI(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/area/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get area whose geometry corresponds to the Intersections
   * 
   * @param body geometry GeoJSON
   * @param Authorization Authentication token
   * @param start Start date : match temporal area after the given start date
   * @param end End date : match temporal area before the given end date
   * @param Accept_Language Request accepted language
   
   */
  searchIntersectsPath() {
    return "/core/area/intersects";
  }
  searchIntersects(e, n, t, o = "body", r = {}) {
    let i = [];
    if (n !== void 0 && i.push("start=" + encodeURIComponent(String(n))), t !== void 0 && i.push("end=" + encodeURIComponent(String(t))), r.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling searchIntersects.");
    return r["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/area/intersects${i.length > 0 ? "?" + i.join("&") : ""}`, e, r);
  }
  /**
   * Update an area
   * 
   * @param body Area description
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  updateAreaPath() {
    return "/core/area";
  }
  updateArea(e, n = "body", t = {}) {
    if (t.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling updateArea.");
    return t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/area`, e, t);
  }
};
ne = Ht([
  x(),
  Be(0, A("IApiHttpClient")),
  Be(1, A("IAPIConfiguration"))
], ne);
var Nt = Object.getOwnPropertyDescriptor, zt = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Nt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Ge = (e, n) => (t, o) => n(t, o, e);
let oe = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add data
   * 
   * @param Authorization Authentication token
   * @param body Data description
   * @param Accept_Language Request accepted language
   
   */
  addListDataPath() {
    return "/core/data";
  }
  addListData(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/data`, e, t);
  }
  /**
   * Count data
   * 
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiment uris
   * @param variables Search by variables uris
   * @param devices Search by devices uris
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenances Search by provenances
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param group_of_germplasm Group filter
   * @param germplasmUris Germplasm uris, can be an empty array but can&#39;t be null
   * @param count_limit Count limit. Specify the maximum number of data to count. Set to 0 for no limit
   * @param targets Targets uris, can be an empty array but can&#39;t be null
   * @param Accept_Language Request accepted language
   
   */
  countDataPath() {
    return "/core/data/count";
  }
  countData(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R = "body", f = {}) {
    let C = [];
    return e !== void 0 && C.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && C.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && C.push("timezone=" + encodeURIComponent(String(t))), o && o.forEach((d) => {
      C.push("experiments=" + encodeURIComponent(String(d)));
    }), r && r.forEach((d) => {
      C.push("variables=" + encodeURIComponent(String(d)));
    }), i && i.forEach((d) => {
      C.push("devices=" + encodeURIComponent(String(d)));
    }), s !== void 0 && C.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && C.push("max_confidence=" + encodeURIComponent(String(a))), p && p.forEach((d) => {
      C.push("provenances=" + encodeURIComponent(String(d)));
    }), c !== void 0 && C.push("metadata=" + encodeURIComponent(String(c))), l && l.forEach((d) => {
      C.push("operators=" + encodeURIComponent(String(d)));
    }), g !== void 0 && C.push("group_of_germplasm=" + encodeURIComponent(String(g))), P && P.forEach((d) => {
      C.push("germplasmUris=" + encodeURIComponent(String(d)));
    }), m !== void 0 && C.push("count_limit=" + encodeURIComponent(String(m))), f.Accept = "application/json", f["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/data/count${C.length > 0 ? "?" + C.join("&") : ""}`, y, f);
  }
  /**
   * Count datafiles
   * 
   * @param Authorization Authentication token
   * @param target Target URI
   * @param device Device URI
   * @param Accept_Language Request accepted language
   
   */
  countDatafilesPath() {
    return "/core/datafiles/count";
  }
  countDatafiles(e, n, t = "body", o = {}) {
    let r = [];
    return e && e.forEach((s) => {
      r.push("target=" + encodeURIComponent(String(s)));
    }), n && n.forEach((s) => {
      r.push("device=" + encodeURIComponent(String(s)));
    }), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/datafiles/count${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Add a provenance
   * 
   * @param Authorization Authentication token
   * @param body Provenance description
   * @param Accept_Language Request accepted language
   
   */
  createProvenancePath() {
    return "/core/provenances";
  }
  createProvenance(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/provenances`, e, t);
  }
  /**
   * Delete data
   * 
   * @param uri Data URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteDataPath() {
    return "/core/data/${encodeURIComponent(String(uri))}";
  }
  deleteData(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/data/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete data on criteria
   * 
   * @param Authorization Authentication token
   * @param experiment Search by experiment uri
   * @param target Search by target uri
   * @param variable Search by variable uri
   * @param provenance Search by provenance uri
   * @param Accept_Language Request accepted language
   
   */
  deleteDataOnSearchPath() {
    return "/core/data";
  }
  deleteDataOnSearch(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e !== void 0 && s.push("experiment=" + encodeURIComponent(String(e))), n !== void 0 && s.push("target=" + encodeURIComponent(String(n))), t !== void 0 && s.push("variable=" + encodeURIComponent(String(t))), o !== void 0 && s.push("provenance=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/data${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Delete a provenance that doesn&#39;t describe data
   * 
   * @param uri Provenance URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteProvenancePath() {
    return "/core/provenances/${encodeURIComponent(String(uri))}";
  }
  deleteProvenance(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/provenances/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Export data
   * 
   * @param Authorization Authentication token
   * @param body CSV export configuration
   * @param Accept_Language Request accepted language
   
   */
  exportDataPath() {
    return "/core/data/export";
  }
  exportData(e, n = "body", t = {}) {
    return t.Accept = "text/plain", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/data/export`, e, t);
  }
  /**
   * Export data
   * 
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiment uris
   * @param targets Search by targets
   * @param variables Search by variables
   * @param devices Search by devices uris
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenances Search by provenances
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param mode Format wide or long
   * @param with_raw_data Export also raw_data
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  exportData1Path() {
    return "/core/data/export";
  }
  exportData1(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R, f, C = "body", $ = {}) {
    let d = [];
    return e !== void 0 && d.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && d.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && d.push("timezone=" + encodeURIComponent(String(t))), o && o.forEach((S) => {
      d.push("experiments=" + encodeURIComponent(String(S)));
    }), r && r.forEach((S) => {
      d.push("targets=" + encodeURIComponent(String(S)));
    }), i && i.forEach((S) => {
      d.push("variables=" + encodeURIComponent(String(S)));
    }), s && s.forEach((S) => {
      d.push("devices=" + encodeURIComponent(String(S)));
    }), a !== void 0 && d.push("min_confidence=" + encodeURIComponent(String(a))), p !== void 0 && d.push("max_confidence=" + encodeURIComponent(String(p))), c && c.forEach((S) => {
      d.push("provenances=" + encodeURIComponent(String(S)));
    }), l !== void 0 && d.push("metadata=" + encodeURIComponent(String(l))), g && g.forEach((S) => {
      d.push("operators=" + encodeURIComponent(String(S)));
    }), P !== void 0 && d.push("mode=" + encodeURIComponent(String(P))), m !== void 0 && d.push("with_raw_data=" + encodeURIComponent(String(m))), y && y.forEach((S) => {
      d.push("order_by=" + encodeURIComponent(String(S)));
    }), R !== void 0 && d.push("page=" + encodeURIComponent(String(R))), f !== void 0 && d.push("page_size=" + encodeURIComponent(String(f))), $.Accept = "text/plain", this.httpClient.get(`${this.basePath}/core/data/export${d.length > 0 ? "?" + d.join("&") : ""}`, $);
  }
  /**
   * Get data
   * 
   * @param uri Data URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDataPath() {
    return "/core/data/${encodeURIComponent(String(uri))}";
  }
  getData(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/data/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a data file
   * 
   * @param uri Search by fileUri
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDataFilePath() {
    return "/core/datafiles/${encodeURIComponent(String(uri))}";
  }
  getDataFile(e, n = "body", t = {}) {
    return t.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/core/datafiles/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a data file description
   * 
   * @param uri Search by fileUri
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDataFileDescriptionPath() {
    return "/core/datafiles/${encodeURIComponent(String(uri))}/description";
  }
  getDataFileDescription(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/datafiles/${encodeURIComponent(String(e))}/description`, t);
  }
  /**
   * Search data files
   * 
   * @param Authorization Authentication token
   * @param rdf_type Search by rdf type uri
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiments
   * @param targets Search by targets uris list
   * @param devices Search by devices uris
   * @param provenances Search by provenance uris list
   * @param metadata Search by metadata
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getDataFileDescriptionsBySearchPath() {
    return "/core/datafiles";
  }
  getDataFileDescriptionsBySearch(e, n, t, o, r, i, s, a, p, c, l, g, P = "body", m = {}) {
    let y = [];
    return e !== void 0 && y.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && y.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && y.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && y.push("timezone=" + encodeURIComponent(String(o))), r && r.forEach((f) => {
      y.push("experiments=" + encodeURIComponent(String(f)));
    }), i && i.forEach((f) => {
      y.push("targets=" + encodeURIComponent(String(f)));
    }), s && s.forEach((f) => {
      y.push("devices=" + encodeURIComponent(String(f)));
    }), a && a.forEach((f) => {
      y.push("provenances=" + encodeURIComponent(String(f)));
    }), p !== void 0 && y.push("metadata=" + encodeURIComponent(String(p))), c && c.forEach((f) => {
      y.push("order_by=" + encodeURIComponent(String(f)));
    }), l !== void 0 && y.push("page=" + encodeURIComponent(String(l))), g !== void 0 && y.push("page_size=" + encodeURIComponent(String(g))), m.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/datafiles${y.length > 0 ? "?" + y.join("&") : ""}`, m);
  }
  /**
   * Search data files for a large list of targets 
   * 
   * @param Authorization Authentication token
   * @param rdf_type Search by rdf type uri
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiments
   * @param devices Search by devices uris
   * @param provenances Search by provenance uris list
   * @param metadata Search by metadata
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param targets Targets uris, can be an empty array but can&#39;t be null
   * @param Accept_Language Request accepted language
   
   */
  getDataFileDescriptionsByTargetsPath() {
    return "/core/datafiles/by_targets";
  }
  getDataFileDescriptionsByTargets(e, n, t, o, r, i, s, a, p, c, l, g, P = "body", m = {}) {
    let y = [];
    return e !== void 0 && y.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && y.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && y.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && y.push("timezone=" + encodeURIComponent(String(o))), r && r.forEach((f) => {
      y.push("experiments=" + encodeURIComponent(String(f)));
    }), i && i.forEach((f) => {
      y.push("devices=" + encodeURIComponent(String(f)));
    }), s && s.forEach((f) => {
      y.push("provenances=" + encodeURIComponent(String(f)));
    }), a !== void 0 && y.push("metadata=" + encodeURIComponent(String(a))), p && p.forEach((f) => {
      y.push("order_by=" + encodeURIComponent(String(f)));
    }), c !== void 0 && y.push("page=" + encodeURIComponent(String(c))), l !== void 0 && y.push("page_size=" + encodeURIComponent(String(l))), m.Accept = "application/json", m["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/datafiles/by_targets${y.length > 0 ? "?" + y.join("&") : ""}`, g, m);
  }
  /**
   * Search data for a large list of targets
   * Deprecated. Use searchDataListByTargets (/search) service which is more optimized
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiment uris
   * @param targets Targets uris, can be an empty array but can&#39;t be null
   * @param variables Search by variables uris
   * @param devices Search by devices uris
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenances Search by provenances
   * @param metadata Search by metadata
   * @param group_of_germplasm Group filter
   * @param operators Search by operators
   * @param germplasmUris Targets uris, can be an empty array but can&#39;t be null
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getDataListByTargetsPath() {
    return "/core/data/by_targets";
  }
  getDataListByTargets(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R, f, C = "body", $ = {}) {
    let d = [];
    return e !== void 0 && d.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && d.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && d.push("timezone=" + encodeURIComponent(String(t))), o && o.forEach((S) => {
      d.push("experiments=" + encodeURIComponent(String(S)));
    }), r && r.forEach((S) => {
      d.push("variables=" + encodeURIComponent(String(S)));
    }), i && i.forEach((S) => {
      d.push("devices=" + encodeURIComponent(String(S)));
    }), s !== void 0 && d.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && d.push("max_confidence=" + encodeURIComponent(String(a))), p && p.forEach((S) => {
      d.push("provenances=" + encodeURIComponent(String(S)));
    }), c !== void 0 && d.push("metadata=" + encodeURIComponent(String(c))), l !== void 0 && d.push("group_of_germplasm=" + encodeURIComponent(String(l))), g && g.forEach((S) => {
      d.push("operators=" + encodeURIComponent(String(S)));
    }), P && P.forEach((S) => {
      d.push("germplasmUris=" + encodeURIComponent(String(S)));
    }), m && m.forEach((S) => {
      d.push("order_by=" + encodeURIComponent(String(S)));
    }), y !== void 0 && d.push("page=" + encodeURIComponent(String(y))), R !== void 0 && d.push("page_size=" + encodeURIComponent(String(R))), $.Accept = "application/json", $["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/data/by_targets${d.length > 0 ? "?" + d.join("&") : ""}`, f, $);
  }
  /**
   * Get all data series associated with a facility
   * 
   * @param variable variable URI
   * @param target target URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param calculated_only Retreive calculated series only
   * @param Accept_Language Request accepted language
   
   */
  getDataSeriesByFacilityPath() {
    return "/core/data/data_serie/facility";
  }
  getDataSeriesByFacility(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    if (!e)
      throw new Error("Required parameter variable was null or undefined when calling getDataSeriesByFacility.");
    if (e !== void 0 && a.push("variable=" + encodeURIComponent(String(e))), !n)
      throw new Error("Required parameter target was null or undefined when calling getDataSeriesByFacility.");
    return n !== void 0 && a.push("target=" + encodeURIComponent(String(n))), t !== void 0 && a.push("start_date=" + encodeURIComponent(String(t))), o !== void 0 && a.push("end_date=" + encodeURIComponent(String(o))), r !== void 0 && a.push("calculated_only=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/data/data_serie/facility${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search provenances linked to datafiles
   * 
   * @param Authorization Authentication token
   * @param experiments Search by experiment uris
   * @param targets Search by targets uris
   * @param devices Search by devices uris
   * @param Accept_Language Request accepted language
   
   */
  getDatafilesProvenancesPath() {
    return "/core/datafiles/provenances";
  }
  getDatafilesProvenances(e, n, t, o = "body", r = {}) {
    let i = [];
    return e && e.forEach((a) => {
      i.push("experiments=" + encodeURIComponent(String(a)));
    }), n && n.forEach((a) => {
      i.push("targets=" + encodeURIComponent(String(a)));
    }), t && t.forEach((a) => {
      i.push("devices=" + encodeURIComponent(String(a)));
    }), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/datafiles/provenances${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Search provenances linked to datafiles for a large list of targets
   * 
   * @param Authorization Authentication token
   * @param experiments Search by experiment uris
   * @param devices Search by devices uris
   * @param body Search by targets uris
   * @param Accept_Language Request accepted language
   
   */
  getDatafilesProvenancesByTargetsPath() {
    return "/core/datafiles/provenances/by_targets";
  }
  getDatafilesProvenancesByTargets(e, n, t, o = "body", r = {}) {
    let i = [];
    return e && e.forEach((a) => {
      i.push("experiments=" + encodeURIComponent(String(a)));
    }), n && n.forEach((a) => {
      i.push("devices=" + encodeURIComponent(String(a)));
    }), r.Accept = "application/json", r["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/datafiles/provenances/by_targets${i.length > 0 ? "?" + i.join("&") : ""}`, t, r);
  }
  /**
   * Get mathematical operators
   * 
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getMathematicalOperatorsPath() {
    return "/core/data/mathematicalOperators";
  }
  getMathematicalOperators(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/data/mathematicalOperators`, n);
  }
  /**
   * Get a picture thumbnail
   * 
   * @param uri Search by fileUri
   * @param Authorization Authentication token
   * @param scaled_width Thumbnail width
   * @param scaled_height Thumbnail height
   * @param Accept_Language Request accepted language
   
   */
  getPicturesThumbnailsPath() {
    return "/core/datafiles/${encodeURIComponent(String(uri))}/thumbnail";
  }
  getPicturesThumbnails(e, n, t, o = "body", r = {}) {
    let i = [];
    return n !== void 0 && i.push("scaled_width=" + encodeURIComponent(String(n))), t !== void 0 && i.push("scaled_height=" + encodeURIComponent(String(t))), r.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/core/datafiles/${encodeURIComponent(String(e))}/thumbnail${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Get a provenance
   * 
   * @param uri Provenance URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getProvenancePath() {
    return "/core/provenances/${encodeURIComponent(String(uri))}";
  }
  getProvenance(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/provenances/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a list of provenances by their URIs
   * 
   * @param uris Provenances URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getProvenancesByURIsPath() {
    return "/core/provenances/by_uris";
  }
  getProvenancesByURIs(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getProvenancesByURIs.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/provenances/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search provenances linked to data
   * 
   * @param Authorization Authentication token
   * @param experiments Search by experiment uris
   * @param targets Search by targets uris
   * @param variables Search by variables uris
   * @param devices Search by devices uris
   * @param Accept_Language Request accepted language
   
   */
  getUsedProvenancesPath() {
    return "/core/data/provenances";
  }
  getUsedProvenances(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e && e.forEach((p) => {
      s.push("experiments=" + encodeURIComponent(String(p)));
    }), n && n.forEach((p) => {
      s.push("targets=" + encodeURIComponent(String(p)));
    }), t && t.forEach((p) => {
      s.push("variables=" + encodeURIComponent(String(p)));
    }), o && o.forEach((p) => {
      s.push("devices=" + encodeURIComponent(String(p)));
    }), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/data/provenances${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Search provenances linked to data for a large list of targets
   * 
   * @param Authorization Authentication token
   * @param experiments Search by experiment uris
   * @param variables Search by variables uris
   * @param devices Search by devices uris
   * @param body Targets uris
   * @param Accept_Language Request accepted language
   
   */
  getUsedProvenancesByTargetsPath() {
    return "/core/data/provenances/by_targets";
  }
  getUsedProvenancesByTargets(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e && e.forEach((p) => {
      s.push("experiments=" + encodeURIComponent(String(p)));
    }), n && n.forEach((p) => {
      s.push("variables=" + encodeURIComponent(String(p)));
    }), t && t.forEach((p) => {
      s.push("devices=" + encodeURIComponent(String(p)));
    }), i.Accept = "application/json", i["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/data/provenances/by_targets${s.length > 0 ? "?" + s.join("&") : ""}`, o, i);
  }
  /**
   * Get variables linked to data
   * 
   * @param Authorization Authentication token
   * @param experiments Search by experiment uris
   * @param targets Search by targets uris
   * @param provenances Search by provenance uris
   * @param devices Search by device uris
   * @param Accept_Language Request accepted language
   
   */
  getUsedVariablesPath() {
    return "/core/data/variables";
  }
  getUsedVariables(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e && e.forEach((p) => {
      s.push("experiments=" + encodeURIComponent(String(p)));
    }), n && n.forEach((p) => {
      s.push("targets=" + encodeURIComponent(String(p)));
    }), t && t.forEach((p) => {
      s.push("provenances=" + encodeURIComponent(String(p)));
    }), o && o.forEach((p) => {
      s.push("devices=" + encodeURIComponent(String(p)));
    }), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/data/variables${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Describe datafiles and give their relative paths in the configured storage system. In the case of already stored datafiles.
   * 
   * @param body Metadata of the file
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  postDataFilePathsPath() {
    return "/core/datafiles/description";
  }
  postDataFilePaths(e, n = "body", t = {}) {
    if (t.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling postDataFilePaths.");
    return t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/datafiles/description`, e, t);
  }
  /**
   * Search data
   * Deprecated. Use searchDataListByTargets (/search) service which is more optimized
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiment uris
   * @param targets Search by targets uris
   * @param variables Search by variables uris
   * @param devices Search by devices uris
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenances Search by provenances
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchDataListPath() {
    return "/core/data";
  }
  searchDataList(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R = "body", f = {}) {
    let C = [];
    return e !== void 0 && C.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && C.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && C.push("timezone=" + encodeURIComponent(String(t))), o && o.forEach((d) => {
      C.push("experiments=" + encodeURIComponent(String(d)));
    }), r && r.forEach((d) => {
      C.push("targets=" + encodeURIComponent(String(d)));
    }), i && i.forEach((d) => {
      C.push("variables=" + encodeURIComponent(String(d)));
    }), s && s.forEach((d) => {
      C.push("devices=" + encodeURIComponent(String(d)));
    }), a !== void 0 && C.push("min_confidence=" + encodeURIComponent(String(a))), p !== void 0 && C.push("max_confidence=" + encodeURIComponent(String(p))), c && c.forEach((d) => {
      C.push("provenances=" + encodeURIComponent(String(d)));
    }), l !== void 0 && C.push("metadata=" + encodeURIComponent(String(l))), g && g.forEach((d) => {
      C.push("operators=" + encodeURIComponent(String(d)));
    }), P && P.forEach((d) => {
      C.push("order_by=" + encodeURIComponent(String(d)));
    }), m !== void 0 && C.push("page=" + encodeURIComponent(String(m))), y !== void 0 && C.push("page_size=" + encodeURIComponent(String(y))), f.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/data${C.length > 0 ? "?" + C.join("&") : ""}`, f);
  }
  /**
   * Search data for a large list of targets
   * Optimized search. The total count of element is not returned. Use countData (/count) service in order to get exact count of element
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiments Search by experiment uris
   * @param targets Targets uris, can be an empty array but can&#39;t be null
   * @param variables Search by variables uris
   * @param devices Search by devices uris
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenances Search by provenances
   * @param metadata Search by metadata
   * @param group_of_germplasm Group filter
   * @param operators Search by operators
   * @param germplasmUris Targets uris, can be an empty array but can&#39;t be null
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchDataListByTargetsPath() {
    return "/core/data/search";
  }
  searchDataListByTargets(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R, f, C = "body", $ = {}) {
    let d = [];
    return e !== void 0 && d.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && d.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && d.push("timezone=" + encodeURIComponent(String(t))), o && o.forEach((S) => {
      d.push("experiments=" + encodeURIComponent(String(S)));
    }), r && r.forEach((S) => {
      d.push("variables=" + encodeURIComponent(String(S)));
    }), i && i.forEach((S) => {
      d.push("devices=" + encodeURIComponent(String(S)));
    }), s !== void 0 && d.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && d.push("max_confidence=" + encodeURIComponent(String(a))), p && p.forEach((S) => {
      d.push("provenances=" + encodeURIComponent(String(S)));
    }), c !== void 0 && d.push("metadata=" + encodeURIComponent(String(c))), l !== void 0 && d.push("group_of_germplasm=" + encodeURIComponent(String(l))), g && g.forEach((S) => {
      d.push("operators=" + encodeURIComponent(String(S)));
    }), P && P.forEach((S) => {
      d.push("germplasmUris=" + encodeURIComponent(String(S)));
    }), m && m.forEach((S) => {
      d.push("order_by=" + encodeURIComponent(String(S)));
    }), y !== void 0 && d.push("page=" + encodeURIComponent(String(y))), R !== void 0 && d.push("page_size=" + encodeURIComponent(String(R))), $.Accept = "application/json", $["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/data/search${d.length > 0 ? "?" + d.join("&") : ""}`, f, $);
  }
  /**
   * Get provenances
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering by name
   * @param description Search by description
   * @param activity Search by activity URI
   * @param activity_type Search by activity type
   * @param agent Search by agent URI
   * @param agent_type Search by agent type
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchProvenancePath() {
    return "/core/provenances";
  }
  searchProvenance(e, n, t, o, r, i, s, a, p, c = "body", l = {}) {
    let g = [];
    return e !== void 0 && g.push("name=" + encodeURIComponent(String(e))), n !== void 0 && g.push("description=" + encodeURIComponent(String(n))), t !== void 0 && g.push("activity=" + encodeURIComponent(String(t))), o !== void 0 && g.push("activity_type=" + encodeURIComponent(String(o))), r !== void 0 && g.push("agent=" + encodeURIComponent(String(r))), i !== void 0 && g.push("agent_type=" + encodeURIComponent(String(i))), s && s.forEach((m) => {
      g.push("order_by=" + encodeURIComponent(String(m)));
    }), a !== void 0 && g.push("page=" + encodeURIComponent(String(a))), p !== void 0 && g.push("page_size=" + encodeURIComponent(String(p))), l.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/provenances${g.length > 0 ? "?" + g.join("&") : ""}`, l);
  }
  /**
   * Update data
   * 
   * @param Authorization Authentication token
   * @param body Data description
   * @param Accept_Language Request accepted language
   
   */
  updatePath() {
    return "/core/data";
  }
  update(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/data`, e, t);
  }
  /**
   * Update confidence index
   * 
   * @param uri Data URI
   * @param Authorization Authentication token
   * @param body Data description
   * @param Accept_Language Request accepted language
   
   */
  updateConfidencePath() {
    return "/core/data/${encodeURIComponent(String(uri))}/confidence";
  }
  updateConfidence(e, n, t = "body", o = {}) {
    return o.Accept = "application/json", o["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/data/${encodeURIComponent(String(e))}/confidence`, n, o);
  }
  /**
   * Update a provenance
   * 
   * @param Authorization Authentication token
   * @param body Provenance description
   * @param Accept_Language Request accepted language
   
   */
  updateProvenancePath() {
    return "/core/provenances";
  }
  updateProvenance(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/provenances`, e, t);
  }
};
oe = zt([
  x(),
  Ge(0, A("IApiHttpClient")),
  Ge(1, A("IAPIConfiguration"))
], oe);
var Wt = Object.getOwnPropertyDescriptor, Jt = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Wt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Me = (e, n) => (t, o) => n(t, o, e);
let re = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count device data
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiment Search by experiment uris
   * @param variable Search by variables
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenance Search by provenance uri
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param Accept_Language Request accepted language
   
   */
  countDeviceDataPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/data/count";
  }
  countDeviceData(e, n, t, o, r, i, s, a, p, c, l, g = "body", P = {}) {
    let m = [];
    return n !== void 0 && m.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && m.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && m.push("timezone=" + encodeURIComponent(String(o))), r && r.forEach((R) => {
      m.push("experiment=" + encodeURIComponent(String(R)));
    }), i && i.forEach((R) => {
      m.push("variable=" + encodeURIComponent(String(R)));
    }), s !== void 0 && m.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && m.push("max_confidence=" + encodeURIComponent(String(a))), p && p.forEach((R) => {
      m.push("provenance=" + encodeURIComponent(String(R)));
    }), c !== void 0 && m.push("metadata=" + encodeURIComponent(String(c))), l && l.forEach((R) => {
      m.push("operators=" + encodeURIComponent(String(R)));
    }), P.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/data/count${m.length > 0 ? "?" + m.join("&") : ""}`, P);
  }
  /**
   * Create a device
   * 
   * @param Authorization Authentication token
   * @param body Device description
   * @param checkOnly Checking only
   * @param Accept_Language Request accepted language
   
   */
  createDevicePath() {
    return "/core/devices";
  }
  createDevice(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("checkOnly=" + encodeURIComponent(String(e))), o.Accept = "application/json", o["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/devices${r.length > 0 ? "?" + r.join("&") : ""}`, n, o);
  }
  /**
   * Delete a device
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteDevicePath() {
    return "/core/devices/${encodeURIComponent(String(uri))}";
  }
  deleteDevice(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * export devices
   * 
   * @param Authorization Authentication token
   * @param rdf_type RDF type filter
   * @param include_subtypes Set this param to true when filtering on rdf_type to also retrieve sub-types
   * @param name Regex pattern for filtering by name
   * @param year Search by year
   * @param existence_date Date to filter device existence
   * @param brand Regex pattern for filtering by brand
   * @param model Regex pattern for filtering by model
   * @param serial_number Regex pattern for filtering by serial number
   * @param metadata Search by metadata
   * @param Accept_Language Request accepted language
   
   */
  exportDevicesPath() {
    return "/core/devices/export";
  }
  exportDevices(e, n, t, o, r, i, s, a, p, c = "body", l = {}) {
    let g = [];
    return e !== void 0 && g.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && g.push("include_subtypes=" + encodeURIComponent(String(n))), t !== void 0 && g.push("name=" + encodeURIComponent(String(t))), o !== void 0 && g.push("year=" + encodeURIComponent(String(o))), r !== void 0 && g.push("existence_date=" + encodeURIComponent(String(r))), i !== void 0 && g.push("brand=" + encodeURIComponent(String(i))), s !== void 0 && g.push("model=" + encodeURIComponent(String(s))), a !== void 0 && g.push("serial_number=" + encodeURIComponent(String(a))), p !== void 0 && g.push("metadata=" + encodeURIComponent(String(p))), l.Accept = "text/plain", this.httpClient.get(`${this.basePath}/core/devices/export${g.length > 0 ? "?" + g.join("&") : ""}`, l);
  }
  /**
   * Export a given list of devices URIs to shapefile
   * 
   * @param Authorization Authentication token
   * @param body Devices
   * @param selected_props properties selected
   * @param format export format (shp/geojson)
   * @param pageSize Page size limited to 10,000 objects
   * @param Accept_Language Request accepted language
   
   */
  exportGeospatial1Path() {
    return "/core/devices/export_geospatial";
  }
  exportGeospatial1(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e && e.forEach((p) => {
      s.push("selected_props=" + encodeURIComponent(String(p)));
    }), n !== void 0 && s.push("format=" + encodeURIComponent(String(n))), t !== void 0 && s.push("pageSize=" + encodeURIComponent(String(t))), i.Accept = "application/octet-stream", i["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/devices/export_geospatial${s.length > 0 ? "?" + s.join("&") : ""}`, o, i);
  }
  /**
   * export devices
   * 
   * @param Authorization Authentication token
   * @param body List of device URI
   * @param Accept_Language Request accepted language
   
   */
  exportListPath() {
    return "/core/devices/export_by_uris";
  }
  exportList(e, n = "body", t = {}) {
    return t.Accept = "text/plain", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/devices/export_by_uris`, e, t);
  }
  /**
   * Get device detail
   * 
   * @param uri device URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDevicePath() {
    return "/core/devices/${encodeURIComponent(String(uri))}";
  }
  getDevice(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get devices by uris
   * 
   * @param uris Device URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDeviceByUrisPath() {
    return "/core/devices/by_uris";
  }
  getDeviceByUris(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getDeviceByUris.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get provenances of datafiles linked to this device
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDeviceDataFilesProvenancesPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/datafiles/provenances";
  }
  getDeviceDataFilesProvenances(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/datafiles/provenances`, t);
  }
  /**
   * Get provenances of data that have been measured on this device
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDeviceDataProvenancesPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/data/provenances";
  }
  getDeviceDataProvenances(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/data/provenances`, t);
  }
  /**
   * Get variables linked to the device
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDeviceVariablesPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/variables";
  }
  getDeviceVariables(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/variables`, t);
  }
  /**
   * Get devices by facility
   * 
   * @param uri target URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDevicesByFacilityPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/facility";
  }
  getDevicesByFacility(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/facility`, t);
  }
  /**
   * Search device data
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiment Search by experiment uris
   * @param variable Search by variables
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenance Search by provenance uri
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchDeviceDataPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/data";
  }
  searchDeviceData(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y = "body", R = {}) {
    let f = [];
    return n !== void 0 && f.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && f.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && f.push("timezone=" + encodeURIComponent(String(o))), r && r.forEach(($) => {
      f.push("experiment=" + encodeURIComponent(String($)));
    }), i && i.forEach(($) => {
      f.push("variable=" + encodeURIComponent(String($)));
    }), s !== void 0 && f.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && f.push("max_confidence=" + encodeURIComponent(String(a))), p && p.forEach(($) => {
      f.push("provenance=" + encodeURIComponent(String($)));
    }), c !== void 0 && f.push("metadata=" + encodeURIComponent(String(c))), l && l.forEach(($) => {
      f.push("operators=" + encodeURIComponent(String($)));
    }), g && g.forEach(($) => {
      f.push("order_by=" + encodeURIComponent(String($)));
    }), P !== void 0 && f.push("page=" + encodeURIComponent(String(P))), m !== void 0 && f.push("page_size=" + encodeURIComponent(String(m))), R.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/data${f.length > 0 ? "?" + f.join("&") : ""}`, R);
  }
  /**
   * Search device datafiles descriptions
   * 
   * @param uri Device URI
   * @param Authorization Authentication token
   * @param rdf_type Search by rdf type uri
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param experiment Search by experiments
   * @param scientific_objects Search by object uris list
   * @param provenances Search by provenance uris list
   * @param metadata Search by metadata
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchDeviceDatafilesPath() {
    return "/core/devices/${encodeURIComponent(String(uri))}/datafiles";
  }
  searchDeviceDatafiles(e, n, t, o, r, i, s, a, p, c, l, g, P = "body", m = {}) {
    let y = [];
    return n !== void 0 && y.push("rdf_type=" + encodeURIComponent(String(n))), t !== void 0 && y.push("start_date=" + encodeURIComponent(String(t))), o !== void 0 && y.push("end_date=" + encodeURIComponent(String(o))), r !== void 0 && y.push("timezone=" + encodeURIComponent(String(r))), i && i.forEach((f) => {
      y.push("experiment=" + encodeURIComponent(String(f)));
    }), s && s.forEach((f) => {
      y.push("scientific_objects=" + encodeURIComponent(String(f)));
    }), a && a.forEach((f) => {
      y.push("provenances=" + encodeURIComponent(String(f)));
    }), p !== void 0 && y.push("metadata=" + encodeURIComponent(String(p))), c && c.forEach((f) => {
      y.push("order_by=" + encodeURIComponent(String(f)));
    }), l !== void 0 && y.push("page=" + encodeURIComponent(String(l))), g !== void 0 && y.push("page_size=" + encodeURIComponent(String(g))), m.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices/${encodeURIComponent(String(e))}/datafiles${y.length > 0 ? "?" + y.join("&") : ""}`, m);
  }
  /**
   * Search devices
   * 
   * @param Authorization Authentication token
   * @param rdf_type RDF type filter
   * @param include_subtypes Set this param to true when filtering on rdf_type to also retrieve sub-types
   * @param name Regex pattern for filtering by name
   * @param variable Variable
   * @param year Search by year
   * @param existence_date Date to filter device existence
   * @param facility Search by facility
   * @param brand Regex pattern for filtering by brand
   * @param model Regex pattern for filtering by model
   * @param serial_number Regex pattern for filtering by serial number
   * @param metadata Search by metadata
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchDevicesPath() {
    return "/core/devices";
  }
  searchDevices(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y = "body", R = {}) {
    let f = [];
    return e !== void 0 && f.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && f.push("include_subtypes=" + encodeURIComponent(String(n))), t !== void 0 && f.push("name=" + encodeURIComponent(String(t))), o !== void 0 && f.push("variable=" + encodeURIComponent(String(o))), r !== void 0 && f.push("year=" + encodeURIComponent(String(r))), i !== void 0 && f.push("existence_date=" + encodeURIComponent(String(i))), s !== void 0 && f.push("facility=" + encodeURIComponent(String(s))), a !== void 0 && f.push("brand=" + encodeURIComponent(String(a))), p !== void 0 && f.push("model=" + encodeURIComponent(String(p))), c !== void 0 && f.push("serial_number=" + encodeURIComponent(String(c))), l !== void 0 && f.push("metadata=" + encodeURIComponent(String(l))), g && g.forEach(($) => {
      f.push("order_by=" + encodeURIComponent(String($)));
    }), P !== void 0 && f.push("page=" + encodeURIComponent(String(P))), m !== void 0 && f.push("page_size=" + encodeURIComponent(String(m))), R.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/devices${f.length > 0 ? "?" + f.join("&") : ""}`, R);
  }
  /**
   * Update a device
   * 
   * @param body Device description
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  updateDevicePath() {
    return "/core/devices";
  }
  updateDevice(e, n = "body", t = {}) {
    if (t.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling updateDevice.");
    return t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/devices`, e, t);
  }
};
re = Jt([
  x(),
  Me(0, A("IApiHttpClient")),
  Me(1, A("IAPIConfiguration"))
], re);
var Yt = Object.getOwnPropertyDescriptor, Xt = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Yt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Fe = (e, n) => (t, o) => n(t, o, e);
let ie = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count documents
   * 
   * @param Authorization Authentication token
   * @param target Target URI
   * @param Accept_Language Request accepted language
   
   */
  countDocumentsPath() {
    return "/core/documents/count";
  }
  countDocuments(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("target=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/documents/count${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Delete a document
   * 
   * @param uri Document URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteDocumentPath() {
    return "/core/documents/${encodeURIComponent(String(uri))}";
  }
  deleteDocument(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/documents/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get document
   * 
   * @param uri Document URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDocumentFilePath() {
    return "/core/documents/${encodeURIComponent(String(uri))}";
  }
  getDocumentFile(e, n = "body", t = {}) {
    return t.Accept = "application/octet-stream", this.httpClient.get(`${this.basePath}/core/documents/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get document&#39;s description
   * 
   * @param uri Document URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getDocumentMetadataPath() {
    return "/core/documents/${encodeURIComponent(String(uri))}/description";
  }
  getDocumentMetadata(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/documents/${encodeURIComponent(String(e))}/description`, t);
  }
  /**
   * Search documents
   * 
   * @param Authorization Authentication token
   * @param rdf_type Search by type
   * @param title Regex pattern for filtering list by title
   * @param date Regex pattern for filtering list by date
   * @param targets Search by targets
   * @param authors Regex pattern for filtering list by author
   * @param keyword Regex pattern for filtering list by keyword
   * @param multiple Regex pattern for filtering list by keyword or title
   * @param deprecated Search deprecated file
   * @param order_by List of fields to sort as an array of fieldTitle&#x3D;asc|desc
   * @param page Page number
   * @param pageSize Page size
   * @param Accept_Language Request accepted language
   
   */
  searchDocumentsPath() {
    return "/core/documents";
  }
  searchDocuments(e, n, t, o, r, i, s, a, p, c, l, g = "body", P = {}) {
    let m = [];
    return e !== void 0 && m.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && m.push("title=" + encodeURIComponent(String(n))), t !== void 0 && m.push("date=" + encodeURIComponent(String(t))), o !== void 0 && m.push("targets=" + encodeURIComponent(String(o))), r !== void 0 && m.push("authors=" + encodeURIComponent(String(r))), i !== void 0 && m.push("keyword=" + encodeURIComponent(String(i))), s !== void 0 && m.push("multiple=" + encodeURIComponent(String(s))), a !== void 0 && m.push("deprecated=" + encodeURIComponent(String(a))), p && p.forEach((R) => {
      m.push("order_by=" + encodeURIComponent(String(R)));
    }), c !== void 0 && m.push("page=" + encodeURIComponent(String(c))), l !== void 0 && m.push("pageSize=" + encodeURIComponent(String(l))), P.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/documents${m.length > 0 ? "?" + m.join("&") : ""}`, P);
  }
};
ie = Xt([
  x(),
  Fe(0, A("IApiHttpClient")),
  Fe(1, A("IAPIConfiguration"))
], ie);
var Zt = Object.getOwnPropertyDescriptor, Qt = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Zt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Le = (e, n) => (t, o) => n(t, o, e);
let se = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count events
   * 
   * @param targets Targets URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  countEventsPath() {
    return "/core/events/count";
  }
  countEvents(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter targets was null or undefined when calling countEvents.");
    return e && e.forEach((i) => {
      o.push("targets=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/events/count${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Create a list of event
   * 
   * @param Authorization Authentication token
   * @param body 
   * @param Accept_Language Request accepted language
   
   */
  createEventsPath() {
    return "/core/events";
  }
  createEvents(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/events`, e, t);
  }
  /**
   * Create a list of move event
   * 
   * @param Authorization Authentication token
   * @param body 
   * @param Accept_Language Request accepted language
   
   */
  createMovesPath() {
    return "/core/events/moves";
  }
  createMoves(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/events/moves`, e, t);
  }
  /**
   * Delete an event
   * 
   * @param uri Event URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteEventPath() {
    return "/core/events/${encodeURIComponent(String(uri))}";
  }
  deleteEvent(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/events/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a move event
   * 
   * @param uri Event URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteMoveEventPath() {
    return "/core/events/moves/${encodeURIComponent(String(uri))}";
  }
  deleteMoveEvent(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/events/moves/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get an event
   * 
   * @param uri Event URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getEventPath() {
    return "/core/events/${encodeURIComponent(String(uri))}";
  }
  getEvent(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/events/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get an event with all it&#39;s properties
   * 
   * @param uri Event URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getEventDetailsPath() {
    return "/core/events/${encodeURIComponent(String(uri))}/details";
  }
  getEventDetails(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/events/${encodeURIComponent(String(e))}/details`, t);
  }
  /**
   * Get a move with all it&#39;s properties
   * 
   * @param uri Move URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getMoveEventPath() {
    return "/core/events/moves/${encodeURIComponent(String(uri))}";
  }
  getMoveEvent(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/events/moves/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a list of moves with all positional information
   * 
   * @param uris Move URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getMoveEventByUrisPath() {
    return "/core/events/moves/by_uris";
  }
  getMoveEventByUris(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getMoveEventByUris.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/events/moves/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search events
   * 
   * @param Authorization Authentication token
   * @param rdf_type Event type
   * @param start Start date : match event after the given start date
   * @param end End date : match event before the given end date
   * @param target Target partial/exact URI
   * @param description Description regex pattern
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchEventsPath() {
    return "/core/events";
  }
  searchEvents(e, n, t, o, r, i, s, a, p = "body", c = {}) {
    let l = [];
    return e !== void 0 && l.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && l.push("start=" + encodeURIComponent(String(n))), t !== void 0 && l.push("end=" + encodeURIComponent(String(t))), o !== void 0 && l.push("target=" + encodeURIComponent(String(o))), r !== void 0 && l.push("description=" + encodeURIComponent(String(r))), i && i.forEach((P) => {
      l.push("order_by=" + encodeURIComponent(String(P)));
    }), s !== void 0 && l.push("page=" + encodeURIComponent(String(s))), a !== void 0 && l.push("page_size=" + encodeURIComponent(String(a))), c.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/events${l.length > 0 ? "?" + l.join("&") : ""}`, c);
  }
  /**
   * Update an event
   * 
   * @param Authorization Authentication token
   * @param body Event description
   * @param Accept_Language Request accepted language
   
   */
  updateEventPath() {
    return "/core/events";
  }
  updateEvent(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/events`, e, t);
  }
  /**
   * Update a move event
   * 
   * @param Authorization Authentication token
   * @param body Event description
   * @param Accept_Language Request accepted language
   
   */
  updateMoveEventPath() {
    return "/core/events/moves";
  }
  updateMoveEvent(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/events/moves`, e, t);
  }
};
se = Qt([
  x(),
  Le(0, A("IApiHttpClient")),
  Le(1, A("IAPIConfiguration"))
], se);
var Kt = Object.getOwnPropertyDescriptor, en = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Kt(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Ve = (e, n) => (t, o) => n(t, o, e);
let ae = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add an experiment
   * 
   * @param Authorization Authentication token
   * @param body Experiment description
   * @param Accept_Language Request accepted language
   
   */
  createExperimentPath() {
    return "/core/experiments";
  }
  createExperiment(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/experiments`, e, t);
  }
  /**
   * Delete an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteExperimentPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}";
  }
  deleteExperiment(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * export experiment data
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param scientific_objects Search by objects
   * @param variables Search by variables
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenance Search by provenance uri
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param mode Format wide or long
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  exportExperimentDataListPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/data/export";
  }
  exportExperimentDataList(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R = "body", f = {}) {
    let C = [];
    return n !== void 0 && C.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && C.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && C.push("timezone=" + encodeURIComponent(String(o))), r && r.forEach((d) => {
      C.push("scientific_objects=" + encodeURIComponent(String(d)));
    }), i && i.forEach((d) => {
      C.push("variables=" + encodeURIComponent(String(d)));
    }), s !== void 0 && C.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && C.push("max_confidence=" + encodeURIComponent(String(a))), p !== void 0 && C.push("provenance=" + encodeURIComponent(String(p))), c !== void 0 && C.push("metadata=" + encodeURIComponent(String(c))), l && l.forEach((d) => {
      C.push("operators=" + encodeURIComponent(String(d)));
    }), g !== void 0 && C.push("mode=" + encodeURIComponent(String(g))), P && P.forEach((d) => {
      C.push("order_by=" + encodeURIComponent(String(d)));
    }), m !== void 0 && C.push("page=" + encodeURIComponent(String(m))), y !== void 0 && C.push("page_size=" + encodeURIComponent(String(y))), f.Accept = "text/plain", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/data/export${C.length > 0 ? "?" + C.join("&") : ""}`, f);
  }
  /**
   * Get facilities available for an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAvailableFacilitiesPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/available_facilities";
  }
  getAvailableFacilities(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/available_facilities`, t);
  }
  /**
   * Get factors with their levels associated to an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAvailableFactorsPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/factors";
  }
  getAvailableFactors(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/factors`, t);
  }
  /**
   * Get species present in an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAvailableSpeciesPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/species";
  }
  getAvailableSpecies(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/species`, t);
  }
  /**
   * Get an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getExperimentPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}";
  }
  getExperiment(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get experiments URIs
   * 
   * @param uris Experiments URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getExperimentsByURIsPath() {
    return "/core/experiments/by_uris";
  }
  getExperimentsByURIs(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getExperimentsByURIs.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get variables involved in an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param scientific_objects Search by objects uris
   * @param Accept_Language Request accepted language
   
   */
  getUsedVariables1Path() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/variables";
  }
  getUsedVariables1(e, n, t = "body", o = {}) {
    let r = [];
    return n && n.forEach((s) => {
      r.push("scientific_objects=" + encodeURIComponent(String(s)));
    }), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/variables${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search data
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param timezone Precise the timezone corresponding to the given dates
   * @param scientific_objects Search by objects
   * @param variables Search by variables
   * @param min_confidence Search by minimal confidence index
   * @param max_confidence Search by maximal confidence index
   * @param provenances Search by provenance uri
   * @param metadata Search by metadata
   * @param operators Search by operators
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchExperimentDataListPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/data";
  }
  searchExperimentDataList(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y = "body", R = {}) {
    let f = [];
    return n !== void 0 && f.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && f.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && f.push("timezone=" + encodeURIComponent(String(o))), r && r.forEach(($) => {
      f.push("scientific_objects=" + encodeURIComponent(String($)));
    }), i && i.forEach(($) => {
      f.push("variables=" + encodeURIComponent(String($)));
    }), s !== void 0 && f.push("min_confidence=" + encodeURIComponent(String(s))), a !== void 0 && f.push("max_confidence=" + encodeURIComponent(String(a))), p && p.forEach(($) => {
      f.push("provenances=" + encodeURIComponent(String($)));
    }), c !== void 0 && f.push("metadata=" + encodeURIComponent(String(c))), l && l.forEach(($) => {
      f.push("operators=" + encodeURIComponent(String($)));
    }), g && g.forEach(($) => {
      f.push("order_by=" + encodeURIComponent(String($)));
    }), P !== void 0 && f.push("page=" + encodeURIComponent(String(P))), m !== void 0 && f.push("page_size=" + encodeURIComponent(String(m))), R.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/data${f.length > 0 ? "?" + f.join("&") : ""}`, R);
  }
  /**
   * Get provenances involved in an experiment
   * 
   * @param uri Experiment URI
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering by name
   * @param description Search by description
   * @param activity Search by activity URI
   * @param activity_type Search by activity type
   * @param agent Search by agent URI
   * @param agent_type Search by agent type
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchExperimentProvenancesPath() {
    return "/core/experiments/${encodeURIComponent(String(uri))}/provenances";
  }
  searchExperimentProvenances(e, n, t, o, r, i, s, a, p, c, l = "body", g = {}) {
    let P = [];
    return n !== void 0 && P.push("name=" + encodeURIComponent(String(n))), t !== void 0 && P.push("description=" + encodeURIComponent(String(t))), o !== void 0 && P.push("activity=" + encodeURIComponent(String(o))), r !== void 0 && P.push("activity_type=" + encodeURIComponent(String(r))), i !== void 0 && P.push("agent=" + encodeURIComponent(String(i))), s !== void 0 && P.push("agent_type=" + encodeURIComponent(String(s))), a && a.forEach((y) => {
      P.push("order_by=" + encodeURIComponent(String(y)));
    }), p !== void 0 && P.push("page=" + encodeURIComponent(String(p))), c !== void 0 && P.push("page_size=" + encodeURIComponent(String(c))), g.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/${encodeURIComponent(String(e))}/provenances${P.length > 0 ? "?" + P.join("&") : ""}`, g);
  }
  /**
   * Search experiments
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering by name
   * @param year Search by year
   * @param is_ended Search ended(false) or active experiments(true)
   * @param species Search by involved species
   * @param factors Search by studied effect
   * @param projects Search by related project uri
   * @param is_public Search private(false) or public experiments(true)
   * @param facilities Search by involved facilities
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchExperimentsPath() {
    return "/core/experiments";
  }
  searchExperiments(e, n, t, o, r, i, s, a, p, c, l, g = "body", P = {}) {
    let m = [];
    return e !== void 0 && m.push("name=" + encodeURIComponent(String(e))), n !== void 0 && m.push("year=" + encodeURIComponent(String(n))), t !== void 0 && m.push("is_ended=" + encodeURIComponent(String(t))), o && o.forEach((R) => {
      m.push("species=" + encodeURIComponent(String(R)));
    }), r && r.forEach((R) => {
      m.push("factors=" + encodeURIComponent(String(R)));
    }), i && i.forEach((R) => {
      m.push("projects=" + encodeURIComponent(String(R)));
    }), s !== void 0 && m.push("is_public=" + encodeURIComponent(String(s))), a && a.forEach((R) => {
      m.push("facilities=" + encodeURIComponent(String(R)));
    }), p && p.forEach((R) => {
      m.push("order_by=" + encodeURIComponent(String(R)));
    }), c !== void 0 && m.push("page=" + encodeURIComponent(String(c))), l !== void 0 && m.push("page_size=" + encodeURIComponent(String(l))), P.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments${m.length > 0 ? "?" + m.join("&") : ""}`, P);
  }
  /**
   * Update an experiment
   * 
   * @param Authorization Authentication token
   * @param body Experiment description
   * @param Accept_Language Request accepted language
   
   */
  updateExperimentPath() {
    return "/core/experiments";
  }
  updateExperiment(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/experiments`, e, t);
  }
};
ae = en([
  x(),
  Ve(0, A("IApiHttpClient")),
  Ve(1, A("IAPIConfiguration"))
], ae);
var tn = Object.getOwnPropertyDescriptor, nn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? tn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, ke = (e, n) => (t, o) => n(t, o, e);
let pe = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count factors
   * 
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param Accept_Language Request accepted language
   
   */
  countFactorsPath() {
    return "/core/experiments/factors/count";
  }
  countFactors(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("experiment=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/count${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Create a factor
   * 
   * @param Authorization Authentication token
   * @param body Factor description
   * @param Accept_Language Request accepted language
   
   */
  createFactorPath() {
    return "/core/experiments/factors";
  }
  createFactor(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/experiments/factors`, e, t);
  }
  /**
   * Delete a factor
   * 
   * @param uri Factor URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteFactorPath() {
    return "/core/experiments/factors/${encodeURIComponent(String(uri))}";
  }
  deleteFactor(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/experiments/factors/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a factor level
   * 
   * @param uri Factor level URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteFactorLevelPath() {
    return "/core/experiments/factors/levels/${encodeURIComponent(String(uri))}";
  }
  deleteFactorLevel(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/experiments/factors/levels/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get factor associated experiments
   * 
   * @param uri Factor URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFactorAssociatedExperimentsPath() {
    return "/core/experiments/factors/${encodeURIComponent(String(uri))}/experiments";
  }
  getFactorAssociatedExperiments(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/${encodeURIComponent(String(e))}/experiments`, t);
  }
  /**
   * Get a factor
   * 
   * @param uri Factor URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFactorByURIPath() {
    return "/core/experiments/factors/${encodeURIComponent(String(uri))}";
  }
  getFactorByURI(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a factor level
   * 
   * @param uri Factor Level URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFactorLevelPath() {
    return "/core/experiments/factors/levels/${encodeURIComponent(String(uri))}";
  }
  getFactorLevel(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/levels/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a factor level
   * 
   * @param uri Factor Level URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFactorLevelDetailPath() {
    return "/core/experiments/factors/levels/${encodeURIComponent(String(uri))}/details";
  }
  getFactorLevelDetail(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/levels/${encodeURIComponent(String(e))}/details`, t);
  }
  /**
   * Get factor levels
   * 
   * @param uri Factor URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFactorLevelsPath() {
    return "/core/experiments/factors/${encodeURIComponent(String(uri))}/levels";
  }
  getFactorLevels(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/${encodeURIComponent(String(e))}/levels`, t);
  }
  /**
   * Get a list of factors by their URIs
   * 
   * @param uris Factors URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFactorsByURIsPath() {
    return "/core/experiments/factors/by_uris";
  }
  getFactorsByURIs(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getFactorsByURIs.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search categories
   * 
   * @param name Category name regex pattern
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param Accept_Language Request accepted language
   
   */
  searchCategoriesPath() {
    return "/core/experiments/factors/categories";
  }
  searchCategories(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("name=" + encodeURIComponent(String(e))), n && n.forEach((s) => {
      r.push("order_by=" + encodeURIComponent(String(s)));
    }), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/categories${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search factors levels
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering on name
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchFactorLevelsPath() {
    return "/core/experiments/factors/factor_levels";
  }
  searchFactorLevels(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e !== void 0 && s.push("name=" + encodeURIComponent(String(e))), n && n.forEach((p) => {
      s.push("order_by=" + encodeURIComponent(String(p)));
    }), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors/factor_levels${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Search factors
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering on name
   * @param description Regex pattern for filtering on description
   * @param category Filter by category of a factor
   * @param experiment Filter by experiment
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchFactorsPath() {
    return "/core/experiments/factors";
  }
  searchFactors(e, n, t, o, r, i, s, a = "body", p = {}) {
    let c = [];
    return e !== void 0 && c.push("name=" + encodeURIComponent(String(e))), n !== void 0 && c.push("description=" + encodeURIComponent(String(n))), t !== void 0 && c.push("category=" + encodeURIComponent(String(t))), o !== void 0 && c.push("experiment=" + encodeURIComponent(String(o))), r && r.forEach((g) => {
      c.push("order_by=" + encodeURIComponent(String(g)));
    }), i !== void 0 && c.push("page=" + encodeURIComponent(String(i))), s !== void 0 && c.push("page_size=" + encodeURIComponent(String(s))), p.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/experiments/factors${c.length > 0 ? "?" + c.join("&") : ""}`, p);
  }
  /**
   * Update a factor
   * 
   * @param Authorization Authentication token
   * @param body Factor description
   * @param Accept_Language Request accepted language
   
   */
  updateFactorPath() {
    return "/core/experiments/factors";
  }
  updateFactor(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/experiments/factors`, e, t);
  }
};
pe = nn([
  x(),
  ke(0, A("IApiHttpClient")),
  ke(1, A("IAPIConfiguration"))
], pe);
var on = Object.getOwnPropertyDescriptor, rn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? on(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, He = (e, n) => (t, o) => n(t, o, e);
let ce = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add a germplasm
   * 
   * @param Authorization Authentication token
   * @param body Germplasm description
   * @param checkOnly Checking only
   * @param Accept_Language Request accepted language
   
   */
  createGermplasmPath() {
    return "/core/germplasm";
  }
  createGermplasm(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("checkOnly=" + encodeURIComponent(String(e))), o.Accept = "application/json", o["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/germplasm${r.length > 0 ? "?" + r.join("&") : ""}`, n, o);
  }
  /**
   * Add a germplasm group
   * 
   * @param Authorization Authentication token
   * @param body Germplasm group description
   * @param Accept_Language Request accepted language
   
   */
  createGermplasmGroupPath() {
    return "/core/germplasm_group";
  }
  createGermplasmGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/germplasm_group`, e, t);
  }
  /**
   * Delete a germplasm
   * 
   * @param uri Germplasm URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteGermplasmPath() {
    return "/core/germplasm/${encodeURIComponent(String(uri))}";
  }
  deleteGermplasm(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/germplasm/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a germplasm group
   * 
   * @param uri Germplasm group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteGermplasmGroupPath() {
    return "/core/germplasm_group/${encodeURIComponent(String(uri))}";
  }
  deleteGermplasmGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/germplasm_group/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * export germplasm
   * 
   * @param Authorization Authentication token
   * @param body CSV export configuration
   * @param Accept_Language Request accepted language
   
   */
  exportGermplasmPath() {
    return "/core/germplasm/export";
  }
  exportGermplasm(e, n = "body", t = {}) {
    return t.Accept = "text/plain", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/germplasm/export`, e, t);
  }
  /**
   * Get a germplasm
   * 
   * @param uri germplasm URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmPath() {
    return "/core/germplasm/${encodeURIComponent(String(uri))}";
  }
  getGermplasm(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get attribute values of all germplasm for a given attribute
   * 
   * @param attribute 
   * @param Authorization Authentication token
   * @param attribute_value Regex pattern for filtering attribute value
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmAttributeValuesPath() {
    return "/core/germplasm/attributes/${encodeURIComponent(String(attribute))}";
  }
  getGermplasmAttributeValues(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return n !== void 0 && s.push("attribute_value=" + encodeURIComponent(String(n))), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm/attributes/${encodeURIComponent(String(e))}${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Get attributes of all germplasm
   * 
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmAttributesPath() {
    return "/core/germplasm/attributes";
  }
  getGermplasmAttributes(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm/attributes`, n);
  }
  /**
   * Get experiments where a germplasm has been used
   * 
   * @param uri germplasm URI
   * @param Authorization Authentication token
   * @param attribute_value Regex pattern for filtering experiments by name
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmExperimentsPath() {
    return "/core/germplasm/${encodeURIComponent(String(uri))}/experiments";
  }
  getGermplasmExperiments(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return n !== void 0 && a.push("attribute_value=" + encodeURIComponent(String(n))), t && t.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), o !== void 0 && a.push("page=" + encodeURIComponent(String(o))), r !== void 0 && a.push("page_size=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm/${encodeURIComponent(String(e))}/experiments${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Get a germplasm group
   * 
   * @param uri Germplasm group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmGroupPath() {
    return "/core/germplasm_group/${encodeURIComponent(String(uri))}";
  }
  getGermplasmGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm_group/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get germplasm groups by their URIs
   * 
   * @param uris Germplasm group URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmGroupByURIsPath() {
    return "/core/germplasm_group/by-uris";
  }
  getGermplasmGroupByURIs(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getGermplasmGroupByURIs.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm_group/by-uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get a germplasm group&#39;s germplasm, paginated
   * 
   * @param uri Germplasm group URI
   * @param Authorization Authentication token
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmGroupContentPath() {
    return "/core/germplasm_group/${encodeURIComponent(String(uri))}/germplasm";
  }
  getGermplasmGroupContent(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return n && n.forEach((p) => {
      s.push("order_by=" + encodeURIComponent(String(p)));
    }), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm_group/${encodeURIComponent(String(e))}/germplasm${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Get a germplasm group with nested germplasm details
   * 
   * @param uri Germplasm group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmGroupWithGermplasmsPath() {
    return "/core/germplasm_group/with-germplasm/${encodeURIComponent(String(uri))}";
  }
  getGermplasmGroupWithGermplasms(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm_group/with-germplasm/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a list of germplasms by their URIs
   * 
   * @param Authorization Authentication token
   * @param body Germplasms URIs
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmsByURIPath() {
    return "/core/germplasm/by_uris";
  }
  getGermplasmsByURI(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/germplasm/by_uris`, e, t);
  }
  /**
   * Search germplasm
   * 
   * @param Authorization Authentication token
   * @param uri Regex pattern for filtering list by uri
   * @param rdf_type Search by type
   * @param name Regex pattern for filtering list by name and synonyms
   * @param code Regex pattern for filtering list by code
   * @param production_year Search by production year
   * @param species Search by species
   * @param variety Search by variety
   * @param accession Search by accession
   * @param group_of_germplasm Group filter
   * @param institute Search by institute
   * @param experiment Search by experiment
   * @param parent_germplasms Search by parent varieties A or B
   * @param parent_germplasms_m Search by parent varieties A
   * @param parent_germplasms_f Search by parent varieties B
   * @param metadata Search by metadata
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchGermplasmPath() {
    return "/core/germplasm";
  }
  searchGermplasm(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R, f, C, $ = "body", d = {}) {
    let D = [];
    return e !== void 0 && D.push("uri=" + encodeURIComponent(String(e))), n !== void 0 && D.push("rdf_type=" + encodeURIComponent(String(n))), t !== void 0 && D.push("name=" + encodeURIComponent(String(t))), o !== void 0 && D.push("code=" + encodeURIComponent(String(o))), r !== void 0 && D.push("production_year=" + encodeURIComponent(String(r))), i !== void 0 && D.push("species=" + encodeURIComponent(String(i))), s !== void 0 && D.push("variety=" + encodeURIComponent(String(s))), a !== void 0 && D.push("accession=" + encodeURIComponent(String(a))), p !== void 0 && D.push("group_of_germplasm=" + encodeURIComponent(String(p))), c !== void 0 && D.push("institute=" + encodeURIComponent(String(c))), l !== void 0 && D.push("experiment=" + encodeURIComponent(String(l))), g && g.forEach((L) => {
      D.push("parent_germplasms=" + encodeURIComponent(String(L)));
    }), P && P.forEach((L) => {
      D.push("parent_germplasms_m=" + encodeURIComponent(String(L)));
    }), m && m.forEach((L) => {
      D.push("parent_germplasms_f=" + encodeURIComponent(String(L)));
    }), y !== void 0 && D.push("metadata=" + encodeURIComponent(String(y))), R && R.forEach((L) => {
      D.push("order_by=" + encodeURIComponent(String(L)));
    }), f !== void 0 && D.push("page=" + encodeURIComponent(String(f))), C !== void 0 && D.push("page_size=" + encodeURIComponent(String(C))), d.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/germplasm${D.length > 0 ? "?" + D.join("&") : ""}`, d);
  }
  /**
   * Search germplasm groups
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering by name
   * @param germplasm Germplasm URIs
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchGermplasmGroupsPath() {
    return "/core/germplasm_group/search";
  }
  searchGermplasmGroups(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("name=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("germplasm=" + encodeURIComponent(String(c)));
    }), t && t.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), o !== void 0 && a.push("page=" + encodeURIComponent(String(o))), r !== void 0 && a.push("page_size=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.post(`${this.basePath}/core/germplasm_group/search${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Update a germplasm
   * 
   * @param Authorization Authentication token
   * @param body Germplasm description
   * @param Accept_Language Request accepted language
   
   */
  updateGermplasmPath() {
    return "/core/germplasm";
  }
  updateGermplasm(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/germplasm`, e, t);
  }
  /**
   * Update a germplasm group
   * 
   * @param Authorization Authentication token
   * @param body Germplasm group description
   * @param Accept_Language Request accepted language
   
   */
  updateGermplasmGroupPath() {
    return "/core/germplasm_group";
  }
  updateGermplasmGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/germplasm_group`, e, t);
  }
};
ce = rn([
  x(),
  He(0, A("IApiHttpClient")),
  He(1, A("IAPIConfiguration"))
], ce);
var sn = Object.getOwnPropertyDescriptor, an = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? sn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Ne = (e, n) => (t, o) => n(t, o, e);
let ue = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Get an experiment summary history
   * 
   * @param uri Metrics URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getExperimentSummaryHistoryPath() {
    return "/core/metrics/experiment/${encodeURIComponent(String(uri))}";
  }
  getExperimentSummaryHistory(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return n !== void 0 && a.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && a.push("end_date=" + encodeURIComponent(String(t))), o !== void 0 && a.push("page=" + encodeURIComponent(String(o))), r !== void 0 && a.push("page_size=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/metrics/experiment/${encodeURIComponent(String(e))}${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Get running experiments metrics
   * 
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getRunningExperimentsSummaryPath() {
    return "/core/metrics/running_experiments";
  }
  getRunningExperimentsSummary(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e !== void 0 && s.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && s.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/metrics/running_experiments${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Get system metrics
   * 
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getSystemMetricsPath() {
    return "/core/metrics/system";
  }
  getSystemMetrics(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e !== void 0 && s.push("start_date=" + encodeURIComponent(String(e))), n !== void 0 && s.push("end_date=" + encodeURIComponent(String(n))), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/metrics/system${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Get system metrics summary
   * 
   * @param Authorization Authentication token
   * @param period Search by minimal date
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getSystemMetricsSummaryPath() {
    return "/core/metrics/system/summary";
  }
  getSystemMetricsSummary(e, n, t, o = "body", r = {}) {
    let i = [];
    return e !== void 0 && i.push("period=" + encodeURIComponent(String(e))), n !== void 0 && i.push("page=" + encodeURIComponent(String(n))), t !== void 0 && i.push("page_size=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/metrics/system/summary${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
};
ue = an([
  x(),
  Ne(0, A("IApiHttpClient")),
  Ne(1, A("IAPIConfiguration"))
], ue);
var pn = Object.getOwnPropertyDescriptor, cn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? pn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, ze = (e, n) => (t, o) => n(t, o, e);
let he = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add a rdf type property restriction
   * 
   * @param Authorization Authentication token
   * @param body Property description
   * @param Accept_Language Request accepted language
   
   */
  addClassPropertyRestrictionPath() {
    return "/ontology/rdf_type_property_restriction";
  }
  addClassPropertyRestriction(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/ontology/rdf_type_property_restriction`, e, t);
  }
  /**
   * Check the given rdf-types on the given uris
   * 
   * @param rdf_types rdf_types list you want to check on the given uris list
   * @param Authorization Authentication token
   * @param body URIs list
   * @param Accept_Language Request accepted language
   
   */
  checkURIsTypesPath() {
    return "/ontology/check_rdf_types";
  }
  checkURIsTypes(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter rdf_types was null or undefined when calling checkURIsTypes.");
    return e && e.forEach((s) => {
      r.push("rdf_types=" + encodeURIComponent(String(s)));
    }), o.Accept = "application/json", o["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/ontology/check_rdf_types${r.length > 0 ? "?" + r.join("&") : ""}`, n, o);
  }
  /**
   * Create a RDF property
   * 
   * @param Authorization Authentication token
   * @param body Property description
   * @param Accept_Language Request accepted language
   
   */
  createPropertyPath() {
    return "/ontology/property";
  }
  createProperty(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/ontology/property`, e, t);
  }
  /**
   * Delete a rdf type property restriction
   * 
   * @param rdf_type RDF type
   * @param propertyURI Property URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteClassPropertyRestrictionPath() {
    return "/ontology/rdf_type_property_restriction";
  }
  deleteClassPropertyRestriction(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter rdf_type was null or undefined when calling deleteClassPropertyRestriction.");
    if (e !== void 0 && r.push("rdf_type=" + encodeURIComponent(String(e))), !n)
      throw new Error("Required parameter propertyURI was null or undefined when calling deleteClassPropertyRestriction.");
    return n !== void 0 && r.push("propertyURI=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.delete(`${this.basePath}/ontology/rdf_type_property_restriction${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Delete a property
   * 
   * @param uri Property URI
   * @param rdf_type Property type
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deletePropertyPath() {
    return "/ontology/property";
  }
  deleteProperty(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uri was null or undefined when calling deleteProperty.");
    if (e !== void 0 && r.push("uri=" + encodeURIComponent(String(e))), !n)
      throw new Error("Required parameter rdf_type was null or undefined when calling deleteProperty.");
    return n !== void 0 && r.push("rdf_type=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.delete(`${this.basePath}/ontology/property${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Return base uri
   * 
   
   */
  getBaseURIPath() {
    return "/ontology/base_uri";
  }
  getBaseURI(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/base_uri`, n);
  }
  /**
   * Return classes models definitions with properties for a list of rdf types
   * 
   * @param rdf_type RDF classes URI
   * @param Authorization Authentication token
   * @param parent_type Parent RDF class URI
   * @param Accept_Language Request accepted language
   
   */
  getClassesPath() {
    return "/ontology/rdf_types";
  }
  getClasses(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter rdf_type was null or undefined when calling getClasses.");
    return e && e.forEach((s) => {
      r.push("rdf_type=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("parent_type=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/rdf_types${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search data properties tree
   * 
   * @param Authorization Authentication token
   * @param domain Domain URI
   * @param name Name regex pattern
   * @param Accept_Language Request accepted language
   
   */
  getDataPropertiesPath() {
    return "/ontology/data_properties";
  }
  getDataProperties(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("domain=" + encodeURIComponent(String(e))), n !== void 0 && r.push("name=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/data_properties${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search properties linkable to a domain
   * 
   * @param domain Domain URI
   * @param Authorization Authentication token
   * @param parent Domain parent URI
   * @param Accept_Language Request accepted language
   
   */
  getLinkablePropertiesPath() {
    return "/ontology/linkable_properties";
  }
  getLinkableProperties(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter domain was null or undefined when calling getLinkableProperties.");
    return e !== void 0 && r.push("domain=" + encodeURIComponent(String(e))), n !== void 0 && r.push("parent=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/linkable_properties${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Return namespaces
   * 
   
   */
  getNameSpacePath() {
    return "/ontology/name_space";
  }
  getNameSpace(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/name_space`, n);
  }
  /**
   * Search object properties tree
   * 
   * @param Authorization Authentication token
   * @param domain Domain URI
   * @param name Name regex pattern
   * @param Accept_Language Request accepted language
   
   */
  getObjectPropertiesPath() {
    return "/ontology/object_properties";
  }
  getObjectProperties(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("domain=" + encodeURIComponent(String(e))), n !== void 0 && r.push("name=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/object_properties${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search properties tree
   * 
   * @param domain Domain URI
   * @param Authorization Authentication token
   * @param name Name regex pattern
   * @param include_sub_classes Return all properties from sub-classes
   * @param Accept_Language Request accepted language
   
   */
  getPropertiesPath() {
    return "/ontology/properties/${encodeURIComponent(String(domain))}";
  }
  getProperties(e, n, t, o = "body", r = {}) {
    let i = [];
    if (!e)
      throw new Error("Required parameter domain was null or undefined when calling getProperties.");
    return e !== void 0 && i.push("domain=" + encodeURIComponent(String(e))), n !== void 0 && i.push("name=" + encodeURIComponent(String(n))), t !== void 0 && i.push("include_sub_classes=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/properties/${encodeURIComponent(String(e))}${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Get restrictions from some super-class domain to one lower down in the hierarchy, ordered by what domain they first appear in.
   * 
   * @param ancestor Domain ancestor URI
   * @param children Domain uris from types that have ancestor as an ancestor
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getPropertiesByDomainHierarchyUsingRestrictionsPath() {
    return "/ontology/domain_hierarchy_restrictions";
  }
  getPropertiesByDomainHierarchyUsingRestrictions(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter ancestor was null or undefined when calling getPropertiesByDomainHierarchyUsingRestrictions.");
    if (e !== void 0 && r.push("ancestor=" + encodeURIComponent(String(e))), !n)
      throw new Error("Required parameter children was null or undefined when calling getPropertiesByDomainHierarchyUsingRestrictions.");
    return n && n.forEach((s) => {
      r.push("children=" + encodeURIComponent(String(s)));
    }), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/domain_hierarchy_restrictions${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Return property model definition detail
   * 
   * @param Authorization Authentication token
   * @param uri Property URI
   * @param rdf_type Property type
   * @param domain_rdf_type Property type
   * @param Accept_Language Request accepted language
   
   */
  getPropertyPath() {
    return "/ontology/property";
  }
  getProperty(e, n, t, o = "body", r = {}) {
    let i = [];
    return e !== void 0 && i.push("uri=" + encodeURIComponent(String(e))), n !== void 0 && i.push("rdf_type=" + encodeURIComponent(String(n))), t !== void 0 && i.push("domain_rdf_type=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/property${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Return class model definition with properties
   * 
   * @param rdf_type RDF type URI
   * @param Authorization Authentication token
   * @param parent_type Parent RDF class URI
   * @param Accept_Language Request accepted language
   
   */
  getRDFTypePath() {
    return "/ontology/rdf_type";
  }
  getRDFType(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter rdf_type was null or undefined when calling getRDFType.");
    return e !== void 0 && r.push("rdf_type=" + encodeURIComponent(String(e))), n !== void 0 && r.push("parent_type=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/rdf_type${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Return the list of shared resource instances
   * 
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getSharedResourceInstancesPath() {
    return "/ontology/shared_resource_instances";
  }
  getSharedResourceInstances(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/shared_resource_instances`, n);
  }
  /**
   * Search sub-classes tree of an RDF class
   * 
   * @param Authorization Authentication token
   * @param parent_type Parent RDF class URI
   * @param ignoreRootClasses Flag to determine if only sub-classes must be include in result
   * @param Accept_Language Request accepted language
   
   */
  getSubClassesOfPath() {
    return "/ontology/subclasses_of";
  }
  getSubClassesOf(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("parent_type=" + encodeURIComponent(String(e))), n !== void 0 && r.push("ignoreRootClasses=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/subclasses_of${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Return property list from a parent property
   * 
   * @param Authorization Authentication token
   * @param domain Domain URI
   * @param uri Property URI
   * @param ignoreRootProperty Flag to determine if only sub-properties must be included in result
   * @param Accept_Language Request accepted language
   
   */
  getSubPropertiesOfPath() {
    return "/ontology/subproperties_of";
  }
  getSubPropertiesOf(e, n, t, o = "body", r = {}) {
    let i = [];
    return e !== void 0 && i.push("domain=" + encodeURIComponent(String(e))), n !== void 0 && i.push("uri=" + encodeURIComponent(String(n))), t !== void 0 && i.push("ignoreRootProperty=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/subproperties_of${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Return associated rdfs:label of an uri if exists
   * 
   * @param uri URI to get label from
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getURILabelPath() {
    return "/ontology/uri_label";
  }
  getURILabel(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uri was null or undefined when calling getURILabel.");
    return e !== void 0 && o.push("uri=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/uri_label${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Return associated rdfs:label of uris if they exist
   * 
   * @param uri URIs to get label from
   * @param Authorization Authentication token
   * @param context Context URI
   * @param searchDefault Look for all contexts if not present in specified context
   * @param Accept_Language Request accepted language
   
   */
  getURILabelsListPath() {
    return "/ontology/uris_labels";
  }
  getURILabelsList(e, n, t, o = "body", r = {}) {
    let i = [];
    if (!e)
      throw new Error("Required parameter uri was null or undefined when calling getURILabelsList.");
    return e && e.forEach((a) => {
      i.push("uri=" + encodeURIComponent(String(a)));
    }), n !== void 0 && i.push("context=" + encodeURIComponent(String(n))), t !== void 0 && i.push("searchDefault=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/uris_labels${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Return all rdf types of an uri
   * 
   * @param uri URIs to get types from
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getURITypesPath() {
    return "/ontology/uri_types";
  }
  getURITypes(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uri was null or undefined when calling getURITypes.");
    return e && e.forEach((i) => {
      o.push("uri=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/uri_types${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Rename all occurrences of the given URI
   * **This method should not be used unless you are fully understanding what you are doing, as it may have side-effects for external ontologies. Please note that occurrences of the URI will NOT be changed in the NoSQL database (MongoDB).**
   * @param uri The URI to rename
   * @param newUri The new URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  renameURIPath() {
    return "/ontology/${encodeURIComponent(String(uri))}/rename";
  }
  renameURI(e, n, t = "body", o = {}) {
    let r = [];
    if (!n)
      throw new Error("Required parameter newUri was null or undefined when calling renameURI.");
    return n !== void 0 && r.push("newUri=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.put(`${this.basePath}/ontology/${encodeURIComponent(String(e))}/rename${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search sub-classes tree of an RDF class
   * 
   * @param parent_type Parent RDF class URI
   * @param Authorization Authentication token
   * @param name Name regex pattern
   * @param ignoreRootClasses Flag to determine if only sub-classes must be include in result
   * @param Accept_Language Request accepted language
   
   */
  searchSubClassesOfPath() {
    return "/ontology/subclasses_of/search";
  }
  searchSubClassesOf(e, n, t, o = "body", r = {}) {
    let i = [];
    if (!e)
      throw new Error("Required parameter parent_type was null or undefined when calling searchSubClassesOf.");
    return e !== void 0 && i.push("parent_type=" + encodeURIComponent(String(e))), n !== void 0 && i.push("name=" + encodeURIComponent(String(n))), t !== void 0 && i.push("ignoreRootClasses=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/ontology/subclasses_of/search${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Update a rdf type property restriction
   * 
   * @param Authorization Authentication token
   * @param body Property description
   * @param Accept_Language Request accepted language
   
   */
  updateClassPropertyRestrictionPath() {
    return "/ontology/rdf_type_property_restriction";
  }
  updateClassPropertyRestriction(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/ontology/rdf_type_property_restriction`, e, t);
  }
  /**
   * Update a RDF property
   * 
   * @param Authorization Authentication token
   * @param body Property description
   * @param Accept_Language Request accepted language
   
   */
  updatePropertyPath() {
    return "/ontology/property";
  }
  updateProperty(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/ontology/property`, e, t);
  }
};
he = cn([
  x(),
  ze(0, A("IApiHttpClient")),
  ze(1, A("IAPIConfiguration"))
], he);
var un = Object.getOwnPropertyDescriptor, hn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? un(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, We = (e, n) => (t, o) => n(t, o, e);
let de = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Create a facility
   * 
   * @param Authorization Authentication token
   * @param body Facility description
   * @param Accept_Language Request accepted language
   
   */
  createFacilityPath() {
    return "/core/facilities";
  }
  createFacility(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/facilities`, e, t);
  }
  /**
   * Create an organisation
   * 
   * @param Authorization Authentication token
   * @param body Organisation description
   * @param Accept_Language Request accepted language
   
   */
  createOrganizationPath() {
    return "/core/organisations";
  }
  createOrganization(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/organisations`, e, t);
  }
  /**
   * Create a site
   * 
   * @param Authorization Authentication token
   * @param body Site creation object
   * @param Accept_Language Request accepted language
   
   */
  createSitePath() {
    return "/core/sites";
  }
  createSite(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/sites`, e, t);
  }
  /**
   * Delete a facility
   * 
   * @param uri Facility URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteFacilityPath() {
    return "/core/facilities/${encodeURIComponent(String(uri))}";
  }
  deleteFacility(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/facilities/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete an organisation
   * 
   * @param uri Organisation URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteOrganizationPath() {
    return "/core/organisations/${encodeURIComponent(String(uri))}";
  }
  deleteOrganization(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/organisations/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a site
   * 
   * @param uri Site URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteSitePath() {
    return "/core/sites/${encodeURIComponent(String(uri))}";
  }
  deleteSite(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/sites/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get all facilities
   * 
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getAllFacilitiesPath() {
    return "/core/facilities/all_facilities";
  }
  getAllFacilities(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/facilities/all_facilities`, n);
  }
  /**
   * Get facilities by their URIs
   * 
   * @param uris Facilities URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFacilitiesByURIPath() {
    return "/core/facilities/by_uris";
  }
  getFacilitiesByURI(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getFacilitiesByURI.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/facilities/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get a facility
   * 
   * @param uri facility URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getFacilityPath() {
    return "/core/facilities/${encodeURIComponent(String(uri))}";
  }
  getFacility(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/facilities/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get an organisation 
   * 
   * @param uri Organisation URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getOrganizationPath() {
    return "/core/organisations/${encodeURIComponent(String(uri))}";
  }
  getOrganization(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/organisations/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a site
   * 
   * @param uri Site URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getSitePath() {
    return "/core/sites/${encodeURIComponent(String(uri))}";
  }
  getSite(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/sites/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a list of sites
   * 
   * @param uris Site URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getSitesByURIPath() {
    return "/core/sites/by_uris";
  }
  getSitesByURI(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getSitesByURI.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/sites/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search facilities returning minimal embedded information for better performance
   * 
   * @param Authorization Authentication token
   * @param pattern Regex pattern for filtering facilities by names
   * @param organizations List of organizations hosted by the facilities to filter
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  minimalSearchFacilitiesPath() {
    return "/core/facilities/minimal_search";
  }
  minimalSearchFacilities(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("pattern=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("organizations=" + encodeURIComponent(String(c)));
    }), t && t.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), o !== void 0 && a.push("page=" + encodeURIComponent(String(o))), r !== void 0 && a.push("page_size=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/facilities/minimal_search${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search facilities
   * 
   * @param Authorization Authentication token
   * @param pattern Regex pattern for filtering facilities by names
   * @param organizations List of organizations hosted by the facilities to filter
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchFacilitiesPath() {
    return "/core/facilities";
  }
  searchFacilities(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("pattern=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("organizations=" + encodeURIComponent(String(c)));
    }), t && t.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), o !== void 0 && a.push("page=" + encodeURIComponent(String(o))), r !== void 0 && a.push("page_size=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/facilities${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search organisations
   * 
   * @param Authorization Authentication token
   * @param pattern Regex pattern for filtering list by names
   * @param organisation_uris  organisation URIs
   * @param Accept_Language Request accepted language
   
   */
  searchOrganizationsPath() {
    return "/core/organisations";
  }
  searchOrganizations(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("pattern=" + encodeURIComponent(String(e))), n && n.forEach((s) => {
      r.push("organisation_uris=" + encodeURIComponent(String(s)));
    }), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/organisations${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search all sites
   * 
   * @param Authorization Authentication token
   * @param pattern Regex pattern for filtering sites by names
   * @param organizations List of organizations of the sites to filter
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchSitesPath() {
    return "/core/sites";
  }
  searchSites(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("pattern=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("organizations=" + encodeURIComponent(String(c)));
    }), t && t.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), o !== void 0 && a.push("page=" + encodeURIComponent(String(o))), r !== void 0 && a.push("page_size=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/sites${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Update a facility
   * 
   * @param Authorization Authentication token
   * @param body Facility description
   * @param Accept_Language Request accepted language
   
   */
  updateFacilityPath() {
    return "/core/facilities";
  }
  updateFacility(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/facilities`, e, t);
  }
  /**
   * Update an organisation
   * 
   * @param Authorization Authentication token
   * @param body Organisation description
   * @param Accept_Language Request accepted language
   
   */
  updateOrganizationPath() {
    return "/core/organisations";
  }
  updateOrganization(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/organisations`, e, t);
  }
  /**
   * Update a site
   * 
   * @param Authorization Authentication token
   * @param body Site update object
   * @param Accept_Language Request accepted language
   
   */
  updateSitePath() {
    return "/core/sites";
  }
  updateSite(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/sites`, e, t);
  }
};
de = hn([
  x(),
  We(0, A("IApiHttpClient")),
  We(1, A("IAPIConfiguration"))
], de);
var dn = Object.getOwnPropertyDescriptor, fn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? dn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Je = (e, n) => (t, o) => n(t, o, e);
let fe = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count moves
   * 
   * @param Authorization Authentication token
   * @param target Target URI
   * @param Accept_Language Request accepted language
   
   */
  countMovesPath() {
    return "/core/positions/count";
  }
  countMoves(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("target=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/positions/count${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get the position of an object
   * 
   * @param uri Object URI
   * @param Authorization Authentication token
   * @param time Time : match position at the given time
   * @param Accept_Language Request accepted language
   
   */
  getPositionPath() {
    return "/core/positions/${encodeURIComponent(String(uri))}";
  }
  getPosition(e, n, t = "body", o = {}) {
    let r = [];
    return n !== void 0 && r.push("time=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/positions/${encodeURIComponent(String(e))}${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search the last geospatialized position of a target for an experiment
   * 
   * @param body geometry GeoJSON
   * @param Authorization Authentication token
   * @param base_type target RDF Type URI
   * @param startDateTime Start date : match position affected after the given start date
   * @param endDateTime End date : match position affected before the given end date
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchGeospatializedPositionPath() {
    return "/core/positions/geospatializedPosition";
  }
  searchGeospatializedPosition(e, n, t, o, r, i, s = "body", a = {}) {
    let p = [];
    if (n !== void 0 && p.push("base_type=" + encodeURIComponent(String(n))), t !== void 0 && p.push("startDateTime=" + encodeURIComponent(String(t))), o !== void 0 && p.push("endDateTime=" + encodeURIComponent(String(o))), r !== void 0 && p.push("page=" + encodeURIComponent(String(r))), i !== void 0 && p.push("page_size=" + encodeURIComponent(String(i))), a.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling searchGeospatializedPosition.");
    return a["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/positions/geospatializedPosition${p.length > 0 ? "?" + p.join("&") : ""}`, e, a);
  }
  /**
   * Search history of position of an object
   * 
   * @param target Target URI
   * @param Authorization Authentication token
   * @param startDateTime Start date : match position affected after the given start date
   * @param endDateTime End date : match position affected before the given end date
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchPositionHistoryPath() {
    return "/core/positions/history";
  }
  searchPositionHistory(e, n, t, o, r, i, s = "body", a = {}) {
    let p = [];
    if (!e)
      throw new Error("Required parameter target was null or undefined when calling searchPositionHistory.");
    return e !== void 0 && p.push("target=" + encodeURIComponent(String(e))), n !== void 0 && p.push("startDateTime=" + encodeURIComponent(String(n))), t !== void 0 && p.push("endDateTime=" + encodeURIComponent(String(t))), o && o.forEach((l) => {
      p.push("order_by=" + encodeURIComponent(String(l)));
    }), r !== void 0 && p.push("page=" + encodeURIComponent(String(r))), i !== void 0 && p.push("page_size=" + encodeURIComponent(String(i))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/positions/history${p.length > 0 ? "?" + p.join("&") : ""}`, a);
  }
};
fe = fn([
  x(),
  Je(0, A("IApiHttpClient")),
  Je(1, A("IAPIConfiguration"))
], fe);
var ln = Object.getOwnPropertyDescriptor, gn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? ln(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Ye = (e, n) => (t, o) => n(t, o, e);
let le = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Add a project
   * 
   * @param Authorization Authentication token
   * @param body Project description
   * @param Accept_Language Request accepted language
   
   */
  createProjectPath() {
    return "/core/projects";
  }
  createProject(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/projects`, e, t);
  }
  /**
   * Delete a project
   * 
   * @param uri Project URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteProjectPath() {
    return "/core/projects/${encodeURIComponent(String(uri))}";
  }
  deleteProject(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/projects/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a project
   * 
   * @param uri Project URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getProjectPath() {
    return "/core/projects/${encodeURIComponent(String(uri))}";
  }
  getProject(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/projects/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get projects by their URIs
   * 
   * @param uris Projects URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getProjectsByURIPath() {
    return "/core/projects/by_uris";
  }
  getProjectsByURI(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getProjectsByURI.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/projects/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search projects
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering by name or shortname
   * @param year Search by year
   * @param keyword Regex pattern for filtering on description or objective
   * @param financial_funding Regex pattern for filtering by financial funding
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchProjectsPath() {
    return "/core/projects";
  }
  searchProjects(e, n, t, o, r, i, s, a = "body", p = {}) {
    let c = [];
    return e !== void 0 && c.push("name=" + encodeURIComponent(String(e))), n !== void 0 && c.push("year=" + encodeURIComponent(String(n))), t !== void 0 && c.push("keyword=" + encodeURIComponent(String(t))), o !== void 0 && c.push("financial_funding=" + encodeURIComponent(String(o))), r && r.forEach((g) => {
      c.push("order_by=" + encodeURIComponent(String(g)));
    }), i !== void 0 && c.push("page=" + encodeURIComponent(String(i))), s !== void 0 && c.push("page_size=" + encodeURIComponent(String(s))), p.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/projects${c.length > 0 ? "?" + c.join("&") : ""}`, p);
  }
  /**
   * Update a project
   * 
   * @param Authorization Authentication token
   * @param body Project description
   * @param Accept_Language Request accepted language
   
   */
  updateProjectPath() {
    return "/core/projects";
  }
  updateProject(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/projects`, e, t);
  }
};
le = gn([
  x(),
  Ye(0, A("IApiHttpClient")),
  Ye(1, A("IAPIConfiguration"))
], le);
var mn = Object.getOwnPropertyDescriptor, bn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? mn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Xe = (e, n) => (t, o) => n(t, o, e);
let ge = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Count scientific objects
   * 
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param Accept_Language Request accepted language
   
   */
  countScientificObjectsPath() {
    return "/core/scientific_objects/count";
  }
  countScientificObjects(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("experiment=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/count${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Create a scientific object for the given experiment
   * 
   * @param body Scientific object description
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  createScientificObjectPath() {
    return "/core/scientific_objects";
  }
  createScientificObject(e, n = "body", t = {}) {
    if (t.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling createScientificObject.");
    return t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/scientific_objects`, e, t);
  }
  /**
   * Delete a scientific object
   * 
   * @param uri scientific object URI
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param Accept_Language Request accepted language
   
   */
  deleteScientificObjectPath() {
    return "/core/scientific_objects/${encodeURIComponent(String(uri))}";
  }
  deleteScientificObject(e, n, t = "body", o = {}) {
    let r = [];
    return n !== void 0 && r.push("experiment=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/scientific_objects/${encodeURIComponent(String(e))}${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Export a given list of scientific object URIs to csv data file
   * 
   * @param Authorization Authentication token
   * @param body CSV export configuration
   * @param Accept_Language Request accepted language
   
   */
  exportCSVPath() {
    return "/core/scientific_objects/export";
  }
  exportCSV(e, n = "body", t = {}) {
    return t.Accept = "application/octet-stream", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/scientific_objects/export`, e, t);
  }
  /**
   * Export a given list of scientific object URIs to shapefile or geojson
   * 
   * @param Authorization Authentication token
   * @param body Scientific objects
   * @param experiment Experiment URI
   * @param selected_props properties selected
   * @param format export format (shp/geojson)
   * @param pageSize Page size limited to 10,000 objects
   * @param Accept_Language Request accepted language
   
   */
  exportGeospatial2Path() {
    return "/core/scientific_objects/export_geospatial";
  }
  exportGeospatial2(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("experiment=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("selected_props=" + encodeURIComponent(String(c)));
    }), t !== void 0 && a.push("format=" + encodeURIComponent(String(t))), o !== void 0 && a.push("pageSize=" + encodeURIComponent(String(o))), s.Accept = "application/octet-stream", s["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/scientific_objects/export_geospatial${a.length > 0 ? "?" + a.join("&") : ""}`, r, s);
  }
  /**
   * Get provenances of datafiles linked to this scientific object
   * 
   * @param uri Scientific Object URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectDataFilesProvenancesPath() {
    return "/core/scientific_objects/${encodeURIComponent(String(uri))}/datafiles/provenances";
  }
  getScientificObjectDataFilesProvenances(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/${encodeURIComponent(String(e))}/datafiles/provenances`, t);
  }
  /**
   * Get provenances of data that have been measured on this scientific object
   * 
   * @param uri Scientific Object URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectDataProvenancesPath() {
    return "/core/scientific_objects/${encodeURIComponent(String(uri))}/data/provenances";
  }
  getScientificObjectDataProvenances(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/${encodeURIComponent(String(e))}/data/provenances`, t);
  }
  /**
   * Get scientific object detail
   * 
   * @param uri scientific object URI
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectDetailPath() {
    return "/core/scientific_objects/${encodeURIComponent(String(uri))}";
  }
  getScientificObjectDetail(e, n, t = "body", o = {}) {
    let r = [];
    return n !== void 0 && r.push("experiment=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/${encodeURIComponent(String(e))}${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get scientific object detail for each experiments, a null value for experiment in response means a properties defined outside of any experiment (shared object).
   * 
   * @param uri scientific object URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectDetailByExperimentsPath() {
    return "/core/scientific_objects/${encodeURIComponent(String(uri))}/experiments";
  }
  getScientificObjectDetailByExperiments(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/${encodeURIComponent(String(e))}/experiments`, t);
  }
  /**
   * Get variables measured on this scientific object
   * 
   * @param uri Scientific Object URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectVariablesPath() {
    return "/core/scientific_objects/${encodeURIComponent(String(uri))}/variables";
  }
  getScientificObjectVariables(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/${encodeURIComponent(String(e))}/variables`, t);
  }
  /**
   * Get list of scientific object children
   * 
   * @param Authorization Authentication token
   * @param parent Parent object URI
   * @param experiment Experiment URI
   * @param rdf_types RDF type filter
   * @param name Regex pattern for filtering by name
   * @param factor_levels Factor levels URI
   * @param facility Facility
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectsChildrenPath() {
    return "/core/scientific_objects/children";
  }
  getScientificObjectsChildren(e, n, t, o, r, i, s, a, p, c = "body", l = {}) {
    let g = [];
    return e !== void 0 && g.push("parent=" + encodeURIComponent(String(e))), n !== void 0 && g.push("experiment=" + encodeURIComponent(String(n))), t && t.forEach((m) => {
      g.push("rdf_types=" + encodeURIComponent(String(m)));
    }), o !== void 0 && g.push("name=" + encodeURIComponent(String(o))), r && r.forEach((m) => {
      g.push("factor_levels=" + encodeURIComponent(String(m)));
    }), i !== void 0 && g.push("facility=" + encodeURIComponent(String(i))), s && s.forEach((m) => {
      g.push("order_by=" + encodeURIComponent(String(m)));
    }), a !== void 0 && g.push("page=" + encodeURIComponent(String(a))), p !== void 0 && g.push("page_size=" + encodeURIComponent(String(p))), l.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/children${g.length > 0 ? "?" + g.join("&") : ""}`, l);
  }
  /**
   * Get scientific objet list of a given experiment URI
   * 
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param body Scientific object uris
   * @param Accept_Language Request accepted language
   
   */
  getScientificObjectsListByUrisPath() {
    return "/core/scientific_objects/by_uris";
  }
  getScientificObjectsListByUris(e, n, t = "body", o = {}) {
    let r = [];
    return e !== void 0 && r.push("experiment=" + encodeURIComponent(String(e))), o.Accept = "application/json", o["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/scientific_objects/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, n, o);
  }
  /**
   * get used scientific object types
   * 
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param Accept_Language Request accepted language
   
   */
  getUsedTypesPath() {
    return "/core/scientific_objects/used_types";
  }
  getUsedTypes(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("experiment=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/used_types${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Search list of scientific objects
   * 
   * @param Authorization Authentication token
   * @param experiment Experiment URI
   * @param rdf_types RDF type filter
   * @param name Regex pattern for filtering by name
   * @param parent Parent URI
   * @param germplasms Germplasm URIs
   * @param factor_levels Factor levels URI
   * @param facility Facility
   * @param variables Variables URI
   * @param devices Devices URI
   * @param existence_date Date to filter object existence
   * @param creation_date Date to filter object creation
   * @param criteria_on_data A CriteriaDTO to be applied to data, retain objects that are targets in returned data
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchScientificObjectsPath() {
    return "/core/scientific_objects";
  }
  searchScientificObjects(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R = "body", f = {}) {
    let C = [];
    return e !== void 0 && C.push("experiment=" + encodeURIComponent(String(e))), n && n.forEach((d) => {
      C.push("rdf_types=" + encodeURIComponent(String(d)));
    }), t !== void 0 && C.push("name=" + encodeURIComponent(String(t))), o !== void 0 && C.push("parent=" + encodeURIComponent(String(o))), r && r.forEach((d) => {
      C.push("germplasms=" + encodeURIComponent(String(d)));
    }), i && i.forEach((d) => {
      C.push("factor_levels=" + encodeURIComponent(String(d)));
    }), s !== void 0 && C.push("facility=" + encodeURIComponent(String(s))), a && a.forEach((d) => {
      C.push("variables=" + encodeURIComponent(String(d)));
    }), p && p.forEach((d) => {
      C.push("devices=" + encodeURIComponent(String(d)));
    }), c !== void 0 && C.push("existence_date=" + encodeURIComponent(String(c))), l !== void 0 && C.push("creation_date=" + encodeURIComponent(String(l))), g !== void 0 && C.push("criteria_on_data=" + encodeURIComponent(String(g))), P && P.forEach((d) => {
      C.push("order_by=" + encodeURIComponent(String(d)));
    }), m !== void 0 && C.push("page=" + encodeURIComponent(String(m))), y !== void 0 && C.push("page_size=" + encodeURIComponent(String(y))), f.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects${C.length > 0 ? "?" + C.join("&") : ""}`, f);
  }
  /**
   * Get scientific objet list with geometry of a given experiment URI
   * 
   * @param experiment Context URI
   * @param Authorization Authentication token
   * @param start_date Search by minimal date
   * @param end_date Search by maximal date
   * @param Accept_Language Request accepted language
   
   */
  searchScientificObjectsWithGeometryListByUrisPath() {
    return "/core/scientific_objects/geometry";
  }
  searchScientificObjectsWithGeometryListByUris(e, n, t, o = "body", r = {}) {
    let i = [];
    if (!e)
      throw new Error("Required parameter experiment was null or undefined when calling searchScientificObjectsWithGeometryListByUris.");
    return e !== void 0 && i.push("experiment=" + encodeURIComponent(String(e))), n !== void 0 && i.push("start_date=" + encodeURIComponent(String(n))), t !== void 0 && i.push("end_date=" + encodeURIComponent(String(t))), r.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/scientific_objects/geometry${i.length > 0 ? "?" + i.join("&") : ""}`, r);
  }
  /**
   * Update a scientific object for the given experiment
   * 
   * @param body Scientific object description
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  updateScientificObjectPath() {
    return "/core/scientific_objects";
  }
  updateScientificObject(e, n = "body", t = {}) {
    if (t.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling updateScientificObject.");
    return t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/scientific_objects`, e, t);
  }
};
ge = bn([
  x(),
  Xe(0, A("IApiHttpClient")),
  Xe(1, A("IAPIConfiguration"))
], ge);
var Cn = Object.getOwnPropertyDescriptor, yn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Cn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Ze = (e, n) => (t, o) => n(t, o, e);
let me = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * get species (no pagination)
   * 
   * @param sharedResourceInstance Shared resource instance URI
   * @param Accept_Language Request accepted language
   
   */
  getAllSpeciesPath() {
    return "/core/species";
  }
  getAllSpecies(e, n = "body", t = {}) {
    let o = [];
    return e !== void 0 && o.push("sharedResourceInstance=" + encodeURIComponent(String(e))), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/species${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
};
me = yn([
  x(),
  Ze(0, A("IApiHttpClient")),
  Ze(1, A("IAPIConfiguration"))
], me);
var Pn = Object.getOwnPropertyDescriptor, In = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Pn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Qe = (e, n) => (t, o) => n(t, o, e);
let be = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * get system information
   * 
   
   */
  getVersionInfoPath() {
    return "/core/system/info";
  }
  getVersionInfo(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/system/info`, n);
  }
};
be = In([
  x(),
  Qe(0, A("IApiHttpClient")),
  Qe(1, A("IAPIConfiguration"))
], be);
var Sn = Object.getOwnPropertyDescriptor, Rn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? Sn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
}, Ke = (e, n) => (t, o) => n(t, o, e);
let Ce = class {
  constructor(e, n) {
    this.httpClient = e, this.APIConfiguration = n, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * export variable by list of uris
   * 
   * @param Authorization Authentication token
   * @param body List of variable URI
   * @param Accept_Language Request accepted language
   
   */
  classicExportVariableByURIsPath() {
    return "/core/variables/export_classic_by_uris";
  }
  classicExportVariableByURIs(e, n = "body", t = {}) {
    return t.Accept = "text/plain", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/variables/export_classic_by_uris`, e, t);
  }
  /**
   * Copy the selected variables from the shared resource instance
   * 
   * @param body List of variable URI to copy
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  copyFromSharedResourceInstancePath() {
    return "/core/variables/copy_from_shared_resource_instance";
  }
  copyFromSharedResourceInstance(e, n = "body", t = {}) {
    if (t.Accept = "application/json", !e)
      throw new Error("Required parameter body was null or undefined when calling copyFromSharedResourceInstance.");
    return t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/variables/copy_from_shared_resource_instance`, e, t);
  }
  /**
   * Add a characteristic
   * 
   * @param Authorization Authentication token
   * @param body Characteristic description
   * @param Accept_Language Request accepted language
   
   */
  createCharacteristicPath() {
    return "/core/characteristics";
  }
  createCharacteristic(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/characteristics`, e, t);
  }
  /**
   * Add an entity
   * 
   * @param Authorization Authentication token
   * @param body Entity description
   * @param Accept_Language Request accepted language
   
   */
  createEntityPath() {
    return "/core/entities";
  }
  createEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/entities`, e, t);
  }
  /**
   * Add an entity of interest
   * 
   * @param Authorization Authentication token
   * @param body Entity of interest description
   * @param Accept_Language Request accepted language
   
   */
  createInterestEntityPath() {
    return "/core/entities_of_interest";
  }
  createInterestEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/entities_of_interest`, e, t);
  }
  /**
   * Add a method
   * 
   * @param Authorization Authentication token
   * @param body Method description
   * @param Accept_Language Request accepted language
   
   */
  createMethodPath() {
    return "/core/methods";
  }
  createMethod(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/methods`, e, t);
  }
  /**
   * Add an unit
   * 
   * @param Authorization Authentication token
   * @param body Unit description
   * @param Accept_Language Request accepted language
   
   */
  createUnitPath() {
    return "/core/units";
  }
  createUnit(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/units`, e, t);
  }
  /**
   * Add a variable
   * 
   * @param Authorization Authentication token
   * @param body Variable description
   * @param Accept_Language Request accepted language
   
   */
  createVariablePath() {
    return "/core/variables";
  }
  createVariable(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/variables`, e, t);
  }
  /**
   * Add a variables group
   * 
   * @param Authorization Authentication token
   * @param body Variables group description
   * @param Accept_Language Request accepted language
   
   */
  createVariablesGroupPath() {
    return "/core/variables_group";
  }
  createVariablesGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/variables_group`, e, t);
  }
  /**
   * Delete a characteristic
   * 
   * @param uri Characteristic URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteCharacteristicPath() {
    return "/core/characteristics/${encodeURIComponent(String(uri))}";
  }
  deleteCharacteristic(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/characteristics/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete an entity
   * 
   * @param uri Entity URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteEntityPath() {
    return "/core/entities/${encodeURIComponent(String(uri))}";
  }
  deleteEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/entities/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete an entity of interest
   * 
   * @param uri Entity of interest URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteInterestEntityPath() {
    return "/core/entities_of_interest/${encodeURIComponent(String(uri))}";
  }
  deleteInterestEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/entities_of_interest/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a method
   * 
   * @param uri Method URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteMethodPath() {
    return "/core/methods/${encodeURIComponent(String(uri))}";
  }
  deleteMethod(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/methods/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete an unit
   * 
   * @param uri Unit URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteUnitPath() {
    return "/core/units/${encodeURIComponent(String(uri))}";
  }
  deleteUnit(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/units/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a variable
   * 
   * @param uri Variable URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteVariablePath() {
    return "/core/variables/${encodeURIComponent(String(uri))}";
  }
  deleteVariable(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/variables/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Delete a variables group
   * 
   * @param uri Variables group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  deleteVariablesGroupPath() {
    return "/core/variables_group/${encodeURIComponent(String(uri))}";
  }
  deleteVariablesGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.delete(`${this.basePath}/core/variables_group/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * export detailed variable by list of uris
   * 
   * @param Authorization Authentication token
   * @param body List of variable URI
   * @param Accept_Language Request accepted language
   
   */
  detailsExportVariableByURIsPath() {
    return "/core/variables/export_details_by_uris";
  }
  detailsExportVariableByURIs(e, n = "body", t = {}) {
    return t.Accept = "text/plain", t["Content-Type"] = "application/json", this.httpClient.post(`${this.basePath}/core/variables/export_details_by_uris`, e, t);
  }
  /**
   * Get a characteristic
   * 
   * @param uri Characteristic URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getCharacteristicPath() {
    return "/core/characteristics/${encodeURIComponent(String(uri))}";
  }
  getCharacteristic(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/characteristics/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get detailed characteristics by uris
   * 
   * @param uris Characteristics URIs
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getCharacteristicsByURIsPath() {
    return "/core/characteristics/by_uris";
  }
  getCharacteristicsByURIs(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getCharacteristicsByURIs.");
    return e && e.forEach((s) => {
      r.push("uris=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/characteristics/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get variables datatypes
   * 
   
   */
  getDatatypesPath() {
    return "/core/variables/datatypes";
  }
  getDatatypes(e = "body", n = {}) {
    return n.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables/datatypes`, n);
  }
  /**
   * Get detailed entities by uris
   * 
   * @param uris Entities URIs
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getEntitiesByURIsPath() {
    return "/core/entities/by_uris";
  }
  getEntitiesByURIs(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getEntitiesByURIs.");
    return e && e.forEach((s) => {
      r.push("uris=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/entities/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get an entity
   * 
   * @param uri Entity URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getEntityPath() {
    return "/core/entities/${encodeURIComponent(String(uri))}";
  }
  getEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/entities/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get detailed entities of interest by uris
   * 
   * @param uris Entities of interest URIs
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getInterestEntitiesByURIsPath() {
    return "/core/entities_of_interest/by_uris";
  }
  getInterestEntitiesByURIs(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getInterestEntitiesByURIs.");
    return e && e.forEach((s) => {
      r.push("uris=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/entities_of_interest/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get an entity of interest
   * 
   * @param uri Entity of interest URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getInterestEntityPath() {
    return "/core/entities_of_interest/${encodeURIComponent(String(uri))}";
  }
  getInterestEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/entities_of_interest/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get a method
   * 
   * @param uri Method URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getMethodPath() {
    return "/core/methods/${encodeURIComponent(String(uri))}";
  }
  getMethod(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/methods/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get detailed methods by uris
   * 
   * @param uris Methods URIs
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getMethodsByURIsPath() {
    return "/core/methods/by_uris";
  }
  getMethodsByURIs(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getMethodsByURIs.");
    return e && e.forEach((s) => {
      r.push("uris=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/methods/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get an unit
   * 
   * @param uri Unit URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getUnitPath() {
    return "/core/units/${encodeURIComponent(String(uri))}";
  }
  getUnit(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/units/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get detailed units by uris
   * 
   * @param uris Units URIs
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getUnitsByURIsPath() {
    return "/core/units/by_uris";
  }
  getUnitsByURIs(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getUnitsByURIs.");
    return e && e.forEach((s) => {
      r.push("uris=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/units/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get a variable
   * 
   * @param uri Variable URI
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getVariablePath() {
    return "/core/variables/${encodeURIComponent(String(uri))}";
  }
  getVariable(e, n, t = "body", o = {}) {
    let r = [];
    return n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables/${encodeURIComponent(String(e))}${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Get detailed variables by uris
   * 
   * @param uris Variables URIs
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getVariablesByURIsPath() {
    return "/core/variables/by_uris";
  }
  getVariablesByURIs(e, n = "body", t = {}) {
    let o = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getVariablesByURIs.");
    return e && e.forEach((i) => {
      o.push("uris=" + encodeURIComponent(String(i)));
    }), t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables/by_uris${o.length > 0 ? "?" + o.join("&") : ""}`, t);
  }
  /**
   * Get a variables group
   * 
   * @param uri Variables group URI
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getVariablesGroupPath() {
    return "/core/variables_group/${encodeURIComponent(String(uri))}";
  }
  getVariablesGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables_group/${encodeURIComponent(String(e))}`, t);
  }
  /**
   * Get variables groups by their URIs
   * 
   * @param uris Variables group URIs
   * @param Authorization Authentication token
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  getVariablesGroupByURIsPath() {
    return "/core/variables_group/by_uris";
  }
  getVariablesGroupByURIs(e, n, t = "body", o = {}) {
    let r = [];
    if (!e)
      throw new Error("Required parameter uris was null or undefined when calling getVariablesGroupByURIs.");
    return e && e.forEach((s) => {
      r.push("uris=" + encodeURIComponent(String(s)));
    }), n !== void 0 && r.push("sharedResourceInstance=" + encodeURIComponent(String(n))), o.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables_group/by_uris${r.length > 0 ? "?" + r.join("&") : ""}`, o);
  }
  /**
   * Search characteristics by name
   * 
   * @param Authorization Authentication token
   * @param name Name (regex)
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchCharacteristicsPath() {
    return "/core/characteristics";
  }
  searchCharacteristics(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("name=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), t !== void 0 && a.push("page=" + encodeURIComponent(String(t))), o !== void 0 && a.push("page_size=" + encodeURIComponent(String(o))), r !== void 0 && a.push("sharedResourceInstance=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/characteristics${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search entities by name
   * 
   * @param Authorization Authentication token
   * @param name Name (regex)
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchEntitiesPath() {
    return "/core/entities";
  }
  searchEntities(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("name=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), t !== void 0 && a.push("page=" + encodeURIComponent(String(t))), o !== void 0 && a.push("page_size=" + encodeURIComponent(String(o))), r !== void 0 && a.push("sharedResourceInstance=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/entities${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search entities of interest by name
   * 
   * @param Authorization Authentication token
   * @param name Name (regex)
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchInterestEntityPath() {
    return "/core/entities_of_interest";
  }
  searchInterestEntity(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("name=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), t !== void 0 && a.push("page=" + encodeURIComponent(String(t))), o !== void 0 && a.push("page_size=" + encodeURIComponent(String(o))), r !== void 0 && a.push("sharedResourceInstance=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/entities_of_interest${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search methods by name
   * 
   * @param Authorization Authentication token
   * @param name Name (regex)
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchMethodsPath() {
    return "/core/methods";
  }
  searchMethods(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("name=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), t !== void 0 && a.push("page=" + encodeURIComponent(String(t))), o !== void 0 && a.push("page_size=" + encodeURIComponent(String(o))), r !== void 0 && a.push("sharedResourceInstance=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/methods${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search units by name
   * 
   * @param Authorization Authentication token
   * @param name Name (regex)
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchUnitsPath() {
    return "/core/units";
  }
  searchUnits(e, n, t, o, r, i = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("name=" + encodeURIComponent(String(e))), n && n.forEach((c) => {
      a.push("order_by=" + encodeURIComponent(String(c)));
    }), t !== void 0 && a.push("page=" + encodeURIComponent(String(t))), o !== void 0 && a.push("page_size=" + encodeURIComponent(String(o))), r !== void 0 && a.push("sharedResourceInstance=" + encodeURIComponent(String(r))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/units${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
  /**
   * Search variables
   * The following fields could be used for sorting :    _entity_name/entityName : the name of the variable entity   _characteristic_name/characteristicName : the name of the variable characteristic   _method_name/methodName : the name of the variable method   _unit_name/unitName : the name of the variable unit  
   * @param Authorization Authentication token
   * @param name Name regex pattern
   * @param entity Entity filter
   * @param entity_of_interest Entity of interest filter
   * @param characteristic Characteristic filter
   * @param method Method filter
   * @param unit Unit filter
   * @param group_of_variables Included in group filter
   * @param not_included_in_group_of_variables Not included in group filter
   * @param data_type Data type filter
   * @param time_interval Time interval filter
   * @param species Species filter
   * @param withAssociatedData Set this param to true to get associated data
   * @param experiments Experiment filter
   * @param scientific_objects Scientific object filter
   * @param devices Device filter
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchVariablesPath() {
    return "/core/variables";
  }
  searchVariables(e, n, t, o, r, i, s, a, p, c, l, g, P, m, y, R, f, C, $, d = "body", D = {}) {
    let S = [];
    return e !== void 0 && S.push("name=" + encodeURIComponent(String(e))), n !== void 0 && S.push("entity=" + encodeURIComponent(String(n))), t !== void 0 && S.push("entity_of_interest=" + encodeURIComponent(String(t))), o !== void 0 && S.push("characteristic=" + encodeURIComponent(String(o))), r !== void 0 && S.push("method=" + encodeURIComponent(String(r))), i !== void 0 && S.push("unit=" + encodeURIComponent(String(i))), s !== void 0 && S.push("group_of_variables=" + encodeURIComponent(String(s))), a !== void 0 && S.push("not_included_in_group_of_variables=" + encodeURIComponent(String(a))), p !== void 0 && S.push("data_type=" + encodeURIComponent(String(p))), c !== void 0 && S.push("time_interval=" + encodeURIComponent(String(c))), l && l.forEach((F) => {
      S.push("species=" + encodeURIComponent(String(F)));
    }), g !== void 0 && S.push("withAssociatedData=" + encodeURIComponent(String(g))), P && P.forEach((F) => {
      S.push("experiments=" + encodeURIComponent(String(F)));
    }), m && m.forEach((F) => {
      S.push("scientific_objects=" + encodeURIComponent(String(F)));
    }), y && y.forEach((F) => {
      S.push("devices=" + encodeURIComponent(String(F)));
    }), R && R.forEach((F) => {
      S.push("order_by=" + encodeURIComponent(String(F)));
    }), f !== void 0 && S.push("page=" + encodeURIComponent(String(f))), C !== void 0 && S.push("page_size=" + encodeURIComponent(String(C))), $ !== void 0 && S.push("sharedResourceInstance=" + encodeURIComponent(String($))), D.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables${S.length > 0 ? "?" + S.join("&") : ""}`, D);
  }
  /**
   * Search detailed variables by name, long name, entity, characteristic, method or unit name
   * The following fields could be used for sorting :    _entity_name : the name of the variable entity   _characteristic_name : the name of the variable characteristic   _method_name : the name of the variable method   _unit_name : the name of the variable unit  
   * @param Authorization Authentication token
   * @param name Name regex pattern
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  searchVariablesDetailsPath() {
    return "/core/variables/details";
  }
  searchVariablesDetails(e, n, t, o, r = "body", i = {}) {
    let s = [];
    return e !== void 0 && s.push("name=" + encodeURIComponent(String(e))), n && n.forEach((p) => {
      s.push("order_by=" + encodeURIComponent(String(p)));
    }), t !== void 0 && s.push("page=" + encodeURIComponent(String(t))), o !== void 0 && s.push("page_size=" + encodeURIComponent(String(o))), i.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables/details${s.length > 0 ? "?" + s.join("&") : ""}`, i);
  }
  /**
   * Search variables groups
   * 
   * @param Authorization Authentication token
   * @param name Regex pattern for filtering by name
   * @param variableUri Variable URI
   * @param order_by List of fields to sort as an array of fieldName&#x3D;asc|desc
   * @param page Page number
   * @param page_size Page size
   * @param sharedResourceInstance Shared resource instance
   * @param Accept_Language Request accepted language
   
   */
  searchVariablesGroupsPath() {
    return "/core/variables_group";
  }
  searchVariablesGroups(e, n, t, o, r, i, s = "body", a = {}) {
    let p = [];
    return e !== void 0 && p.push("name=" + encodeURIComponent(String(e))), n !== void 0 && p.push("variableUri=" + encodeURIComponent(String(n))), t && t.forEach((l) => {
      p.push("order_by=" + encodeURIComponent(String(l)));
    }), o !== void 0 && p.push("page=" + encodeURIComponent(String(o))), r !== void 0 && p.push("page_size=" + encodeURIComponent(String(r))), i !== void 0 && p.push("sharedResourceInstance=" + encodeURIComponent(String(i))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/core/variables_group${p.length > 0 ? "?" + p.join("&") : ""}`, a);
  }
  /**
   * Update a characteristic
   * 
   * @param Authorization Authentication token
   * @param body Characteristic description
   * @param Accept_Language Request accepted language
   
   */
  updateCharacteristicPath() {
    return "/core/characteristics";
  }
  updateCharacteristic(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/characteristics`, e, t);
  }
  /**
   * Update an entity
   * 
   * @param Authorization Authentication token
   * @param body Entity description
   * @param Accept_Language Request accepted language
   
   */
  updateEntityPath() {
    return "/core/entities";
  }
  updateEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/entities`, e, t);
  }
  /**
   * Update an entity of interest
   * 
   * @param Authorization Authentication token
   * @param body Entity of interest description
   * @param Accept_Language Request accepted language
   
   */
  updateInterestEntityPath() {
    return "/core/entities_of_interest";
  }
  updateInterestEntity(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/entities_of_interest`, e, t);
  }
  /**
   * Update a method
   * 
   * @param Authorization Authentication token
   * @param body Method description
   * @param Accept_Language Request accepted language
   
   */
  updateMethodPath() {
    return "/core/methods";
  }
  updateMethod(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/methods`, e, t);
  }
  /**
   * Update an unit
   * 
   * @param Authorization Authentication token
   * @param body Unit description
   * @param Accept_Language Request accepted language
   
   */
  updateUnitPath() {
    return "/core/units";
  }
  updateUnit(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/units`, e, t);
  }
  /**
   * Update a variable
   * 
   * @param Authorization Authentication token
   * @param body Variable description
   * @param Accept_Language Request accepted language
   
   */
  updateVariablePath() {
    return "/core/variables";
  }
  updateVariable(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/variables`, e, t);
  }
  /**
   * Update a variables group
   * 
   * @param Authorization Authentication token
   * @param body Variables group description
   * @param Accept_Language Request accepted language
   
   */
  updateVariablesGroupPath() {
    return "/core/variables_group";
  }
  updateVariablesGroup(e, n = "body", t = {}) {
    return t.Accept = "application/json", t["Content-Type"] = "application/json", this.httpClient.put(`${this.basePath}/core/variables_group`, e, t);
  }
};
Ce = Rn([
  x(),
  Ke(0, A("IApiHttpClient")),
  Ke(1, A("IAPIConfiguration"))
], Ce);
var et;
((e) => {
  e.TypeEnum = {
    Name: "name",
    Link: "link"
  };
})(et || (et = {}));
var tt;
((e) => {
  e.OrderEnum = {
    ASCENDING: "ASCENDING",
    DESCENDING: "DESCENDING"
  };
})(tt || (tt = {}));
var nt;
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(nt || (nt = {}));
const Gn = "", Mn = {
  csv: ",",
  tsv: "   ",
  ssv: " ",
  pipes: "|"
};
class Fn {
  static with(n) {
    n.bind("AnnotationsService").to(te).inSingletonScope(), n.bind("AreaService").to(ne).inSingletonScope(), n.bind("DataService").to(oe).inSingletonScope(), n.bind("DevicesService").to(re).inSingletonScope(), n.bind("DocumentsService").to(ie).inSingletonScope(), n.bind("EventsService").to(se).inSingletonScope(), n.bind("ExperimentsService").to(ae).inSingletonScope(), n.bind("FactorsService").to(pe).inSingletonScope(), n.bind("GermplasmService").to(ce).inSingletonScope(), n.bind("MetricsService").to(ue).inSingletonScope(), n.bind("OntologyService").to(he).inSingletonScope(), n.bind("OrganizationsService").to(de).inSingletonScope(), n.bind("PositionsService").to(fe).inSingletonScope(), n.bind("ProjectsService").to(le).inSingletonScope(), n.bind("ScientificObjectsService").to(ge).inSingletonScope(), n.bind("SpeciesService").to(me).inSingletonScope(), n.bind("SystemService").to(be).inSingletonScope(), n.bind("VariablesService").to(Ce).inSingletonScope();
  }
}
var B = typeof globalThis < "u" && globalThis || typeof self < "u" && self || // eslint-disable-next-line no-undef
typeof global < "u" && global || {}, M = {
  searchParams: "URLSearchParams" in B,
  iterable: "Symbol" in B && "iterator" in Symbol,
  blob: "FileReader" in B && "Blob" in B && function() {
    try {
      return new Blob(), !0;
    } catch {
      return !1;
    }
  }(),
  formData: "FormData" in B,
  arrayBuffer: "ArrayBuffer" in B
};
function vn(e) {
  return e && DataView.prototype.isPrototypeOf(e);
}
if (M.arrayBuffer)
  var Un = [
    "[object Int8Array]",
    "[object Uint8Array]",
    "[object Uint8ClampedArray]",
    "[object Int16Array]",
    "[object Uint16Array]",
    "[object Int32Array]",
    "[object Uint32Array]",
    "[object Float32Array]",
    "[object Float64Array]"
  ], $n = ArrayBuffer.isView || function(e) {
    return e && Un.indexOf(Object.prototype.toString.call(e)) > -1;
  };
function J(e) {
  if (typeof e != "string" && (e = String(e)), /[^a-z0-9\-#$%&'*+.^_`|~!]/i.test(e) || e === "")
    throw new TypeError('Invalid character in header field name: "' + e + '"');
  return e.toLowerCase();
}
function ye(e) {
  return typeof e != "string" && (e = String(e)), e;
}
function Pe(e) {
  var n = {
    next: function() {
      var t = e.shift();
      return { done: t === void 0, value: t };
    }
  };
  return M.iterable && (n[Symbol.iterator] = function() {
    return n;
  }), n;
}
function T(e) {
  this.map = {}, e instanceof T ? e.forEach(function(n, t) {
    this.append(t, n);
  }, this) : Array.isArray(e) ? e.forEach(function(n) {
    if (n.length != 2)
      throw new TypeError("Headers constructor: expected name/value pair to be length 2, found" + n.length);
    this.append(n[0], n[1]);
  }, this) : e && Object.getOwnPropertyNames(e).forEach(function(n) {
    this.append(n, e[n]);
  }, this);
}
T.prototype.append = function(e, n) {
  e = J(e), n = ye(n);
  var t = this.map[e];
  this.map[e] = t ? t + ", " + n : n;
};
T.prototype.delete = function(e) {
  delete this.map[J(e)];
};
T.prototype.get = function(e) {
  return e = J(e), this.has(e) ? this.map[e] : null;
};
T.prototype.has = function(e) {
  return this.map.hasOwnProperty(J(e));
};
T.prototype.set = function(e, n) {
  this.map[J(e)] = ye(n);
};
T.prototype.forEach = function(e, n) {
  for (var t in this.map)
    this.map.hasOwnProperty(t) && e.call(n, this.map[t], t, this);
};
T.prototype.keys = function() {
  var e = [];
  return this.forEach(function(n, t) {
    e.push(t);
  }), Pe(e);
};
T.prototype.values = function() {
  var e = [];
  return this.forEach(function(n) {
    e.push(n);
  }), Pe(e);
};
T.prototype.entries = function() {
  var e = [];
  return this.forEach(function(n, t) {
    e.push([t, n]);
  }), Pe(e);
};
M.iterable && (T.prototype[Symbol.iterator] = T.prototype.entries);
function ee(e) {
  if (!e._noBody) {
    if (e.bodyUsed)
      return Promise.reject(new TypeError("Already read"));
    e.bodyUsed = !0;
  }
}
function at(e) {
  return new Promise(function(n, t) {
    e.onload = function() {
      n(e.result);
    }, e.onerror = function() {
      t(e.error);
    };
  });
}
function jn(e) {
  var n = new FileReader(), t = at(n);
  return n.readAsArrayBuffer(e), t;
}
function _n(e) {
  var n = new FileReader(), t = at(n), o = /charset=([A-Za-z0-9_-]+)/.exec(e.type), r = o ? o[1] : "utf-8";
  return n.readAsText(e, r), t;
}
function An(e) {
  for (var n = new Uint8Array(e), t = new Array(n.length), o = 0; o < n.length; o++)
    t[o] = String.fromCharCode(n[o]);
  return t.join("");
}
function ot(e) {
  if (e.slice)
    return e.slice(0);
  var n = new Uint8Array(e.byteLength);
  return n.set(new Uint8Array(e)), n.buffer;
}
function pt() {
  return this.bodyUsed = !1, this._initBody = function(e) {
    this.bodyUsed = this.bodyUsed, this._bodyInit = e, e ? typeof e == "string" ? this._bodyText = e : M.blob && Blob.prototype.isPrototypeOf(e) ? this._bodyBlob = e : M.formData && FormData.prototype.isPrototypeOf(e) ? this._bodyFormData = e : M.searchParams && URLSearchParams.prototype.isPrototypeOf(e) ? this._bodyText = e.toString() : M.arrayBuffer && M.blob && vn(e) ? (this._bodyArrayBuffer = ot(e.buffer), this._bodyInit = new Blob([this._bodyArrayBuffer])) : M.arrayBuffer && (ArrayBuffer.prototype.isPrototypeOf(e) || $n(e)) ? this._bodyArrayBuffer = ot(e) : this._bodyText = e = Object.prototype.toString.call(e) : (this._noBody = !0, this._bodyText = ""), this.headers.get("content-type") || (typeof e == "string" ? this.headers.set("content-type", "text/plain;charset=UTF-8") : this._bodyBlob && this._bodyBlob.type ? this.headers.set("content-type", this._bodyBlob.type) : M.searchParams && URLSearchParams.prototype.isPrototypeOf(e) && this.headers.set("content-type", "application/x-www-form-urlencoded;charset=UTF-8"));
  }, M.blob && (this.blob = function() {
    var e = ee(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return Promise.resolve(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(new Blob([this._bodyArrayBuffer]));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as blob");
    return Promise.resolve(new Blob([this._bodyText]));
  }), this.arrayBuffer = function() {
    if (this._bodyArrayBuffer) {
      var e = ee(this);
      return e || (ArrayBuffer.isView(this._bodyArrayBuffer) ? Promise.resolve(
        this._bodyArrayBuffer.buffer.slice(
          this._bodyArrayBuffer.byteOffset,
          this._bodyArrayBuffer.byteOffset + this._bodyArrayBuffer.byteLength
        )
      ) : Promise.resolve(this._bodyArrayBuffer));
    } else {
      if (M.blob)
        return this.blob().then(jn);
      throw new Error("could not read as ArrayBuffer");
    }
  }, this.text = function() {
    var e = ee(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return _n(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(An(this._bodyArrayBuffer));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as text");
    return Promise.resolve(this._bodyText);
  }, M.formData && (this.formData = function() {
    return this.text().then(Dn);
  }), this.json = function() {
    return this.text().then(JSON.parse);
  }, this;
}
var wn = ["CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE"];
function En(e) {
  var n = e.toUpperCase();
  return wn.indexOf(n) > -1 ? n : e;
}
function z(e, n) {
  if (!(this instanceof z))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  n = n || {};
  var t = n.body;
  if (e instanceof z) {
    if (e.bodyUsed)
      throw new TypeError("Already read");
    this.url = e.url, this.credentials = e.credentials, n.headers || (this.headers = new T(e.headers)), this.method = e.method, this.mode = e.mode, this.signal = e.signal, !t && e._bodyInit != null && (t = e._bodyInit, e.bodyUsed = !0);
  } else
    this.url = String(e);
  if (this.credentials = n.credentials || this.credentials || "same-origin", (n.headers || !this.headers) && (this.headers = new T(n.headers)), this.method = En(n.method || this.method || "GET"), this.mode = n.mode || this.mode || null, this.signal = n.signal || this.signal || function() {
    if ("AbortController" in B) {
      var i = new AbortController();
      return i.signal;
    }
  }(), this.referrer = null, (this.method === "GET" || this.method === "HEAD") && t)
    throw new TypeError("Body not allowed for GET or HEAD requests");
  if (this._initBody(t), (this.method === "GET" || this.method === "HEAD") && (n.cache === "no-store" || n.cache === "no-cache")) {
    var o = /([?&])_=[^&]*/;
    if (o.test(this.url))
      this.url = this.url.replace(o, "$1_=" + (/* @__PURE__ */ new Date()).getTime());
    else {
      var r = /\?/;
      this.url += (r.test(this.url) ? "&" : "?") + "_=" + (/* @__PURE__ */ new Date()).getTime();
    }
  }
}
z.prototype.clone = function() {
  return new z(this, { body: this._bodyInit });
};
function Dn(e) {
  var n = new FormData();
  return e.trim().split("&").forEach(function(t) {
    if (t) {
      var o = t.split("="), r = o.shift().replace(/\+/g, " "), i = o.join("=").replace(/\+/g, " ");
      n.append(decodeURIComponent(r), decodeURIComponent(i));
    }
  }), n;
}
function On(e) {
  var n = new T(), t = e.replace(/\r?\n[\t ]+/g, " ");
  return t.split("\r").map(function(o) {
    return o.indexOf(`
`) === 0 ? o.substr(1, o.length) : o;
  }).forEach(function(o) {
    var r = o.split(":"), i = r.shift().trim();
    if (i) {
      var s = r.join(":").trim();
      try {
        n.append(i, s);
      } catch (a) {
        console.warn("Response " + a.message);
      }
    }
  }), n;
}
pt.call(z.prototype);
function V(e, n) {
  if (!(this instanceof V))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  if (n || (n = {}), this.type = "default", this.status = n.status === void 0 ? 200 : n.status, this.status < 200 || this.status > 599)
    throw new RangeError("Failed to construct 'Response': The status provided (0) is outside the range [200, 599].");
  this.ok = this.status >= 200 && this.status < 300, this.statusText = n.statusText === void 0 ? "" : "" + n.statusText, this.headers = new T(n.headers), this.url = n.url || "", this._initBody(e);
}
pt.call(V.prototype);
V.prototype.clone = function() {
  return new V(this._bodyInit, {
    status: this.status,
    statusText: this.statusText,
    headers: new T(this.headers),
    url: this.url
  });
};
V.error = function() {
  var e = new V(null, { status: 200, statusText: "" });
  return e.ok = !1, e.status = 0, e.type = "error", e;
};
var Tn = [301, 302, 303, 307, 308];
V.redirect = function(e, n) {
  if (Tn.indexOf(n) === -1)
    throw new RangeError("Invalid status code");
  return new V(null, { status: n, headers: { location: e } });
};
var N = B.DOMException;
try {
  new N();
} catch {
  N = function(n, t) {
    this.message = n, this.name = t;
    var o = Error(n);
    this.stack = o.stack;
  }, N.prototype = Object.create(Error.prototype), N.prototype.constructor = N;
}
function ct(e, n) {
  return new Promise(function(t, o) {
    var r = new z(e, n);
    if (r.signal && r.signal.aborted)
      return o(new N("Aborted", "AbortError"));
    var i = new XMLHttpRequest();
    function s() {
      i.abort();
    }
    i.onload = function() {
      var c = {
        statusText: i.statusText,
        headers: On(i.getAllResponseHeaders() || "")
      };
      r.url.indexOf("file://") === 0 && (i.status < 200 || i.status > 599) ? c.status = 200 : c.status = i.status, c.url = "responseURL" in i ? i.responseURL : c.headers.get("X-Request-URL");
      var l = "response" in i ? i.response : i.responseText;
      setTimeout(function() {
        t(new V(l, c));
      }, 0);
    }, i.onerror = function() {
      setTimeout(function() {
        o(new TypeError("Network request failed"));
      }, 0);
    }, i.ontimeout = function() {
      setTimeout(function() {
        o(new TypeError("Network request timed out"));
      }, 0);
    }, i.onabort = function() {
      setTimeout(function() {
        o(new N("Aborted", "AbortError"));
      }, 0);
    };
    function a(c) {
      try {
        return c === "" && B.location.href ? B.location.href : c;
      } catch {
        return c;
      }
    }
    if (i.open(r.method, a(r.url), !0), r.credentials === "include" ? i.withCredentials = !0 : r.credentials === "omit" && (i.withCredentials = !1), "responseType" in i && (M.blob ? i.responseType = "blob" : M.arrayBuffer && (i.responseType = "arraybuffer")), n && typeof n.headers == "object" && !(n.headers instanceof T || B.Headers && n.headers instanceof B.Headers)) {
      var p = [];
      Object.getOwnPropertyNames(n.headers).forEach(function(c) {
        p.push(J(c)), i.setRequestHeader(c, ye(n.headers[c]));
      }), r.headers.forEach(function(c, l) {
        p.indexOf(l) === -1 && i.setRequestHeader(l, c);
      });
    } else
      r.headers.forEach(function(c, l) {
        i.setRequestHeader(l, c);
      });
    r.signal && (r.signal.addEventListener("abort", s), i.onreadystatechange = function() {
      i.readyState === 4 && r.signal.removeEventListener("abort", s);
    }), i.send(typeof r._bodyInit > "u" ? null : r._bodyInit);
  });
}
ct.polyfill = !0;
B.fetch || (B.fetch = ct, B.Headers = T, B.Request = z, B.Response = V);
class xn {
  constructor(n, t, o) {
    this.response = n, this.status = t, this.headers = o;
  }
}
class rt {
  constructor(n, t, o, r) {
    this.message = n, this.level = t, this.translationKey = o, this.translationValues = r;
  }
}
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(rt || (rt = {}));
var qn = Object.getOwnPropertyDescriptor, Bn = (e, n, t, o) => {
  for (var r = o > 1 ? void 0 : o ? qn(n, t) : n, i = e.length - 1, s; i >= 0; i--)
    (s = e[i]) && (r = s(r) || r);
  return r;
};
let it = class {
  get(e, n) {
    return this.performNetworkCall(e, "get", void 0, n);
  }
  post(e, n, t) {
    return this.performNetworkCall(e, "post", this.getJsonBody(n), this.addJsonHeaders(t));
  }
  put(e, n, t) {
    return this.performNetworkCall(e, "put", this.getJsonBody(n), this.addJsonHeaders(t));
  }
  delete(e, n) {
    return this.performNetworkCall(e, "delete", void 0, n);
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
  performNetworkCall(e, n, t, o) {
    let r;
    return typeof window > "u" ? r = require("node-fetch") : r = window.fetch, r(e, {
      method: n,
      body: t,
      mode: "cors",
      headers: o
    }).then((s) => {
      let a = {};
      return s.headers.forEach((p, c) => {
        a[c.toString().toLowerCase()] = p;
      }), s.text().then((p) => {
        let c = a["content-type"] || "", l;
        c.match("application/json") ? (l = JSON.parse(p), l.metadata && l.metadata) : l = p;
        let g = new xn(l, s.status, a);
        if (s.status >= 400)
          throw g;
        return g;
      });
    });
  }
};
it = Bn([
  x()
], it);
export {
  te as AnnotationsService,
  Fn as ApiServiceBinder,
  ne as AreaService,
  Mn as COLLECTION_FORMATS,
  et as Crs,
  oe as DataService,
  re as DevicesService,
  ie as DocumentsService,
  se as EventsService,
  ae as ExperimentsService,
  pe as FactorsService,
  ce as GermplasmService,
  ue as MetricsService,
  he as OntologyService,
  tt as OrderBy,
  de as OrganizationsService,
  fe as PositionsService,
  le as ProjectsService,
  ge as ScientificObjectsService,
  me as SpeciesService,
  nt as StatusDTO,
  be as SystemService,
  Ce as VariablesService,
  Gn as __fakeExport
};
