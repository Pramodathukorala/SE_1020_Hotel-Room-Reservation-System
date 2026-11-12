package com.hotel.reservation.service;

import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.repository.CustomerRepository;
import com.hotel.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Business logic for customers (registration, search, update, delete). */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;

    public CustomerService(CustomerRepository customerRepository, ReservationRepository reservationRepository) {
        this.customerRepository = customerRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll().stream()
                .sorted(Comparator.comparing(Customer::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Customer findById(String id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    public long count() {
        return customerRepository.count();
    }

    /** Matches ID, name, email or phone (case-insensitive). */
    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(c -> c.getId().toLowerCase(Locale.ROOT).contains(needle)
                        || c.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                        || c.getEmail().contains(needle)
                        || c.getPhone().replace(" ", "").contains(needle.replace(" ", "")))
                .toList();
    }

    public Customer register(CustomerForm form) {
        ensureEmailIsUnique(form.getEmail(), null);
        Customer customer = build(customerRepository.nextId(), form);
        return customerRepository.save(customer);
    }

    public Customer update(String id, CustomerForm form) {
        Customer existing = findById(id);
        ensureEmailIsUnique(form.getEmail(), existing.getId());
        return customerRepository.save(build(existing.getId(), form));
    }

    /**
     * Used by the public booking page: a returning guest is recognised by email address,
     * so the same person does not create duplicate customer records.
     */
    public Customer findOrRegister(CustomerForm form) {
        if (form.getEmail() != null) {
            var existing = customerRepository.findByEmail(form.getEmail());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return register(form);
    }

    /** Customers with reservations on record cannot be deleted (their bookings would lose their owner). */
    public void delete(String id) {
        Customer customer = findById(id);
        int linked = reservationRepository.findByCustomerId(id).size();
        if (linked > 0) {
            throw new ValidationException(customer.getFullName() + " has " + linked
                    + " reservation(s) on record. Cancel and delete those reservations first.");
        }
        customerRepository.deleteById(id);
    }

    private void ensureEmailIsUnique(String email, String ownId) {
        customerRepository.findByEmail(email)
                .filter(other -> !other.getId().equals(ownId))
                .ifPresent(other -> {
                    throw new ValidationException("email", "A customer with this email address is already registered.");
                });
    }

    private static Customer build(String id, CustomerForm form) {
        try {
            return new Customer(id, form.getFullName(), form.getEmail(), form.getPhone(), form.getAddress());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }
}
