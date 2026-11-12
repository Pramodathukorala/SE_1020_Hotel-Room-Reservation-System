package com.hotel.reservation.repository.file;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Stores customers in data/customers.txt. */
@Repository
public class FileCustomerRepository extends AbstractFileRepository<Customer> implements CustomerRepository {

    public FileCustomerRepository(@Value("${hotel.storage.data-dir:data}") String dataDirectory) {
        super(Path.of(dataDirectory), "customers.txt", "C", 3);
    }

    @Override
    protected String fileHeader() {
        return "# id|fullName|email|phone|address";
    }

    @Override
    protected List<String> toFields(Customer c) {
        return List.of(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getAddress());
    }

    @Override
    protected Customer fromFields(String[] f) {
        requireFieldCount(f, 5);
        return new Customer(f[0], f[1], f[2], f[3], f[4]);
    }

    @Override
    public synchronized Optional<Customer> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(customer -> customer.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();
    }
}
