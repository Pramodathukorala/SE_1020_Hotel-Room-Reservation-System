package com.hotel.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Guest registration form. COMPOSITION: the personal details re-use CustomerForm
 * (and its validation); this class adds the password fields.
 */
public class RegistrationForm {

    @Valid
    private CustomerForm customer = new CustomerForm();

    @NotBlank(message = "Please choose a password.")
    @Size(min = 8, max = 72, message = "Password must be 8-72 characters.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).*$", message = "Password must contain at least one letter and one number.")
    private String password;

    @NotBlank(message = "Please type the password again.")
    private String confirmPassword;

    /** Checked by Bean Validation because the method name starts with "is" and it is annotated. */
    @AssertTrue(message = "The two passwords do not match.")
    public boolean isPasswordConfirmed() {
        return password == null || confirmPassword == null || Objects.equals(password, confirmPassword);
    }

    public CustomerForm getCustomer() { return customer; }
    public void setCustomer(CustomerForm customer) { this.customer = customer; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
