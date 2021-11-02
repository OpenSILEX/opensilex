package org.opensilex.core.address.api;

import org.opensilex.core.address.dal.AddressModel;

public abstract class AddressDTO<T extends AddressModel> {
    protected String countryName;

    protected String locality;

    protected String postalCode;

    protected String region;

    protected String streetAddress;

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public abstract T newModelInstance();

    public T newModel() {
        T model = newModelInstance();
        toModel(model);
        return model;
    }

    public void fromModel(T model) {
        setCountryName(model.getCountryName());
        setLocality(model.getLocality());
        setStreetAddress(model.getStreetAddress());
        setRegion(model.getRegion());
        setPostalCode(model.getPostalCode());
    }

    public void toModel(T model) {
        model.setCountryName(getCountryName());
        model.setLocality(getLocality());
        model.setStreetAddress(getStreetAddress());
        model.setRegion(getRegion());
        model.setPostalCode(getPostalCode());
    }
}
