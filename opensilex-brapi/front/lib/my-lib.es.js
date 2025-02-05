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
      var u = typeof ie == "object" ? ie : typeof self == "object" ? self : typeof this == "object" ? this : Function("return this;")(), s = a(t);
      typeof u.Reflect > "u" ? u.Reflect = t : s = a(u.Reflect, s), i(s);
      function a(d, m) {
        return function(c, g) {
          typeof d[c] != "function" && Object.defineProperty(d, c, { configurable: !0, writable: !0, value: g }), m && m(c, g);
        };
      }
    })(function(i) {
      var u = Object.prototype.hasOwnProperty, s = typeof Symbol == "function", a = s && typeof Symbol.toPrimitive < "u" ? Symbol.toPrimitive : "@@toPrimitive", d = s && typeof Symbol.iterator < "u" ? Symbol.iterator : "@@iterator", m = typeof Object.create == "function", c = { __proto__: [] } instanceof Array, g = !m && !c, R = {
        // create an object in dictionary mode (a.k.a. "slow" mode in v8)
        create: m ? function() {
          return F(/* @__PURE__ */ Object.create(null));
        } : c ? function() {
          return F({ __proto__: null });
        } : function() {
          return F({});
        },
        has: g ? function(r, n) {
          return u.call(r, n);
        } : function(r, n) {
          return n in r;
        },
        get: g ? function(r, n) {
          return u.call(r, n) ? r[n] : void 0;
        } : function(r, n) {
          return r[n];
        }
      }, O = Object.getPrototypeOf(Function), N = typeof process == "object" && process.env && process.env.REFLECT_METADATA_USE_MAP_POLYFILL === "true", $ = !N && typeof Map == "function" && typeof Map.prototype.entries == "function" ? Map : qe(), ge = !N && typeof Set == "function" && typeof Set.prototype.entries == "function" ? Set : He(), we = !N && typeof WeakMap == "function" ? WeakMap : Ve(), k = new we();
      function _e(r, n, o, f) {
        if (w(o)) {
          if (!ee(r))
            throw new TypeError();
          if (!te(n))
            throw new TypeError();
          return Ue(r, n);
        } else {
          if (!ee(r))
            throw new TypeError();
          if (!S(n))
            throw new TypeError();
          if (!S(f) && !w(f) && !D(f))
            throw new TypeError();
          return D(f) && (f = void 0), o = E(o), Me(r, n, o, f);
        }
      }
      i("decorate", _e);
      function Ae(r, n) {
        function o(f, l) {
          if (!S(f))
            throw new TypeError();
          if (!w(l) && !Ne(l))
            throw new TypeError();
          X(r, n, f, l);
        }
        return o;
      }
      i("metadata", Ae);
      function Se(r, n, o, f) {
        if (!S(o))
          throw new TypeError();
        return w(f) || (f = E(f)), X(r, n, o, f);
      }
      i("defineMetadata", Se);
      function Ie(r, n, o) {
        if (!S(n))
          throw new TypeError();
        return w(o) || (o = E(o)), W(r, n, o);
      }
      i("hasMetadata", Ie);
      function Re(r, n, o) {
        if (!S(n))
          throw new TypeError();
        return w(o) || (o = E(o)), G(r, n, o);
      }
      i("hasOwnMetadata", Re);
      function Pe(r, n, o) {
        if (!S(n))
          throw new TypeError();
        return w(o) || (o = E(o)), z(r, n, o);
      }
      i("getMetadata", Pe);
      function Te(r, n, o) {
        if (!S(n))
          throw new TypeError();
        return w(o) || (o = E(o)), Y(r, n, o);
      }
      i("getOwnMetadata", Te);
      function Ee(r, n) {
        if (!S(r))
          throw new TypeError();
        return w(n) || (n = E(n)), Z(r, n);
      }
      i("getMetadataKeys", Ee);
      function Oe(r, n) {
        if (!S(r))
          throw new TypeError();
        return w(n) || (n = E(n)), Q(r, n);
      }
      i("getOwnMetadataKeys", Oe);
      function Ce(r, n, o) {
        if (!S(n))
          throw new TypeError();
        w(o) || (o = E(o));
        var f = j(
          n,
          o,
          /*Create*/
          !1
        );
        if (w(f) || !f.delete(r))
          return !1;
        if (f.size > 0)
          return !0;
        var l = k.get(n);
        return l.delete(o), l.size > 0 || k.delete(n), !0;
      }
      i("deleteMetadata", Ce);
      function Ue(r, n) {
        for (var o = r.length - 1; o >= 0; --o) {
          var f = r[o], l = f(n);
          if (!w(l) && !D(l)) {
            if (!te(l))
              throw new TypeError();
            n = l;
          }
        }
        return n;
      }
      function Me(r, n, o, f) {
        for (var l = r.length - 1; l >= 0; --l) {
          var A = r[l], p = A(n, o, f);
          if (!w(p) && !D(p)) {
            if (!S(p))
              throw new TypeError();
            f = p;
          }
        }
        return f;
      }
      function j(r, n, o) {
        var f = k.get(r);
        if (w(f)) {
          if (!o)
            return;
          f = new $(), k.set(r, f);
        }
        var l = f.get(n);
        if (w(l)) {
          if (!o)
            return;
          l = new $(), f.set(n, l);
        }
        return l;
      }
      function W(r, n, o) {
        var f = G(r, n, o);
        if (f)
          return !0;
        var l = L(n);
        return D(l) ? !1 : W(r, l, o);
      }
      function G(r, n, o) {
        var f = j(
          n,
          o,
          /*Create*/
          !1
        );
        return w(f) ? !1 : ke(f.has(r));
      }
      function z(r, n, o) {
        var f = G(r, n, o);
        if (f)
          return Y(r, n, o);
        var l = L(n);
        if (!D(l))
          return z(r, l, o);
      }
      function Y(r, n, o) {
        var f = j(
          n,
          o,
          /*Create*/
          !1
        );
        if (!w(f))
          return f.get(r);
      }
      function X(r, n, o, f) {
        var l = j(
          o,
          f,
          /*Create*/
          !0
        );
        l.set(r, n);
      }
      function Z(r, n) {
        var o = Q(r, n), f = L(r);
        if (f === null)
          return o;
        var l = Z(f, n);
        if (l.length <= 0)
          return o;
        if (o.length <= 0)
          return l;
        for (var A = new ge(), p = [], y = 0, h = o; y < h.length; y++) {
          var v = h[y], b = A.has(v);
          b || (A.add(v), p.push(v));
        }
        for (var C = 0, ne = l; C < ne.length; C++) {
          var v = ne[C], b = A.has(v);
          b || (A.add(v), p.push(v));
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
        if (w(f))
          return o;
        for (var l = f.keys(), A = $e(l), p = 0; ; ) {
          var y = Le(A);
          if (!y)
            return o.length = p, o;
          var h = Ge(y);
          try {
            o[p] = h;
          } catch (v) {
            try {
              Fe(A);
            } finally {
              throw v;
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
      function w(r) {
        return r === void 0;
      }
      function D(r) {
        return r === null;
      }
      function De(r) {
        return typeof r == "symbol";
      }
      function S(r) {
        return typeof r == "object" ? r !== null : typeof r == "function";
      }
      function je(r, n) {
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
          var l = f.call(r, o);
          if (S(l))
            throw new TypeError();
          return l;
        }
        return Be(r);
      }
      function Be(r, n) {
        var o, f, l;
        {
          var A = r.toString;
          if (x(A)) {
            var f = A.call(r);
            if (!S(f))
              return f;
          }
          var o = r.valueOf;
          if (x(o)) {
            var f = o.call(r);
            if (!S(f))
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
      function E(r) {
        var n = je(r);
        return De(n) ? n : xe(n);
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
        if (!S(o))
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
        var l = f.constructor;
        return typeof l != "function" || l === r ? n : l;
      }
      function qe() {
        var r = {}, n = [], o = (
          /** @class */
          function() {
            function p(y, h, v) {
              this._index = 0, this._keys = y, this._values = h, this._selector = v;
            }
            return p.prototype["@@iterator"] = function() {
              return this;
            }, p.prototype[d] = function() {
              return this;
            }, p.prototype.next = function() {
              var y = this._index;
              if (y >= 0 && y < this._keys.length) {
                var h = this._selector(this._keys[y], this._values[y]);
                return y + 1 >= this._keys.length ? (this._index = -1, this._keys = n, this._values = n) : this._index++, { value: h, done: !1 };
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
              var h = this._find(
                y,
                /*insert*/
                !1
              );
              return h >= 0 ? this._values[h] : void 0;
            }, p.prototype.set = function(y, h) {
              var v = this._find(
                y,
                /*insert*/
                !0
              );
              return this._values[v] = h, this;
            }, p.prototype.delete = function(y) {
              var h = this._find(
                y,
                /*insert*/
                !1
              );
              if (h >= 0) {
                for (var v = this._keys.length, b = h + 1; b < v; b++)
                  this._keys[b - 1] = this._keys[b], this._values[b - 1] = this._values[b];
                return this._keys.length--, this._values.length--, y === this._cacheKey && (this._cacheKey = r, this._cacheIndex = -2), !0;
              }
              return !1;
            }, p.prototype.clear = function() {
              this._keys.length = 0, this._values.length = 0, this._cacheKey = r, this._cacheIndex = -2;
            }, p.prototype.keys = function() {
              return new o(this._keys, this._values, f);
            }, p.prototype.values = function() {
              return new o(this._keys, this._values, l);
            }, p.prototype.entries = function() {
              return new o(this._keys, this._values, A);
            }, p.prototype["@@iterator"] = function() {
              return this.entries();
            }, p.prototype[d] = function() {
              return this.entries();
            }, p.prototype._find = function(y, h) {
              return this._cacheKey !== y && (this._cacheIndex = this._keys.indexOf(this._cacheKey = y)), this._cacheIndex < 0 && h && (this._cacheIndex = this._keys.length, this._keys.push(y), this._values.push(void 0)), this._cacheIndex;
            }, p;
          }()
        );
        function f(p, y) {
          return p;
        }
        function l(p, y) {
          return y;
        }
        function A(p, y) {
          return [p, y];
        }
      }
      function He() {
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
        var r = 16, n = R.create(), o = f();
        return (
          /** @class */
          function() {
            function h() {
              this._key = f();
            }
            return h.prototype.has = function(v) {
              var b = l(
                v,
                /*create*/
                !1
              );
              return b !== void 0 ? R.has(b, this._key) : !1;
            }, h.prototype.get = function(v) {
              var b = l(
                v,
                /*create*/
                !1
              );
              return b !== void 0 ? R.get(b, this._key) : void 0;
            }, h.prototype.set = function(v, b) {
              var C = l(
                v,
                /*create*/
                !0
              );
              return C[this._key] = b, this;
            }, h.prototype.delete = function(v) {
              var b = l(
                v,
                /*create*/
                !1
              );
              return b !== void 0 ? delete b[this._key] : !1;
            }, h.prototype.clear = function() {
              this._key = f();
            }, h;
          }()
        );
        function f() {
          var h;
          do
            h = "@@WeakMap@@" + y();
          while (R.has(n, h));
          return n[h] = !0, h;
        }
        function l(h, v) {
          if (!u.call(h, o)) {
            if (!v)
              return;
            Object.defineProperty(h, o, { value: R.create() });
          }
          return h[o];
        }
        function A(h, v) {
          for (var b = 0; b < v; ++b)
            h[b] = Math.random() * 255 | 0;
          return h;
        }
        function p(h) {
          return typeof Uint8Array == "function" ? typeof crypto < "u" ? crypto.getRandomValues(new Uint8Array(h)) : typeof msCrypto < "u" ? msCrypto.getRandomValues(new Uint8Array(h)) : A(new Uint8Array(h), h) : A(new Array(h), h);
        }
        function y() {
          var h = p(r);
          h[6] = h[6] & 79 | 64, h[8] = h[8] & 191 | 128;
          for (var v = "", b = 0; b < r; ++b) {
            var C = h[b];
            (b === 4 || b === 6 || b === 8) && (v += "-"), C < 16 && (v += "0"), v += C.toString(16).toLowerCase();
          }
          return v;
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
function nt(e, t, i, u) {
  var s = Ye;
  pe(s, e, t, u, i);
}
function it(e, t, i) {
  var u = Xe;
  pe(u, e.constructor, t, i);
}
function pe(e, t, i, u, s) {
  var a = {}, d = typeof s == "number", m = s !== void 0 && d ? s.toString() : i;
  if (d && i !== void 0)
    throw new Error(tt);
  Reflect.hasOwnMetadata(e, t) && (a = Reflect.getMetadata(e, t));
  var c = a[m];
  if (!Array.isArray(c))
    c = [];
  else
    for (var g = 0, R = c; g < R.length; g++) {
      var O = R[g];
      if (O.key === u.key)
        throw new Error(Ke + " " + O.key.toString());
    }
  c.push(u), a[m] = c, Reflect.defineMetadata(e, a, t);
}
function ue(e) {
  return function(t, i, u) {
    if (e === void 0)
      throw new Error(et(t.name));
    var s = new rt(ze, e);
    typeof u == "number" ? nt(t, i, u, s) : it(t, i, s);
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
var ot = Object.getOwnPropertyDescriptor, at = (e, t, i, u) => {
  for (var s = u > 1 ? void 0 : u ? ot(t, i) : t, a = e.length - 1, d; a >= 0; a--)
    (d = e[a]) && (s = d(s) || s);
  return s;
}, fe = (e, t) => (i, u) => t(i, u, e);
let H = class {
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
  getCalls(e, t, i, u = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("page=" + encodeURIComponent(String(e))), t !== void 0 && a.push("pageSize=" + encodeURIComponent(String(t))), i !== void 0 && a.push("dataType=" + encodeURIComponent(String(i))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/calls${a.length > 0 ? "?" + a.join("&") : ""}`, s);
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
  getGermplasmBySearch(e, t, i, u, s, a, d = "body", m = {}) {
    let c = [];
    return e !== void 0 && c.push("germplasmDbId=" + encodeURIComponent(String(e))), t !== void 0 && c.push("germplasmPUI=" + encodeURIComponent(String(t))), i !== void 0 && c.push("germplasmName=" + encodeURIComponent(String(i))), u !== void 0 && c.push("commonCropName=" + encodeURIComponent(String(u))), s !== void 0 && c.push("page=" + encodeURIComponent(String(s))), a !== void 0 && c.push("page_size=" + encodeURIComponent(String(a))), m.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/germplasm${c.length > 0 ? "?" + c.join("&") : ""}`, m);
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
  getObservationUnits(e, t, i, u, s = "body", a = {}) {
    let d = [];
    return t !== void 0 && d.push("observationLevel=" + encodeURIComponent(String(t))), i !== void 0 && d.push("pageSize=" + encodeURIComponent(String(i))), u !== void 0 && d.push("page=" + encodeURIComponent(String(u))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}/observationunits${d.length > 0 ? "?" + d.join("&") : ""}`, a);
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
  getObservationVariables(e, t, i, u = "body", s = {}) {
    let a = [];
    return t !== void 0 && a.push("pageSize=" + encodeURIComponent(String(t))), i !== void 0 && a.push("page=" + encodeURIComponent(String(i))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}/observationvariables${a.length > 0 ? "?" + a.join("&") : ""}`, s);
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
  getObservations(e, t, i, u, s = "body", a = {}) {
    let d = [];
    return t && t.forEach((c) => {
      d.push("observationVariableDbIds=" + encodeURIComponent(String(c)));
    }), i !== void 0 && d.push("pageSize=" + encodeURIComponent(String(i))), u !== void 0 && d.push("page=" + encodeURIComponent(String(u))), a.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies/${encodeURIComponent(String(e))}/observations${d.length > 0 ? "?" + d.join("&") : ""}`, a);
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
  getStudies(e, t, i, u, s, a, d = "body", m = {}) {
    let c = [];
    return e !== void 0 && c.push("studyDbId=" + encodeURIComponent(String(e))), t !== void 0 && c.push("active=" + encodeURIComponent(String(t))), i !== void 0 && c.push("sortBy=" + encodeURIComponent(String(i))), u !== void 0 && c.push("sortOrder=" + encodeURIComponent(String(u))), s !== void 0 && c.push("page=" + encodeURIComponent(String(s))), a !== void 0 && c.push("pageSize=" + encodeURIComponent(String(a))), m.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies${c.length > 0 ? "?" + c.join("&") : ""}`, m);
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
  getStudiesSearch(e, t, i, u, s, a, d = "body", m = {}) {
    let c = [];
    return e !== void 0 && c.push("studyDbId=" + encodeURIComponent(String(e))), t !== void 0 && c.push("active=" + encodeURIComponent(String(t))), i !== void 0 && c.push("sortBy=" + encodeURIComponent(String(i))), u !== void 0 && c.push("sortOrder=" + encodeURIComponent(String(u))), s !== void 0 && c.push("pageSize=" + encodeURIComponent(String(s))), a !== void 0 && c.push("page=" + encodeURIComponent(String(a))), m.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/studies-search${c.length > 0 ? "?" + c.join("&") : ""}`, m);
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
  getVariablesList(e, t, i, u = "body", s = {}) {
    let a = [];
    return e !== void 0 && a.push("observationVariableDbId=" + encodeURIComponent(String(e))), t !== void 0 && a.push("pageSize=" + encodeURIComponent(String(t))), i !== void 0 && a.push("page=" + encodeURIComponent(String(i))), s.Accept = "application/json", this.httpClient.get(`${this.basePath}/brapi/v1/variables${a.length > 0 ? "?" + a.join("&") : ""}`, s);
  }
};
H = at([
  ye(),
  fe(0, ue("IApiHttpClient")),
  fe(1, ue("IAPIConfiguration"))
], H);
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
class St {
  static with(t) {
    t.bind("BRAPIService").to(H).inSingletonScope();
  }
}
var I = typeof globalThis < "u" && globalThis || typeof self < "u" && self || typeof I < "u" && I, P = {
  searchParams: "URLSearchParams" in I,
  iterable: "Symbol" in I && "iterator" in Symbol,
  blob: "FileReader" in I && "Blob" in I && function() {
    try {
      return new Blob(), !0;
    } catch {
      return !1;
    }
  }(),
  formData: "FormData" in I,
  arrayBuffer: "ArrayBuffer" in I
};
function st(e) {
  return e && DataView.prototype.isPrototypeOf(e);
}
if (P.arrayBuffer)
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
function B(e) {
  if (typeof e != "string" && (e = String(e)), /[^a-z0-9\-#$%&'*+.^_`|~!]/i.test(e) || e === "")
    throw new TypeError("Invalid character in header field name");
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
  return P.iterable && (t[Symbol.iterator] = function() {
    return t;
  }), t;
}
function _(e) {
  this.map = {}, e instanceof _ ? e.forEach(function(t, i) {
    this.append(i, t);
  }, this) : Array.isArray(e) ? e.forEach(function(t) {
    this.append(t[0], t[1]);
  }, this) : e && Object.getOwnPropertyNames(e).forEach(function(t) {
    this.append(t, e[t]);
  }, this);
}
_.prototype.append = function(e, t) {
  e = B(e), t = V(t);
  var i = this.map[e];
  this.map[e] = i ? i + ", " + t : t;
};
_.prototype.delete = function(e) {
  delete this.map[B(e)];
};
_.prototype.get = function(e) {
  return e = B(e), this.has(e) ? this.map[e] : null;
};
_.prototype.has = function(e) {
  return this.map.hasOwnProperty(B(e));
};
_.prototype.set = function(e, t) {
  this.map[B(e)] = V(t);
};
_.prototype.forEach = function(e, t) {
  for (var i in this.map)
    this.map.hasOwnProperty(i) && e.call(t, this.map[i], i, this);
};
_.prototype.keys = function() {
  var e = [];
  return this.forEach(function(t, i) {
    e.push(i);
  }), J(e);
};
_.prototype.values = function() {
  var e = [];
  return this.forEach(function(t) {
    e.push(t);
  }), J(e);
};
_.prototype.entries = function() {
  var e = [];
  return this.forEach(function(t, i) {
    e.push([i, t]);
  }), J(e);
};
P.iterable && (_.prototype[Symbol.iterator] = _.prototype.entries);
function q(e) {
  if (e.bodyUsed)
    return Promise.reject(new TypeError("Already read"));
  e.bodyUsed = !0;
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
function ht(e) {
  var t = new FileReader(), i = ve(t);
  return t.readAsText(e), i;
}
function lt(e) {
  for (var t = new Uint8Array(e), i = new Array(t.length), u = 0; u < t.length; u++)
    i[u] = String.fromCharCode(t[u]);
  return i.join("");
}
function he(e) {
  if (e.slice)
    return e.slice(0);
  var t = new Uint8Array(e.byteLength);
  return t.set(new Uint8Array(e)), t.buffer;
}
function be() {
  return this.bodyUsed = !1, this._initBody = function(e) {
    this.bodyUsed = this.bodyUsed, this._bodyInit = e, e ? typeof e == "string" ? this._bodyText = e : P.blob && Blob.prototype.isPrototypeOf(e) ? this._bodyBlob = e : P.formData && FormData.prototype.isPrototypeOf(e) ? this._bodyFormData = e : P.searchParams && URLSearchParams.prototype.isPrototypeOf(e) ? this._bodyText = e.toString() : P.arrayBuffer && P.blob && st(e) ? (this._bodyArrayBuffer = he(e.buffer), this._bodyInit = new Blob([this._bodyArrayBuffer])) : P.arrayBuffer && (ArrayBuffer.prototype.isPrototypeOf(e) || ft(e)) ? this._bodyArrayBuffer = he(e) : this._bodyText = e = Object.prototype.toString.call(e) : this._bodyText = "", this.headers.get("content-type") || (typeof e == "string" ? this.headers.set("content-type", "text/plain;charset=UTF-8") : this._bodyBlob && this._bodyBlob.type ? this.headers.set("content-type", this._bodyBlob.type) : P.searchParams && URLSearchParams.prototype.isPrototypeOf(e) && this.headers.set("content-type", "application/x-www-form-urlencoded;charset=UTF-8"));
  }, P.blob && (this.blob = function() {
    var e = q(this);
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
      var e = q(this);
      return e || (ArrayBuffer.isView(this._bodyArrayBuffer) ? Promise.resolve(
        this._bodyArrayBuffer.buffer.slice(
          this._bodyArrayBuffer.byteOffset,
          this._bodyArrayBuffer.byteOffset + this._bodyArrayBuffer.byteLength
        )
      ) : Promise.resolve(this._bodyArrayBuffer));
    } else
      return this.blob().then(ct);
  }), this.text = function() {
    var e = q(this);
    if (e)
      return e;
    if (this._bodyBlob)
      return ht(this._bodyBlob);
    if (this._bodyArrayBuffer)
      return Promise.resolve(lt(this._bodyArrayBuffer));
    if (this._bodyFormData)
      throw new Error("could not read FormData body as text");
    return Promise.resolve(this._bodyText);
  }, P.formData && (this.formData = function() {
    return this.text().then(yt);
  }), this.json = function() {
    return this.text().then(JSON.parse);
  }, this;
}
var dt = ["DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT"];
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
    this.url = e.url, this.credentials = e.credentials, t.headers || (this.headers = new _(e.headers)), this.method = e.method, this.mode = e.mode, this.signal = e.signal, !i && e._bodyInit != null && (i = e._bodyInit, e.bodyUsed = !0);
  } else
    this.url = String(e);
  if (this.credentials = t.credentials || this.credentials || "same-origin", (t.headers || !this.headers) && (this.headers = new _(t.headers)), this.method = pt(t.method || this.method || "GET"), this.mode = t.mode || this.mode || null, this.signal = t.signal || this.signal, this.referrer = null, (this.method === "GET" || this.method === "HEAD") && i)
    throw new TypeError("Body not allowed for GET or HEAD requests");
  if (this._initBody(i), (this.method === "GET" || this.method === "HEAD") && (t.cache === "no-store" || t.cache === "no-cache")) {
    var u = /([?&])_=[^&]*/;
    if (u.test(this.url))
      this.url = this.url.replace(u, "$1_=" + (/* @__PURE__ */ new Date()).getTime());
    else {
      var s = /\?/;
      this.url += (s.test(this.url) ? "&" : "?") + "_=" + (/* @__PURE__ */ new Date()).getTime();
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
      var u = i.split("="), s = u.shift().replace(/\+/g, " "), a = u.join("=").replace(/\+/g, " ");
      t.append(decodeURIComponent(s), decodeURIComponent(a));
    }
  }), t;
}
function vt(e) {
  var t = new _(), i = e.replace(/\r?\n[\t ]+/g, " ");
  return i.split(/\r?\n/).forEach(function(u) {
    var s = u.split(":"), a = s.shift().trim();
    if (a) {
      var d = s.join(":").trim();
      t.append(a, d);
    }
  }), t;
}
be.call(M.prototype);
function T(e, t) {
  if (!(this instanceof T))
    throw new TypeError('Please use the "new" operator, this DOM object constructor cannot be called as a function.');
  t || (t = {}), this.type = "default", this.status = t.status === void 0 ? 200 : t.status, this.ok = this.status >= 200 && this.status < 300, this.statusText = "statusText" in t ? t.statusText : "", this.headers = new _(t.headers), this.url = t.url || "", this._initBody(e);
}
be.call(T.prototype);
T.prototype.clone = function() {
  return new T(this._bodyInit, {
    status: this.status,
    statusText: this.statusText,
    headers: new _(this.headers),
    url: this.url
  });
};
T.error = function() {
  var e = new T(null, { status: 0, statusText: "" });
  return e.type = "error", e;
};
var bt = [301, 302, 303, 307, 308];
T.redirect = function(e, t) {
  if (bt.indexOf(t) === -1)
    throw new RangeError("Invalid status code");
  return new T(null, { status: t, headers: { location: e } });
};
var U = I.DOMException;
try {
  new U();
} catch {
  U = function(t, i) {
    this.message = t, this.name = i;
    var u = Error(t);
    this.stack = u.stack;
  }, U.prototype = Object.create(Error.prototype), U.prototype.constructor = U;
}
function me(e, t) {
  return new Promise(function(i, u) {
    var s = new M(e, t);
    if (s.signal && s.signal.aborted)
      return u(new U("Aborted", "AbortError"));
    var a = new XMLHttpRequest();
    function d() {
      a.abort();
    }
    a.onload = function() {
      var c = {
        status: a.status,
        statusText: a.statusText,
        headers: vt(a.getAllResponseHeaders() || "")
      };
      c.url = "responseURL" in a ? a.responseURL : c.headers.get("X-Request-URL");
      var g = "response" in a ? a.response : a.responseText;
      setTimeout(function() {
        i(new T(g, c));
      }, 0);
    }, a.onerror = function() {
      setTimeout(function() {
        u(new TypeError("Network request failed"));
      }, 0);
    }, a.ontimeout = function() {
      setTimeout(function() {
        u(new TypeError("Network request failed"));
      }, 0);
    }, a.onabort = function() {
      setTimeout(function() {
        u(new U("Aborted", "AbortError"));
      }, 0);
    };
    function m(c) {
      try {
        return c === "" && I.location.href ? I.location.href : c;
      } catch {
        return c;
      }
    }
    a.open(s.method, m(s.url), !0), s.credentials === "include" ? a.withCredentials = !0 : s.credentials === "omit" && (a.withCredentials = !1), "responseType" in a && (P.blob ? a.responseType = "blob" : P.arrayBuffer && s.headers.get("Content-Type") && s.headers.get("Content-Type").indexOf("application/octet-stream") !== -1 && (a.responseType = "arraybuffer")), t && typeof t.headers == "object" && !(t.headers instanceof _) ? Object.getOwnPropertyNames(t.headers).forEach(function(c) {
      a.setRequestHeader(c, V(t.headers[c]));
    }) : s.headers.forEach(function(c, g) {
      a.setRequestHeader(g, c);
    }), s.signal && (s.signal.addEventListener("abort", d), a.onreadystatechange = function() {
      a.readyState === 4 && s.signal.removeEventListener("abort", d);
    }), a.send(typeof s._bodyInit > "u" ? null : s._bodyInit);
  });
}
me.polyfill = !0;
I.fetch || (I.fetch = me, I.Headers = _, I.Request = M, I.Response = T);
class mt {
  constructor(t, i, u) {
    this.response = t, this.status = i, this.headers = u;
  }
}
class le {
  constructor(t, i, u, s) {
    this.message = t, this.level = i, this.translationKey = u, this.translationValues = s;
  }
}
((e) => {
  e.LevelEnum = {
    ERROR: "ERROR",
    WARNING: "WARNING",
    INFO: "INFO",
    DEBUG: "DEBUG"
  };
})(le || (le = {}));
var gt = Object.getOwnPropertyDescriptor, wt = (e, t, i, u) => {
  for (var s = u > 1 ? void 0 : u ? gt(t, i) : t, a = e.length - 1, d; a >= 0; a--)
    (d = e[a]) && (s = d(s) || s);
  return s;
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
  performNetworkCall(e, t, i, u) {
    let s;
    return typeof window > "u" ? s = require("node-fetch") : s = window.fetch, s(e, {
      method: t,
      body: i,
      mode: "cors",
      headers: u
    }).then((d) => {
      let m = {};
      return d.headers.forEach((c, g) => {
        m[g.toString().toLowerCase()] = c;
      }), d.text().then((c) => {
        let g = m["content-type"] || "", R;
        g.match("application/json") ? (R = JSON.parse(c), R.metadata && R.metadata) : R = c;
        let O = new mt(R, d.status, m);
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
  St as ApiServiceBinder,
  H as BRAPIService,
  At as COLLECTION_FORMATS,
  ce as StatusDTO,
  _t as __fakeExport
};
