package com.onlyrooms.landlord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LandlordProfileRequest {

    @NotBlank
    @Size(max = 50)
    private String landlordType;

    @NotBlank
    @Size(max = 300)
    private String address;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 100)
    private String state;

    @NotBlank
    @Size(max = 10)
    private String postalCode;

    public String getLandlordType() { return landlordType; }
    public void setLandlordType(String landlordType) {
        this.landlordType = landlordType;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() { return city; }
    public void setCity(String city) {
        this.city = city;
    }

    public String getState() { return state; }
    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}