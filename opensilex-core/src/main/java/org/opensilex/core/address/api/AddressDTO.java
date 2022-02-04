package org.opensilex.core.address.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.address.dal.AddressModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An address corresponding to the VCARD specification.
 *
 * @param <T> AddressModel
 *
 * @author Valentin RIGOLLE
 */
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AddressDTO<?>)) {
            return false;
        }
        AddressDTO<?> otherAddress = (AddressDTO<?>) other;
        return Objects.equals(this.getCountryName(), otherAddress.getCountryName())
                && Objects.equals(this.getRegion(), otherAddress.getRegion())
                && Objects.equals(this.getPostalCode(), otherAddress.getPostalCode())
                && Objects.equals(this.getLocality(), otherAddress.getLocality())
                && Objects.equals(this.getStreetAddress(), otherAddress.getStreetAddress());
    }

    @JsonProperty(
            value = "readableAddress"
    )
    public String toReadableAddress() {
        List<String> segments = new ArrayList<>();
        if (StringUtils.isNotEmpty(this.getStreetAddress())) {
            segments.add(this.getStreetAddress());
        }
        if (StringUtils.isNotEmpty(this.getPostalCode())) {
            segments.add(this.getPostalCode());
        }
        if (StringUtils.isNotEmpty(this.getLocality())) {
            segments.add(this.getLocality());
        }
        if (StringUtils.isNotEmpty(this.getRegion())) {
            segments.add(this.getRegion());
        }
        if (StringUtils.isNotEmpty(this.getCountryName())) {
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
