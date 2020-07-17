/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public enum BuiltInDatatypes {
    STRING("String", XSD.xstring, String.class, (value) -> {
        return true;
    }, (value) -> {
        return value;
    }),
    URI("URI", XSD.anyURI, URI.class, (value) -> {
        try {
            return new URI(value).isAbsolute();
        } catch (Exception ex) {
            return false;
        }
    }, (value) -> {
        try {
            return new URI(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }),
    BOOLEAN("Boolean", XSD.xboolean, Boolean.class, (value) -> {
        return true;
    }, (value) -> {
        return Boolean.valueOf(value);
    }),
    DATE("Date", XSD.date, LocalDate.class, (value) -> {
        try {
            LocalDate.parse(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }, (value) -> {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }),
    DATETIME("Datetime", XSD.dateTime, OffsetDateTime.class, (value) -> {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }, (value) -> {
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }),
    INTEGER("Integer", new Resource[]{
        XSD.integer,
        XSD.xint,
        XSD.unsignedInt,
        XSD.nonPositiveInteger,
        XSD.nonNegativeInteger,
        XSD.positiveInteger,
        XSD.negativeInteger
    }, Integer.class, (value) -> {
        try {
            Integer.valueOf(value);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }, (value) -> {
        return Integer.valueOf(value);
    }),
    BYTE("Byte", new Resource[]{
        XSD.xbyte,
        XSD.unsignedByte
    }, Byte.class, (value) -> {
        try {
            Byte.valueOf(value);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }, (value) -> {
        return Byte.valueOf(value);
    }),
    LONG("Long", new Resource[]{
        XSD.xlong,
        XSD.unsignedLong
    }, Long.class, (value) -> {
        try {
            Long.valueOf(value);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }, (value) -> {
        return Long.valueOf(value);
    }),
    SHORT("Short", new Resource[]{
        XSD.xshort,
        XSD.unsignedShort
    }, Short.class, (value) -> {
        try {
            Short.valueOf(value);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }, (value) -> {
        return Short.valueOf(value);
    }),
    DOUBLE("Double", XSD.xdouble, Double.class, (value) -> {
        try {
            Double.valueOf(value);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }, (value) -> {
        return Double.valueOf(value);
    }),
    FLOAT("Float", new Resource[]{
        XSD.xfloat,
        XSD.decimal
    }, Float.class, (value) -> {
        try {
            Float.valueOf(value);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }, (value) -> {
        return Float.valueOf(value);
    });

    private List<String> datatypeURIs = new ArrayList<>();
    private Class<?> typeClass;
    private String label;
    private Function<String, Boolean> validator;
    private Function<String, ?> deserializer;

    <T> BuiltInDatatypes(String label, Resource r, Class<T> typeClass, Function<String, Boolean> validator, Function<String, T> deserializer) {
        datatypeURIs.add(r.getURI());
        this.typeClass = typeClass;
        this.label = label;
        this.validator = validator;
        this.deserializer = deserializer;
    }

    <T> BuiltInDatatypes(String label, Resource[] rs, Class<T> typeClass, Function<String, Boolean> validator, Function<String, T> deserializer) {
        for (Resource r : rs) {
            datatypeURIs.add(r.getURI());
        }
        this.typeClass = typeClass;
        this.label = label;
        this.validator = validator;
        this.deserializer = deserializer;
    }

    List<String> getURIs() {
        return datatypeURIs;
    }

    public String getLabel() {
        return this.label;
    }

    public Class<?> getTypeClass() {
        return this.typeClass;
    }

    public boolean validate(String value) {
        return this.validator.apply(value);
    }

    public Object deserialize(String value) {
        return this.deserializer.apply(value);
    }

    public static Map<String, BuiltInDatatypes> builtInDatatypes = new HashMap<>();

    static {
        for (BuiltInDatatypes type : BuiltInDatatypes.values()) {
            for (String typeURI : type.getURIs()) {
                builtInDatatypes.put(typeURI, type);
            }
        }
    }

    public static boolean isBuiltInDatatype(URI dataType) {
        return isBuiltInDatatype(dataType.toString());
    }

    public static boolean isBuiltInDatatype(Resource dataType) {
        return isBuiltInDatatype(dataType.getURI());
    }

    public static boolean isBuiltInDatatype(String dataType) {
        return builtInDatatypes.containsKey(SPARQLDeserializers.getExpandedURI(dataType));
    }

    public static BuiltInDatatypes getBuiltInDatatype(Resource dataType) {
        return getBuiltInDatatype(dataType.getURI());
    }

    public static BuiltInDatatypes getBuiltInDatatype(URI dataType) {
        return getBuiltInDatatype(dataType.toString());
    }

    public static BuiltInDatatypes getBuiltInDatatype(String dataType) {
        return builtInDatatypes.get(SPARQLDeserializers.getExpandedURI(dataType));
    }
}
