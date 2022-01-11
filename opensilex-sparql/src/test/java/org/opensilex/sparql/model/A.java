//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;


/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "A"
)
public class A extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasRelationToA"
    )
    private A a;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasRelationToB"
    )
    private B b;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasString"
    )
    private String string;

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
            property = "hasFloat"
    )
    private Float floatVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasDouble"
    )
    private Double doubleVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasChar"
    )
    private Character charVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasShort"
    )
    private Short shortVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasByte"
    )
    private Byte byteVar;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasDate"
    )
    private LocalDate date;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasDateTime"
    )
    private OffsetDateTime datetime;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = TEST_ONTOLOGY.uriToRenameString
    )
    private String propertyToRename;

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = TEST_ONTOLOGY.renamedUriString
    )
    private String renamedProperty;

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    public String getString() {
        return string;
    }

    public void setString(String aString) {
        this.string = aString;
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

    public Boolean isBool() {
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public OffsetDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(OffsetDateTime datetime) {
        this.datetime = datetime;
    }

    public String getPropertyToRename() {
        return propertyToRename;
    }

    public void setPropertyToRename(String propertyToRename) {
        this.propertyToRename = propertyToRename;
    }

    public String getRenamedProperty() {
        return renamedProperty;
    }

    public void setRenamedProperty(String renamedProperty) {
        this.renamedProperty = renamedProperty;
    }
}
