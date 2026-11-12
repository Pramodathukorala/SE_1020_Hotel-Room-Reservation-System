package com.hotel.reservation.config;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import com.hotel.reservation.repository.CustomerRepository;
import com.hotel.reservation.repository.ReservationRepository;
import com.hotel.reservation.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adds sample rooms and sample customers when the application starts with an EMPTY database.
 *
 * If the database already contains any room, customer or reservation, nothing is added,
 * so records created, edited or deleted by the user are never overwritten on restart.
 * No sample reservations are created - every reservation is made through the real booking workflow.
 */
@Component
public class SampleDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataInitializer.class);

    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;
    private final boolean enabled;

    public SampleDataInitializer(RoomRepository roomRepository, CustomerRepository customerRepository,
                                 ReservationRepository reservationRepository,
                                 @Value("${hotel.seed-sample-data:true}") boolean enabled) {
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
        this.reservationRepository = reservationRepository;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        boolean databaseIsEmpty = roomRepository.count() == 0
                && customerRepository.count() == 0
                && reservationRepository.count() == 0;
        if (databaseIsEmpty) {
            seedRooms();
            seedCustomers();
        } else {
            log.info("Database contains {} room(s), {} customer(s), {} reservation(s) - no sample data added.",
                    roomRepository.count(), customerRepository.count(), reservationRepository.count());
        }
    }

    private void seedRooms() {
        String img = "/images/rooms/";
        roomRepository.save(new StandardRoom("R001", "101", "Garden Standard",
                "A calm, light-filled retreat overlooking the frangipani garden, with soft linens and a writing desk.",
                new BigDecimal("18500"), 2, List.of("Free Wi-Fi", "Air conditioning", "Rain shower", "Smart TV", "Tea & coffee"),
                img + "standard-garden.jpg", false, "Queen"));
        roomRepository.save(new StandardRoom("R002", "102", "City Standard",
                "Contemporary comfort with floor-to-ceiling windows and a quiet, well-designed work corner.",
                new BigDecimal("16500"), 2, List.of("Free Wi-Fi", "Air conditioning", "Work desk", "Smart TV"),
                img + "standard-city.jpg", false, "Queen"));
        roomRepository.save(new StandardRoom("R003", "103", "Twin Standard",
                "Two comfortable single beds - ideal for friends or colleagues travelling together.",
                new BigDecimal("17500"), 2, List.of("Free Wi-Fi", "Air conditioning", "Mini fridge", "Smart TV"),
                img + "standard-twin.jpg", false, "Twin"));
        roomRepository.save(new DeluxeRoom("R004", "201", "Deluxe King",
                "A generous king room in warm timber tones, with a lounge chair and a curated mini bar.",
                new BigDecimal("29500"), 2, List.of("Free Wi-Fi", "Mini bar", "Bathtub", "Nespresso machine", "Bathrobes"),
                img + "deluxe-king.jpg", false, "City", false));
        roomRepository.save(new DeluxeRoom("R005", "202", "Deluxe Garden Retreat",
                "Opens onto a private terrace surrounded by tropical greenery - breakfast outdoors is a must.",
                new BigDecimal("32000"), 3, List.of("Free Wi-Fi", "Mini bar", "Outdoor seating", "Rain shower", "Bathrobes"),
                img + "deluxe-garden.jpg", false, "Garden", true));
        roomRepository.save(new DeluxeRoom("R006", "203", "Deluxe Ocean Terrace",
                "Wake to the sound of the waves. Wide doors open to a balcony with uninterrupted ocean views.",
                new BigDecimal("35000"), 3, List.of("Free Wi-Fi", "Mini bar", "Bathtub", "Nespresso machine", "Daily turndown"),
                img + "deluxe-terrace.jpg", false, "Ocean", true));
        roomRepository.save(new SuiteRoom("R007", "301", "Panorama Suite",
                "A sweeping open-plan suite with a glass-walled bathroom and panoramic views of the hills.",
                new BigDecimal("58000"), 4, List.of("Free Wi-Fi", "Living room", "Soaking tub", "Bar", "Airport transfer"),
                img + "suite-panorama.jpg", false, 2, false));
        roomRepository.save(new SuiteRoom("R008", "302", "Heritage Suite",
                "Classic elegance with a separate sitting room, antique details and a deep soaking tub.",
                new BigDecimal("52000"), 3, List.of("Free Wi-Fi", "Living room", "Soaking tub", "Mini bar"),
                img + "suite-heritage.jpg", false, 1, false));
        roomRepository.save(new SuiteRoom("R009", "401", "Royal Residence",
                "Our finest residence: two bedrooms, a private dining area and a dedicated butler for your stay.",
                new BigDecimal("75000"), 5, List.of("Free Wi-Fi", "Private dining", "Soaking tub", "Bar", "Airport transfer", "Spa credit"),
                img + "suite-royal.jpg", false, 2, true));
        log.info("Empty database: added 9 sample rooms.");
    }

    private void seedCustomers() {
        customerRepository.save(new Customer("C001", "Nimal Perera", "nimal.perera@example.com", "+94 77 123 4567", "12 Galle Road, Colombo 03"));
        customerRepository.save(new Customer("C002", "Ayesha Fernando", "ayesha.fernando@example.com", "+94 71 555 0182", "45 Temple Street, Kandy"));
        customerRepository.save(new Customer("C003", "Daniel Silva", "daniel.silva@example.com", "+94 76 220 9031", "8 Lake Drive, Negombo"));
        log.info("Empty database: added 3 sample customers.");
    }
}
