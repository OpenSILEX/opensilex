var ie = typeof globalThis < "u" ? globalThis : typeof window < "u" ? window : typeof global < "u" ? global : typeof self < "u" ? self : {}, oe = {};
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
var ae;
function Je() {
  if (ae) return oe;
  ae = 1;
  var e;
  return function(t) {
    (function(i) {
      var s = typeof ie == "object" ? ie : typeof self == "object" ? self : typeof this == "object" ? this : Function("return this;")(), u = a(t);
      typeof s.Reflect > "u" ? s.Reflect = t : u = a(s.Reflect, u), i(u);
      function a(d, g) {
        return function(l, v) {
          typeof d[l] != "function" && Object.defineProperty(d, l, { configurable: !0, writable: !0, value: v }), g && g(l, v);
        };
      }
    })(function(i) {
      var s = Object.prototype.hasOwnProperty, u = typeof Symbol == "function", a = u && typeof Symbol.toPrimitive < "u" ? Symbol.toPrimitive : "@@toPrimitive", d = u && typeof Symbol.iterator < "u" ? Symbol.iterator : "@@iterator", g = typeof Object.create == "function", l = { __proto__: [] } instanceof Array, v = !g && !l, w = {
        // create an object in dictionary mode (a.k.a. "slow" mode in v8)
        create: g ? function() {
          return F(/* @__PURE__ */ Object.create(null));
        } : l ? function() {
          return F({ __proto__: null });
        } : function() {
          return F({});
        },
        has: v ? function(r, n) {
          return s.call(r, n);
        } : function(r, n) {
          return n in r;
        },
        get: v ? function(r, n) {
          return s.call(r, n) ? r[n] : void 0;
        } : function(r, n) {
          return r[n];
        }
      }, O = Object.getPrototypeOf(Function), N = typeof process == "object" && process.env && process.env.REFLECT_METADATA_USE_MAP_POLYFILL === "true", $ = !N && typeof Map == "function" && typeof Map.prototype.entries == "function" ? Map : He(), ge = !N && typeof Set == "function" && typeof Set.prototype.entries == "function" ? Set : qe(), we = !N && typeof WeakMap == "function" ? WeakMap : Ve(), k = new we();
      function _e(r, n, o, f) {
        if (_(o)) {
          if (!ee(r))
            throw new TypeError();
          if (!te(n))
            throw new TypeError();
          return Ue(r, n);
        } else {
          if (!ee(r))
            throw new TypeError();
          if (!I(n))
            throw new TypeError();
          if (!I(f) && !_(f) && !B(f))
            throw new TypeError();
          return B(f) && (f = void 0), o = P(o), Me(r, n, o, f);
        }
      }
      i("decorate", _e);
      function Ae(r, n) {
        function o(f, h) {
          if (!I(f))
            throw new TypeError();
          if (!_(h) && !Ne(h))
            throw new TypeError();
          X(r, n, f, h);
        }
        return o;
      }
      i("metadata", Ae);
      function Re(r, n, o, f) {
        if (!I(o))
          throw new TypeError();
        return _(f) || (f = P(f)), X(r, n, o, f);
      }
      i("defineMetadata", Re);
      function Se(r, n, o) {
        if (!I(n))
          throw new TypeError();
        return _(o) || (o = P(o)), W(r, n, o);
      }
      i("hasMetadata", Se);
      function Ie(r, n, o) {
        if (!I(n))
          throw new TypeError();
        return _(o) || (o = P(o)), G(r, n, o);
      }
      i("hasOwnMetadata", Ie);
      function Ee(r, n, o) {
        if (!I(n))
          throw new TypeError();
        return _(o) || (o = P(o)), z(r, n, o);
      }
      i("getMetadata", Ee);
      function Te(r, n, o) {
        if (!I(n))
          throw new TypeError();
        return _(o) || (o = P(o)), Y(r, n, o);
      }
      i("getOwnMetadata", Te);
      function Pe(r, n) {
        if (!I(r))
          throw new TypeError();
        return _(n) || (n = P(n)), Z(r, n);
      }
      i("getMetadataKeys", Pe);
      function Oe(r, n) {
        if (!I(r))
          throw new TypeError();
        return _(n) || (n = P(n)), Q(r, n);
      }
      i("getOwnMetadataKeys", Oe);
      function Ce(r, n, o) {
        if (!I(n))
          throw new TypeError();
        _(o) || (o = P(o));
        var f = j(
          n,
          o,
          /*Create*/
          !1
        );
        if (_(f) || !f.delete(r))
          return !1;
        if (f.size > 0)
          return !0;
        var h = k.get(n);
        return h.delete(o), h.size > 0 || k.delete(n), !0;
      }
      i("deleteMetadata", Ce);
      function Ue(r, n) {
        for (var o = r.length - 1; o >= 0; --o) {
          var f = r[o], h = f(n);
          if (!_(h) && !B(h)) {
            if (!te(h))
              throw new TypeError();
            n = h;
          }
        }
        return n;
      }
      function Me(r, n, o, f) {
        for (var h = r.length - 1; h >= 0; --h) {
          var R = r[h], p = R(n, o, f);
          if (!_(p) && !B(p)) {
            if (!I(p))
              throw new TypeError();
            f = p;
          }
        }
        return f;
      }
      function j(r, n, o) {
        var f = k.get(r);
        if (_(f)) {
          if (!o)
            return;
          f = new $(), k.set(r, f);
        }
        var h = f.get(n);
        if (_(h)) {
          if (!o)
            return;
          h = new $(), f.set(n, h);
        }
        return h;
      }
      function W(r, n, o) {
        var f = G(r, n, o);
        if (f)
          return !0;
        var h = L(n);
        return B(h) ? !1 : W(r, h, o);
      }
      function G(r, n, o) {
        var f = j(
          n,
          o,
          /*Create*/
          !1
        );
        return _(f) ? !1 : ke(f.has(r));
      }
      function z(r, n, o) {
        var f = G(r, n, o);
        if (f)
          return Y(r, n, o);
        var h = L(n);
        if (!B(h))
          return z(r, h, o);
      }
      function Y(r, n, o) {
        var f = j(
          n,
          o,
          /*Create*/
          !1
        );
        if (!_(f))
          return f.get(r);
      }
      function X(r, n, o, f) {
        var h = j(
          o,
          f,
          /*Create*/
          !0
        );
        h.set(r, n);
      }
      function Z(r, n) {
        var o = Q(r, n), f = L(r);
        if (f === null)
          return o;
        var h = Z(f, n);
        if (h.length <= 0)
          return o;
        if (o.length <= 0)
          return h;
        for (var R = new ge(), p = [], y = 0, c = o; y < c.length; y++) {
          var b = c[y], m = R.has(b);
          m || (R.add(b), p.push(b));
        }
        for (var C = 0, ne = h; C < ne.length; C++) {
          var b = ne[C], m = R.has(b);
          m || (R.add(b), p.push(b));
        }
        return p;
      }
      function Q(r, n) {
        var o = [], f = j(
          r,
          n,
          /*Create*/
          !1
        );
        if (_(f))
          return o;
        for (var h = f.keys(), R = $e(h), p = 0; ; ) {
          var y = Le(R);
          if (!y)
            return o.length = p, o;
          var c = Ge(y);
          try {
            o[p] = c;
          } catch (b) {
            try {
              Fe(R);
            } finally {
              throw b;
            }
          }
          p++;
        }
      }
      function K(r) {
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
      function _(r) {
        return r === void 0;
      }
      function B(r) {
        return r === null;
      }
      function Be(r) {
        return typeof r == "symbol";
      }
      function I(r) {
        return typeof r == "object" ? r !== null : typeof r == "function";
      }
      function De(r, n) {
        switch (K(r)) {
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
        var o = "string", f = re(r, a);
        if (f !== void 0) {
          var h = f.call(r, o);
          if (I(h))
            throw new TypeError();
          return h;
        }
        return je(r);
      }
      function je(r, n) {
        var o, f, h;
        {
          var R = r.toString;
          if (x(R)) {
            var f = R.call(r);
            if (!I(f))
              return f;
          }
          var o = r.valueOf;
          if (x(o)) {
            var f = o.call(r);
            if (!I(f))
              return f;
          }
        }
        throw new TypeError();
      }
      function ke(r) {
        return !!r;
      }
      function xe(r) {
        return "" + r;
      }
      function P(r) {
        var n = De(r);
        return Be(n) ? n : xe(n);
      }
      function ee(r) {
        return Array.isArray ? Array.isArray(r) : r instanceof Object ? r instanceof Array : Object.prototype.toString.call(r) === "[object Array]";
      }
      function x(r) {
        return typeof r == "function";
      }
      function te(r) {
        return typeof r == "function";
      }
      function Ne(r) {
        switch (K(r)) {
          case 3:
            return !0;
          case 4:
            return !0;
          default:
            return !1;
        }
      }
      function re(r, n) {
        var o = r[n];
        if (o != null) {
          if (!x(o))
            throw new TypeError();
          return o;
        }
      }
      function $e(r) {
        var n = re(r, d);
        if (!x(n))
          throw new TypeError();
        var o = n.call(r);
        if (!I(o))
          throw new TypeError();
        return o;
      }
      function Ge(r) {
        return r.value;
      }
      function Le(r) {
        var n = r.next();
        return n.done ? !1 : n;
      }
      function Fe(r) {
        var n = r.return;
        n && n.call(r);
      }
      function L(r) {
        var n = Object.getPrototypeOf(r);
        if (typeof r != "function" || r === O || n !== O)
          return n;
        var o = r.prototype, f = o && Object.getPrototypeOf(o);
        if (f == null || f === Object.prototype)
          return n;
        var h = f.constructor;
        return typeof h != "function" || h === r ? n : h;
      }
      function He() {
        var r = {}, n = [], o = (
          /** @class */
          function() {
            function p(y, c, b) {
              this._index = 0, this._keys = y, this._values = c, this._selector = b;
            }
            return p.prototype["@@iterator"] = function() {
              return this;
            }, p.prototype[d] = function() {
              return this;
            }, p.prototype.next = function() {
              var y = this._index;
              if (y >= 0 && y < this._keys.length) {
                var c = this._selector(this._keys[y], this._values[y]);
                return y + 1 >= this._keys.length ? (this._index = -1, this._keys = n, this._values = n) : this._index++, { value: c, done: !1 };
              }
              return { value: void 0, done: !0 };
            }, p.prototype.throw = function(y) {
              throw this._index >= 0 && (this._index = -1, this._keys = n, this._values = n), y;
            }, p.prototype.return = function(y) {
              return this._index >= 0 && (this._index = -1, this._keys = n, this._values = n), { value: y, done: !0 };
            }, p;
          }()
        );
        return (
          /** @class */
          function() {
            function p() {
              this._keys = [], this._values = [], this._cacheKey = r, this._cacheIndex = -2;
            }
            return Object.defineProperty(p.prototype, "size", {
              get: function() {
                return this._keys.length;
              },
              enumerable: !0,
              configurable: !0
            }), p.prototype.has = function(y) {
              return this._find(
                y,
                /*insert*/
                !1
              ) >= 0;
            }, p.prototype.get = function(y) {
              var c = this._find(
                y,
                /*insert*/
                !1
              );
              return c >= 0 ? this._values[c] : void 0;
            }, p.prototype.set = function(y, c) {
              var b = this._find(
                y,
                /*insert*/
                !0
              );
              return this._values[b] = c, this;
            }, p.prototype.delete = function(y) {
              var c = this._find(
                y,
                /*insert*/
                !1
              );
              if (c >= 0) {
                for (var b = this._keys.length, m = c + 1; m < b; m++)
                  this._keys[m - 1] = this._keys[m], this._values[m - 1] = this._values[m];
                return this._keys.length--, this._values.length--, y === this._cacheKey && (this._cacheKey = r, this._cacheIndex = -2), !0;
              }
              return !1;
            }, p.prototype.clear = function() {
              this._keys.length = 0, this._values.length = 0, this._cacheKey = r, this._cacheIndex = -2;
            }, p.prototype.keys = function() {
              return new o(this._keys, this._values, f);
            }, p.prototype.values = function() {
              return new o(this._keys, this._values, h);
            }, p.prototype.entries = function() {
              return new o(this._keys, this._values, R);
            }, p.prototype["@@iterator"] = function() {
              return this.entries();
            }, p.prototype[d] = function() {
              return this.entries();
            }, p.prototype._find = function(y, c) {
              return this._cacheKey !== y && (this._cacheIndex = this._keys.indexOf(this._cacheKey = y)), this._cacheIndex < 0 && c && (this._cacheIndex = this._keys.length, this._keys.push(y), this._values.push(void 0)), this._cacheIndex;
            }, p;
          }()
        );
        function f(p, y) {
          return p;
        }
        function h(p, y) {
          return y;
        }
        function R(p, y) {
          return [p, y];
        }
      }
      function qe() {
        return (
          /** @class */
          function() {
            function r() {
              this._map = new $();
            }
            return Object.defineProperty(r.prototype, "size", {
              get: function() {
                return this._map.size;
              },
              enumerable: !0,
              configurable: !0
            }), r.prototype.has = function(n) {
              return this._map.has(n);
            }, r.prototype.add = function(n) {
              return this._map.set(n, n), this;
            }, r.prototype.delete = function(n) {
              return this._map.delete(n);
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
      function Ve() {
        var r = 16, n = w.create(), o = f();
        return (
          /** @class */
          function() {
            function c() {
              this._key = f();
            }
            return c.prototype.has = function(b) {
              var m = h(
                b,
                /*create*/
                !1
              );
              return m !== void 0 ? w.has(m, this._key) : !1;
            }, c.prototype.get = function(b) {
              var m = h(
                b,
                /*create*/
                !1
              );
              return m !== void 0 ? w.get(m, this._key) : void 0;
            }, c.prototype.set = function(b, m) {
              var C = h(
                b,
                /*create*/
                !0
              );
              return C[this._key] = m, this;
            }, c.prototype.delete = function(b) {
              var m = h(
                b,
                /*create*/
                !1
              );
              return m !== void 0 ? delete m[this._key] : !1;
            }, c.prototype.clear = function() {
              this._key = f();
            }, c;
          }()
        );
        function f() {
          var c;
          do
            c = "@@WeakMap@@" + y();
          while (w.has(n, c));
          return n[c] = !0, c;
        }
        function h(c, b) {
          if (!s.call(c, o)) {
            if (!b)
              return;
            Object.defineProperty(c, o, { value: w.create() });
          }
          return c[o];
        }
        function R(c, b) {
          for (var m = 0; m < b; ++m)
            c[m] = Math.random() * 255 | 0;
          return c;
        }
        function p(c) {
          return typeof Uint8Array == "function" ? typeof crypto < "u" ? crypto.getRandomValues(new Uint8Array(c)) : typeof msCrypto < "u" ? msCrypto.getRandomValues(new Uint8Array(c)) : R(new Uint8Array(c), c) : R(new Array(c), c);
        }
        function y() {
          var c = p(r);
          c[6] = c[6] & 79 | 64, c[8] = c[8] & 191 | 128;
          for (var b = "", m = 0; m < r; ++m) {
            var C = c[m];
            (m === 4 || m === 6 || m === 8) && (b += "-"), C < 16 && (b += "0"), b += C.toString(16).toLowerCase();
          }
          return b;
        }
      }
      function F(r) {
        return r.__ = void 0, delete r.__, r;
      }
    });
  }(e || (e = {})), oe;
}
Je();
var We = "named", ze = "inject", Ye = "inversify:tagged", Xe = "inversify:tagged_props", se = "inversify:paramtypes", Ze = "design:paramtypes", Qe = "Cannot apply @injectable decorator multiple times.", Ke = "Metadata key was used more than once in a parameter:", et = function(e) {
  return "@inject called with undefined this could mean that the class " + e + " has a circular dependency problem. You can use a LazyServiceIdentifer to  overcome this limitation.";
}, tt = "The @inject @multiInject @tagged and @named decorators must be applied to the parameters of a class constructor or a class property.", rt = function() {
  function e(t, i) {
    this.key = t, this.value = i;
  }
  return e.prototype.toString = function() {
    return this.key === We ? "named: " + this.value.toString() + " " : "tagged: { key:" + this.key.toString() + ", value: " + this.value + " }";
  }, e;
}();
function nt(e, t, i, s) {
  var u = Ye;
  pe(u, e, t, s, i);
}
function it(e, t, i) {
  var s = Xe;
  pe(s, e.constructor, t, i);
}
function pe(e, t, i, s, u) {
  var a = {}, d = typeof u == "number", g = u !== void 0 && d ? u.toString() : i;
  if (d && i !== void 0)
    throw new Error(tt);
  Reflect.hasOwnMetadata(e, t) && (a = Reflect.getMetadata(e, t));
  var l = a[g];
  if (!Array.isArray(l))
    l = [];
  else
    for (var v = 0, w = l; v < w.length; v++) {
      var O = w[v];
      if (O.key === s.key)
        throw new Error(Ke + " " + O.key.toString());
    }
  l.push(s), a[g] = l, Reflect.defineMetadata(e, a, t);
}
function ue(e) {
  return function(t, i, s) {
    if (e === void 0)
      throw new Error(et(t.name));
    var u = new rt(ze, e);
    typeof s == "number" ? nt(t, i, s, u) : it(t, i, u);
  };
}
function ye() {
  return function(e) {
    if (Reflect.hasOwnMetadata(se, e))
      throw new Error(Qe);
    var t = Reflect.getMetadata(Ze, e) || [];
    return Reflect.defineMetadata(se, t, e), e;
  };
}
var ot = Object.getOwnPropertyDescriptor, at = (e, t, i, s) => {
  for (var u = s > 1 ? void 0 : s ? ot(t, i) : t, a = e.length - 1, d; a >= 0; a--)
    (d = e[a]) && (u = d(u) || u);
  return u;
}, fe = (e, t) => (i, s) => t(i, s, e);
let q = class {
  constructor(e, t) {
    this.httpClient = e, this.APIConfiguration = t, this.basePath = "https://${host}", this.APIConfiguration.basePath && (this.basePath = this.APIConfiguration.basePath);
  }
  /**
   * Check the available BrAPI calls
   * Check the available BrAPI calls
   * @param page Page number
   * @param pageSize Page size
   * @param dataType datatype
   
   */
  getCallsPath() {
    return "/brapi/v1/calls";
  }
  getCalls(e, t, i, s = "body", u = {}) {
    let a = [];
    return e !== void 0 && a.push("page=" + encodeURIComponent(String(e))), t !== void 0 && a.push("pageSize=" + encodeURIComponent(String(t))), i !== void 0 && a.push("dataType=" + encodeURIComponent(String(i))), u.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/calls${a.length > 0 ? "?" + a.join("&") : ""}`, u);
  }
  /**
   * Submit a search request for germplasm (type accession in OpenSILEX
   * 
   * @param Authorization Authentication token
   * @param germplasmDbId Search by germplasmDbId (URI of an OpenSilex accession)
   * @param germplasmPUI Search by germplasmPUI (URI of an OpenSilex accession)
   * @param germplasmName Search by germplasmName (name of an OpenSilex accession)
   * @param commonCropName Search by commonCropName (name of the species of an OpenSilex accession)
   * @param page Page number
   * @param page_size Page size
   * @param Accept_Language Request accepted language
   
   */
  getGermplasmBySearchPath() {
    return "/brapi/v1/germplasm";
  }
  getGermplasmBySearch(e, t, i, s, u, a, d = "body", g = {}) {
    let l = [];
    return e !== void 0 && l.push("germplasmDbId=" + encodeURIComponent(String(e))), t !== void 0 && l.push("germplasmPUI=" + encodeURIComponent(String(t))), i !== void 0 && l.push("germplasmName=" + encodeURIComponent(String(i))), s !== void 0 && l.push("commonCropName=" + encodeURIComponent(String(s))), u !== void 0 && l.push("page=" + encodeURIComponent(String(u))), a !== void 0 && l.push("page_size=" + encodeURIComponent(String(a))), g.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/germplasm${l.length > 0 ? "?" + l.join("&") : ""}`, g);
  }
  /**
   * List all the observation units measured in the study.
   * List all the observation units measured in the study.
   * @param studyDbId studyDbId
   * @param Authorization Authentication token
   * @param observationLevel observationLevel
   * @param pageSize pageSize
   * @param page page
   * @param Accept_Language Request accepted language
   
   */
  getObservationUnitsPath() {
    return "/brapi/v1/studies/${encodeURIComponent(String(studyDbId))}/observationunits";
  }
  getObservationUnits(e, t, i, s, u = "body", a = {}) {
    let d = [];
    return t !== void 0 && d.push("observationLevel=" + encodeURIComponent(String(t))), i !== void 0 && d.push("pageSize=" + encodeURIComponent(String(i))), s !== void 0 && d.push("page=" + encodeURIComponent(String(s))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}/observationunits${d.length > 0 ? "?" + d.join("&") : ""}`, a);
  }
  /**
   * List all the observation variables measured in the study.
   * List all the observation variables measured in the study.
   * @param studyDbId studyDbId
   * @param Authorization Authentication token
   * @param pageSize pageSize
   * @param page page
   * @param Accept_Language Request accepted language
   
   */
  getObservationVariablesPath() {
    return "/brapi/v1/studies/${encodeURIComponent(String(studyDbId))}/observationvariables";
  }
  getObservationVariables(e, t, i, s = "body", u = {}) {
    let a = [];
    return t !== void 0 && a.push("pageSize=" + encodeURIComponent(String(t))), i !== void 0 && a.push("page=" + encodeURIComponent(String(i))), u.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}/observationvariables${a.length > 0 ? "?" + a.join("&") : ""}`, u);
  }
  /**
   * Get the observations associated to a specific study
   * Get the observations associated to a specific study
   * @param studyDbId studyDbId
   * @param Authorization Authentication token
   * @param observationVariableDbIds observationVariableDbIds
   * @param pageSize pageSize
   * @param page page
   * @param Accept_Language Request accepted language
   
   */
  getObservationsPath() {
    return "/brapi/v1/studies/${encodeURIComponent(String(studyDbId))}/observations";
  }
  getObservations(e, t, i, s, u = "body", a = {}) {
    let d = [];
    return t && t.forEach((l) => {
      d.push("observationVariableDbIds=" + encodeURIComponent(String(l)));
    }), i !== void 0 && d.push("pageSize=" + encodeURIComponent(String(i))), s !== void 0 && d.push("page=" + encodeURIComponent(String(s))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}/observations${d.length > 0 ? "?" + d.join("&") : ""}`, a);
  }
  /**
   * Retrieve studies information
   * Retrieve studies information
   * @param Authorization Authentication token
   * @param studyDbId Search by studyDbId
   * @param active Filter active status true/false
   * @param sortBy Name of the field to sort by: studyDbId, active
   * @param sortOrder Sort order direction - ASC or DESC
   * @param page Page number
   * @param pageSize Page size
   * @param Accept_Language Request accepted language
   
   */
  getStudiesPath() {
    return "/brapi/v1/studies";
  }
  getStudies(e, t, i, s, u, a, d = "body", g = {}) {
    let l = [];
    return e !== void 0 && l.push("studyDbId=" + encodeURIComponent(String(e))), t !== void 0 && l.push("active=" + encodeURIComponent(String(t))), i !== void 0 && l.push("sortBy=" + encodeURIComponent(String(i))), s !== void 0 && l.push("sortOrder=" + encodeURIComponent(String(s))), u !== void 0 && l.push("page=" + encodeURIComponent(String(u))), a !== void 0 && l.push("pageSize=" + encodeURIComponent(String(a))), g.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies${l.length > 0 ? "?" + l.join("&") : ""}`, g);
  }
  /**
   * Retrieve studies information
   * Retrieve studies information
   * @param Authorization Authentication token
   * @param studyDbId Search by studyDbId
   * @param active Filter active status true/false
   * @param sortBy Name of the field to sort by: studyDbId or seasonDbId
   * @param sortOrder Sort order direction - ASC or DESC
   * @param pageSize pageSize
   * @param page page
   * @param Accept_Language Request accepted language
   
   */
  getStudiesSearchPath() {
    return "/brapi/v1/studies-search";
  }
  getStudiesSearch(e, t, i, s, u, a, d = "body", g = {}) {
    let l = [];
    return e !== void 0 && l.push("studyDbId=" + encodeURIComponent(String(e))), t !== void 0 && l.push("active=" + encodeURIComponent(String(t))), i !== void 0 && l.push("sortBy=" + encodeURIComponent(String(i))), s !== void 0 && l.push("sortOrder=" + encodeURIComponent(String(s))), u !== void 0 && l.push("pageSize=" + encodeURIComponent(String(u))), a !== void 0 && l.push("page=" + encodeURIComponent(String(a))), g.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies-search${l.length > 0 ? "?" + l.join("&") : ""}`, g);
  }
  /**
   * Retrieve study details
   * Retrieve study details
   * @param studyDbId Search by studyDbId
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getStudyDetailsPath() {
    return "/brapi/v1/studies/${encodeURIComponent(String(studyDbId))}";
  }
  getStudyDetails(e, t = "body", i = {}) {
    return i.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}`, i);
  }
  /**
   * Retrieve variable details by id
   * Retrieve variable details by id
   * @param observationVariableDbId A variable URI (Unique Resource Identifier)
   * @param Authorization Authentication token
   * @param Accept_Language Request accepted language
   
   */
  getVariableDetailsPath() {
    return "/brapi/v1/variables/${encodeURIComponent(String(observationVariableDbId))}";
  }
  getVariableDetails(e, t = "body", i = {}) {
    return i.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/variables/${encodeURIComponent(String(e))}`, i);
  }
  /**
   * BrAPIv1CallDTO to retrieve a list of observationVariables available in the system
   * retrieve variables information
   * @param Authorization Authentication token
   * @param observationVariableDbId observationVariableDbId
   * @param pageSize pageSize
   * @param page page
   * @param Accept_Language Request accepted language
   
   */
  getVariablesListPath() {
    return "/brapi/v1/variables";
  }
  getVariablesList(e, t, i, s = "body", u = {}) {
    let a = [];
    return e !== void 0 && a.push("observationVariableDbId=" + encodeURIComponent(String(e))), t !== void 0 && a.push("pageSize=" + encodeURIComponent(String(t))), i !== void 0 && a.push("page=" + encodeURIComponent(String(i))), u.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/variables${a.length > 0 ? "?" + a.join("&") : ""}`, u);
  }
};
q = at([
  ye(),
  fe(0, ue("IApiHttpClient")),
  fe(1, ue("IAPIConfiguration"))
], q);
var ce;
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(ce || (ce = {}));
const _t = "", At = {
  csv: ",",
  tsv: "   ",
  ssv: " ",
  pipes: "|"
};
class Rt {
  static with(t) {
    t.bind("BRAPIService").to(q).inSingletonScope();
  }
}
var S = typeof globalThis < "u" && globalThis || typeof self < "u" && self || // eslint-disable-next-line no-undef
typeof global < "u" && global || {}, E = {
  searchParams: "URLSearchParams" in S,
  iterable: "Symbol" in S && "iterator" in Symbol,
  blob: "FileReader" in S && "Blob" in S && function() {
    try {
      return new Blob(), !0;
    } catch {
      return !1;
    }
  }(),
  formData: "FormData" in S,
  arrayBuffer: "ArrayBuffer" in S
};
function st(e) {
  return e && DataView.prototype.isPrototypeOf(e);
}
if (E.arrayBuffer)
  var ut = [
    "[object Int8Array]",
    "[object Uint8Array]",
    "[object Uint8ClampedArray]",
    "[object Int16Array]",
    "[object Uint16Array]",
    "[object Int32Array]",
    "[object Uint32Array]",
    "[object Float32Array]",
    "[object Float64Array]"
  ], ft = ArrayBuffer.isView || function(e) {
    return e && ut.indexOf(Object.prototype.toString.call(e)) > -1;
  };
function D(e) {
  if (typeof e != "string" && (e = String(e)), /[^a-z0-9\-#$%&'*+.^_`|~!]/i.test(e) || e === "")
    throw new TypeError('Invalid character in header field name: "' + e + '"');
  return e.toLowerCase();
}
function V(e) {
  return typeof e != "string" && (e = String(e)), e;
}
function J(e) {
  var t = {
    next: function() {
      var i = e.shift();
      return { done: i === void 0, value: i };
    }
  };
  return E.iterable && (t[Symbol.iterator] = function() {
    return t;
  }), t;
}
function A(e) {
  this.map = {}, e instanceof A ? e.forEach(function(t, i) {
    this.append(i, t);
  }, this) : Array.isArray(e) ? e.forEach(function(t) {
    if (t.length != 2)
      throw new TypeError("Headers constructor: expected name/value pair to be length 2, found" + t.length);
    this.append(t[0], t[1]);
  }, this) : e && Object.getOwnPropertyNames(e).forEach(function(t) {
    this.append(t, e[t]);
  }, this);
}
A.prototype.append = function(e, t) {
  e = D(e), t = V(t);
  var i = this.map[e];
  this.map[e] = i ? i + ", " + t : t;
};
A.prototype.delete = function(e) {
  delete this.map[D(e)];
};
A.prototype.get = function(e) {
  return e = D(e), this.has(e) ? this.map[e] : null;
};
A.prototype.has = function(e) {
  return this.map.hasOwnProperty(D(e));
};
A.prototype.set = function(e, t) {
  this.map[D(e)] = V(t);
};
A.prototype.forEach = function(e, t) {
  for (var i in this.map)
    this.map.hasOwnProperty(i) && e.call(t, this.map[i], i, this);
};
A.prototype.keys = function() {
  var e = [];
  return this.forEach(function(t, i) {
    e.push(i);
  }), J(e);
};
A.prototype.values = function() {
  var e = [];
  return this.forEach(function(t) {
    e.push(t);
  }), J(e);
};
A.prototype.entries = function() {
  var e = [];
  return this.forEach(function(t, i) {
    e.push([i, t]);
  }), J(e);
};
E.iterable && (A.prototype[Symbol.iterator] = A.prototype.entries);
function H(e) {
  if (!e._noBody) {
    if (e.bodyUsed)
      return Promise.reject(new TypeError("Already read"));
    e.bodyUsed = !0;
  }
}
function ve(e) {
  return new Promise(function(t, i) {
    e.onload = function() {
      t(e.result);
    }, e.onerror = function() {
      i(e.error);
    };
  });
}
function ct(e) {
  var t = new FileReader(), i = ve(t);
  return t.readAsArrayBuffer(e), i;
}
function lt(e) {
  var t = new FileReader(), i = ve(t), s = /charset=([A-Za-z0-9_-]+)/.exec(e.type), u = s ? s[1] : "utf-8";
  return t.readAsText(e, u), i;
}
function ht(e) {
  for (var t = new Uint8Array(e), i = new Array(t.length), s = 0; s < t.length; s++)
    i[s] = String.fromCharCode(t[s]);
  return i.join("");
}
function le(e) {
  if (e.slice)
    return e.slice(0);
  var t = new Uint8Array(e.byteLength);
  return t.set(new Uint8Array(e)), t.buffer;
}
function be() {
  return this.bodyUsed = !1, this._initBody = function(e) {
    this.bodyUsed = this.bodyUsed, this._bodyInit = e, e ? typeof e == "string" ? this._bodyText = e : E.blob && Blob.prototype.isPrototypeOf(e) ? this._bodyBlob = e : E.formData && FormData.prototype.isPrototypeOf(e) ? this._bodyFormData = e : E.searchParams && URLSearchParams.prototype.isPrototypeOf(e) ? this._bodyText = e.toString() : E.arrayBuffer && E.blob && st(e) ? (this._bodyArrayBuffer = le(e.buffer), this._bodyInit = new Blob([this._bodyArrayBuffer])) : E.arrayBuffer && (ArrayBuffer.prototype.isPrototypeOf(e) || ft(e)) ? this._bodyArrayBuffer = le(e) : this._bodyText = e = Object.prototype.toString.call(e) : (this._noBody = !0, this._bodyText = ""), this.headers.get("content-type") || (typeof e == "string" ? this.headers.set("content-type", "text/plain;charset=UTF-8") : this._bodyBlob && this._bodyBlob.type ? this.headers.set("content-type", this._bodyBlob.type) : E.searchParams && URLSearchParams.prototype.isPrototypeOf(e) && this.headers.set("content-type", "application/x-www-form-urlencoded;charset=UTF-8"));
  }, E.blob && (this.blob = function() {
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
  }), this.arrayBuffer = function() {
    if (this._bodyArrayBuffer) {
      var e = H(this);
      return e || (ArrayBuffer.isView(this._bodyArrayBuffer) ? Promise.resolve(
        this._bodyArrayBuffer.buffer.slice(
          this._bodyArrayBuffer.byteOffset,
          this._bodyArrayBuffer.byteOffset + this._bodyArrayBuffer.byteLength
        )
      ) : Promise.resolve(this._bodyArrayBuffer));
    } else {
      if (E.blob)
        return this.blob().then(ct);
      throw new Error("could not read as ArrayBuffer");
    }
  }, this.text = function() {
    var e = H(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return lt(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(ht(this._bodyArrayBuffer));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as text");
    return Promise.resolve(this._bodyText);
  }, E.formData && (this.formData = function() {
    return this.text().then(yt);
  }), this.json = function() {
    return this.text().then(JSON.parse);
  }, this;
}
var dt = ["CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE"];
function pt(e) {
  var t = e.toUpperCase();
  return dt.indexOf(t) > -1 ? t : e;
}
function M(e, t) {
  if (!(this instanceof M))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  t = t || {};
  var i = t.body;
  if (e instanceof M) {
    if (e.bodyUsed)
      throw new TypeError("Already read");
    this.url = e.url, this.credentials = e.credentials, t.headers || (this.headers = new A(e.headers)), this.method = e.method, this.mode = e.mode, this.signal = e.signal, !i && e._bodyInit != null && (i = e._bodyInit, e.bodyUsed = !0);
  } else
    this.url = String(e);
  if (this.credentials = t.credentials || this.credentials || "same-origin", (t.headers || !this.headers) && (this.headers = new A(t.headers)), this.method = pt(t.method || this.method || "GET"), this.mode = t.mode || this.mode || null, this.signal = t.signal || this.signal || function() {
    if ("AbortController" in S) {
      var a = new AbortController();
      return a.signal;
    }
  }(), this.referrer = null, (this.method === "GET" || this.method === "HEAD") && i)
    throw new TypeError("Body not allowed for GET or HEAD requests");
  if (this._initBody(i), (this.method === "GET" || this.method === "HEAD") && (t.cache === "no-store" || t.cache === "no-cache")) {
    var s = /([?&])_=[^&]*/;
    if (s.test(this.url))
      this.url = this.url.replace(s, "$1_=" + (/* @__PURE__ */ new Date()).getTime());
    else {
      var u = /\?/;
      this.url += (u.test(this.url) ? "&" : "?") + "_=" + (/* @__PURE__ */ new Date()).getTime();
    }
  }
}
M.prototype.clone = function() {
  return new M(this, { body: this._bodyInit });
};
function yt(e) {
  var t = new FormData();
  return e.trim().split("&").forEach(function(i) {
    if (i) {
      var s = i.split("="), u = s.shift().replace(/\+/g, " "), a = s.join("=").replace(/\+/g, " ");
      t.append(decodeURIComponent(u), decodeURIComponent(a));
    }
  }), t;
}
function vt(e) {
  var t = new A(), i = e.replace(/\r?\n[\t ]+/g, " ");
  return i.split("\r").map(function(s) {
    return s.indexOf(`
`) === 0 ? s.substr(1, s.length) : s;
  }).forEach(function(s) {
    var u = s.split(":"), a = u.shift().trim();
    if (a) {
      var d = u.join(":").trim();
      try {
        t.append(a, d);
      } catch (g) {
        console.warn("Response " + g.message);
      }
    }
  }), t;
}
be.call(M.prototype);
function T(e, t) {
  if (!(this instanceof T))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  if (t || (t = {}), this.type = "default", this.status = t.status === void 0 ? 200 : t.status, this.status < 200 || this.status > 599)
    throw new RangeError("Failed to construct 'Response': The status provided (0) is outside the range [200, 599].");
  this.ok = this.status >= 200 && this.status < 300, this.statusText = t.statusText === void 0 ? "" : "" + t.statusText, this.headers = new A(t.headers), this.url = t.url || "", this._initBody(e);
}
be.call(T.prototype);
T.prototype.clone = function() {
  return new T(this._bodyInit, {
    status: this.status,
    statusText: this.statusText,
    headers: new A(this.headers),
    url: this.url
  });
};
T.error = function() {
  var e = new T(null, { status: 200, statusText: "" });
  return e.ok = !1, e.status = 0, e.type = "error", e;
};
var bt = [301, 302, 303, 307, 308];
T.redirect = function(e, t) {
  if (bt.indexOf(t) === -1)
    throw new RangeError("Invalid status code");
  return new T(null, { status: t, headers: { location: e } });
};
var U = S.DOMException;
try {
  new U();
} catch {
  U = function(t, i) {
    this.message = t, this.name = i;
    var s = Error(t);
    this.stack = s.stack;
  }, U.prototype = Object.create(Error.prototype), U.prototype.constructor = U;
}
function me(e, t) {
  return new Promise(function(i, s) {
    var u = new M(e, t);
    if (u.signal && u.signal.aborted)
      return s(new U("Aborted", "AbortError"));
    var a = new XMLHttpRequest();
    function d() {
      a.abort();
    }
    a.onload = function() {
      var v = {
        statusText: a.statusText,
        headers: vt(a.getAllResponseHeaders() || "")
      };
      u.url.indexOf("file://") === 0 && (a.status < 200 || a.status > 599) ? v.status = 200 : v.status = a.status, v.url = "responseURL" in a ? a.responseURL : v.headers.get("X-Request-URL");
      var w = "response" in a ? a.response : a.responseText;
      setTimeout(function() {
        i(new T(w, v));
      }, 0);
    }, a.onerror = function() {
      setTimeout(function() {
        s(new TypeError("Network request failed"));
      }, 0);
    }, a.ontimeout = function() {
      setTimeout(function() {
        s(new TypeError("Network request timed out"));
      }, 0);
    }, a.onabort = function() {
      setTimeout(function() {
        s(new U("Aborted", "AbortError"));
      }, 0);
    };
    function g(v) {
      try {
        return v === "" && S.location.href ? S.location.href : v;
      } catch {
        return v;
      }
    }
    if (a.open(u.method, g(u.url), !0), u.credentials === "include" ? a.withCredentials = !0 : u.credentials === "omit" && (a.withCredentials = !1), "responseType" in a && (E.blob ? a.responseType = "blob" : E.arrayBuffer && (a.responseType = "arraybuffer")), t && typeof t.headers == "object" && !(t.headers instanceof A || S.Headers && t.headers instanceof S.Headers)) {
      var l = [];
      Object.getOwnPropertyNames(t.headers).forEach(function(v) {
        l.push(D(v)), a.setRequestHeader(v, V(t.headers[v]));
      }), u.headers.forEach(function(v, w) {
        l.indexOf(w) === -1 && a.setRequestHeader(w, v);
      });
    } else
      u.headers.forEach(function(v, w) {
        a.setRequestHeader(w, v);
      });
    u.signal && (u.signal.addEventListener("abort", d), a.onreadystatechange = function() {
      a.readyState === 4 && u.signal.removeEventListener("abort", d);
    }), a.send(typeof u._bodyInit > "u" ? null : u._bodyInit);
  });
}
me.polyfill = !0;
S.fetch || (S.fetch = me, S.Headers = A, S.Request = M, S.Response = T);
class mt {
  constructor(t, i, s) {
    this.response = t, this.status = i, this.headers = s;
  }
}
class he {
  constructor(t, i, s, u) {
    this.message = t, this.level = i, this.translationKey = s, this.translationValues = u;
  }
}
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(he || (he = {}));
var gt = Object.getOwnPropertyDescriptor, wt = (e, t, i, s) => {
  for (var u = s > 1 ? void 0 : s ? gt(t, i) : t, a = e.length - 1, d; a >= 0; a--)
    (d = e[a]) && (u = d(u) || u);
  return u;
};
let de = class {
  get(e, t) {
    return this.performNetworkCall(e, "get", void 0, t);
  }
  post(e, t, i) {
    return this.performNetworkCall(e, "post", this.getJsonBody(t), this.addJsonHeaders(i));
  }
  put(e, t, i) {
    return this.performNetworkCall(e, "put", this.getJsonBody(t), this.addJsonHeaders(i));
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
  performNetworkCall(e, t, i, s) {
    let u;
    return typeof window > "u" ? u = require("node-fetch") : u = window.fetch, u(e, {
      method: t,
      body: i,
      mode: "cors",
      headers: s
    }).then((d) => {
      let g = {};
      return d.headers.forEach((l, v) => {
        g[v.toString().toLowerCase()] = l;
      }), d.text().then((l) => {
        let v = g["content-type"] || "", w;
        v.match("application/json") ? (w = JSON.parse(l), w.metadata && w.metadata) : w = l;
        let O = new mt(w, d.status, g);
        if (d.status >= 400)
          throw O;
        return O;
      });
    });
  }
};
de = wt([
  ye()
], de);
export {
  Rt as ApiServiceBinder,
  q as BRAPIService,
  At as COLLECTION_FORMATS,
  ce as StatusDTO,
  _t as __fakeExport
};
