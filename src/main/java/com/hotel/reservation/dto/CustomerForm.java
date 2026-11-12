package com.hotel.reservation.dto;

import com.hotel.reservation.model.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form object for registering or editing a customer. */
public class CustomerForm {

    @NotBlank(message = "Full name is required.")
    @Size(min = 2, max = 80, message = "Full name must be 2-80 characters.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Size(max = 120, message = "Email must be at most 120 characters.")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^\\+?[0-9](?:[ -]?[0-9]){8,14}$", message = "Please enter a valid phone number (9-15 digits).")
    private String phone;

    @NotBlank(message = "Address is required.")
    @Size(min = 3, max = 200, message = "Address must be 3-200 characters.")
    private String address;

    public static CustomerForm fromCustomer(Customer customer) {
        CustomerForm form = new CustomerForm();
        form.fullName = customer.getFullName();
        form.email = customer.getEmail();
        form.phone = customer.getPhone();
        form.address = customer.getAddress();
        return form;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
