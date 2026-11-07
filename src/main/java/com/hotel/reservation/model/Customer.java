package com.hotel.reservation.model;

/** A guest profile. Application-level validation is owned by HotelService. */
public class Customer {
    private String id;
    private String name;
    private String email;
    private String phone;

    public Customer() {}

    public Customer(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getId() { return id; }
    public void setId(String value) { id = value; }

    public String getName() { return name; }
    public void setName(String value) { name = value; }

    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }

    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
}
