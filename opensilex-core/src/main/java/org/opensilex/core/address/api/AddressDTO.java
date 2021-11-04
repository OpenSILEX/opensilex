package org.opensilex.core.address.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.address.dal.AddressModel;

import java.util.ArrayList;
import java.util.List;

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

    @JsonProperty(
            value = "readableAddress"
    )
    public String toReadableAddress() {
        List<String> segments = new ArrayList<String>();
        if (this.getStreetAddress() != null && !this.getStreetAddress().isEmpty()) {
            segments.add(this.getStreetAddress());
        }
        if (this.getPostalCode() != null && !this.getPostalCode().isEmpty()) {
            segments.add(this.getPostalCode());
        }
        if (this.getLocality() != null && !this.getLocality().isEmpty()) {
            segments.add(this.getLocality());
        }
        if (this.getRegion() != null && !this.getRegion().isEmpty()) {
            segments.add(this.getRegion());
        }
        if (this.getCountryName() != null && !this.getCountryName().isEmpty()) {
            segments.add(this.getCountryName());
        }
        return segments.stream().reduce("", (total, current) -> {
            if (!total.isEmpty()) {
                total += ", ";
            }
            total += current;
            return total;
        });
    }
}
