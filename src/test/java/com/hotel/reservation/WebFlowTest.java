package com.hotel.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the full Spring application (controllers + Thymeleaf + services + file storage)
 * against a temporary data folder, and drives it through HTTP requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebFlowTest {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void dataFolder(DynamicPropertyRegistry registry) {
        registry.add("hotel.storage.data-dir", () -> dataDir.toString());
    }

    @Autowired
    MockMvc mvc;

    private static final String IN = LocalDate.now().plusDays(10).toString();
    private static final String OUT = LocalDate.now().plusDays(12).toString();

    @Test
    void publicAndAdminPagesRender() throws Exception {
        String[] pages = {"/", "/rooms", "/rooms/R001", "/manage-booking", "/admin", "/admin/rooms", "/admin/rooms/new",
                "/admin/customers", "/admin/customers/new", "/admin/reservations", "/admin/reservations/new"};
        for (String page : pages) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
        mvc.perform(get("/")).andExpect(content().string(containsString("Heritage Suite")));   // cheapest seeded suite is featured
    }

    @Test
    void unknownRoomShowsFriendlyNotFoundPage() throws Exception {
        mvc.perform(get("/rooms/R404")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("was not found")));
    }

    @Test
    void completeGuestBookingWorkflow() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/book").param("roomId", "R002").param("checkIn", IN).param("checkOut", OUT).param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Available for 2 nights")));

        String location = mvc.perform(post("/book").session(session)
                        .param("roomId", "R002").param("checkIn", IN).param("checkOut", OUT).param("guests", "2")
                        .param("customer.fullName", "Web Test Guest").param("customer.email", "web.guest@example.com")
                        .param("customer.phone", "+94 77 000 1111").param("customer.address", "1 Test Lane, Colombo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/booking/confirmation/RES-*"))
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(location).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Web Test Guest")));

        // the same room and dates can no longer be booked
        mvc.perform(post("/book")
                        .param("roomId", "R002").param("checkIn", IN).param("checkOut", OUT).param("guests", "1")
                        .param("customer.fullName", "Second Guest").param("customer.email", "second@example.com")
                        .param("customer.phone", "0771112222").param("customer.address", "2 Test Lane"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already booked")));
    }

    @Test
    void invalidBookingShowsValidationMessages() throws Exception {
        mvc.perform(post("/book").param("roomId", "R001").param("checkIn", IN).param("checkOut", IN).param("guests", "1")
                        .param("customer.fullName", "").param("customer.email", "nope")
                        .param("customer.phone", "1").param("customer.address", "x"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Please enter a valid email address.")))
                .andExpect(content().string(containsString("Full name is required.")));
    }

    @Test
    void adminRoomCrudThroughTheWeb() throws Exception {
        mvc.perform(post("/admin/rooms").param("roomNumber", "777").param("name", "Web Room").param("type", "STANDARD")
                        .param("description", "Created in a test").param("basePrice", "12000").param("capacity", "2")
                        .param("bedType", "Twin"))
                .andExpect(redirectedUrl("/admin/rooms"));

        mvc.perform(get("/admin/rooms").param("q", "777"))
                .andExpect(content().string(containsString("Web Room")));

        mvc.perform(post("/admin/rooms").param("roomNumber", "777").param("name", "Duplicate").param("type", "STANDARD")
                        .param("description", "x").param("basePrice", "100").param("capacity", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    void customerWithReservationCannotBeDeleted() throws Exception {
        mvc.perform(post("/admin/reservations").param("customerId", "C001").param("roomId", "R003")
                        .param("checkIn", IN).param("checkOut", OUT).param("guests", "1"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/customers/C001/delete"))
                .andExpect(redirectedUrl("/admin/customers"));
        mvc.perform(get("/admin/customers/C001")).andExpect(status().isOk());   // still exists
    }
}
