//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.model;

import java.util.List;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;



/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "B",
        graph = TEST_ONTOLOGY.GRAPH_SUFFIX
)
public class B extends SPARQLResourceModel {
    
    @SPARQLProperty(
        ontology = TEST_ONTOLOGY.class,
        property = "hasRelationToB",
        inverse = true
    )
    private A a;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasInt"
    )
    private Integer integer;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasLong"
    )
    private Long longVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasBoolean"
    )
    private Boolean bool;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasFloat",
            required = true
    )
    private Float floatVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasDouble",
            required = true
    )
    private Double doubleVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasChar",
            required = true
    )
    private Character charVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasShort",
            required = true
    )
    private Short shortVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasByte"
    )
    private Byte byteVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasStringList"
    )
    private List<String> stringList;
    
    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public Long getLongVar() {
        return longVar;
    }

    public void setLongVar(Long longVar) {
        this.longVar = longVar;
    }

    public Boolean getBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }

    public Float getFloatVar() {
        return floatVar;
    }

    public void setFloatVar(Float floatVar) {
        this.floatVar = floatVar;
    }

    public Double getDoubleVar() {
        return doubleVar;
    }

    public void setDoubleVar(Double doubleVar) {
        this.doubleVar = doubleVar;
    }

    public Character getCharVar() {
        return charVar;
    }

    public void setCharVar(Character charVar) {
        this.charVar = charVar;
    }

    public Short getShortVar() {
        return shortVar;
    }

    public void setShortVar(Short shortVar) {
        this.shortVar = shortVar;
    }

    public Byte getByteVar() {
        return byteVar;
    }

    public void setByteVar(Byte byteVar) {
        this.byteVar = byteVar;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }
}
