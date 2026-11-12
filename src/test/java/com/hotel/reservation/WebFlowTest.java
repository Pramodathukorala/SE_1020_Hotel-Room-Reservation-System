package com.hotel.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the full Spring application (controllers + Thymeleaf + services + JDBC repositories)
 * against an in-memory SQL database (profile "test"), and drives it through HTTP requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebFlowTest {

    @Autowired
    MockMvc mvc;

    private static final String IN = LocalDate.now().plusDays(10).toString();
    private static final String OUT = LocalDate.now().plusDays(12).toString();

    /** Signs in as the test administrator and returns the session to use for admin requests. */
    private MockHttpSession adminSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session).param("username", "admin").param("password", "TestAdmin123"))
                .andExpect(redirectedUrl("/admin"));
        return session;
    }

    private MockHttpSession registerGuest(String name, String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/register").session(session)
                        .param("customer.fullName", name).param("customer.email", email)
                        .param("customer.phone", "0771234567").param("customer.address", "1 Test Road")
                        .param("password", "Secret123").param("confirmPassword", "Secret123"))
                .andExpect(redirectedUrl("/my-bookings"));
        return session;
    }

    @Test
    void publicAndAdminPagesRender() throws Exception {
        for (String page : new String[]{"/", "/rooms", "/rooms/R001", "/manage-booking", "/login", "/register", "/admin/login"}) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
        MockHttpSession admin = adminSession();
        for (String page : new String[]{"/admin", "/admin/rooms", "/admin/rooms/new", "/admin/customers",
                "/admin/customers/new", "/admin/reservations", "/admin/reservations/new"}) {
            mvc.perform(get(page).session(admin)).andExpect(status().isOk());
        }
        mvc.perform(get("/")).andExpect(content().string(containsString("Heritage Suite")));   // cheapest seeded suite is featured
    }

    @Test
    void homePageHasNoAdminButton() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(not(containsString("href=\"/admin\""))))
                .andExpect(content().string(containsString("Sign in")));
    }

    @Test
    void adminPagesNeedAnAdminLogin() throws Exception {
        mvc.perform(get("/admin/rooms")).andExpect(redirectedUrl("/admin/login?next=/admin/rooms"));
        mvc.perform(post("/admin/rooms/R001/delete")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/login").param("username", "admin").param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Incorrect username or password.")));
        // a signed-in GUEST is still not an admin
        MockHttpSession guest = registerGuest("Guest Not Admin", "not.admin@example.com");
        mvc.perform(get("/admin").session(guest)).andExpect(redirectedUrl("/admin/login?next=/admin"));
    }

    @Test
    void guestRegistersBooksAndManagesOwnBookings() throws Exception {
        mvc.perform(get("/my-bookings")).andExpect(redirectedUrl("/login?next=/my-bookings"));

        MockHttpSession guest = registerGuest("Kamala Silva", "kamala@example.com");
        String in = LocalDate.now().plusDays(40).toString();
        String out = LocalDate.now().plusDays(42).toString();
        // the booking form uses the account's details automatically
        mvc.perform(get("/book").session(guest).param("roomId", "R004").param("checkIn", in).param("checkOut", out))
                .andExpect(content().string(containsString("kamala@example.com")));
        String location = mvc.perform(post("/book").session(guest)
                        .param("roomId", "R004").param("checkIn", in).param("checkOut", out).param("guests", "1")
                        .param("customer.fullName", "x").param("customer.email", "not-an-email")
                        .param("customer.phone", "0779999999").param("customer.address", "Elsewhere"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        String reference = location.substring(location.lastIndexOf('/') + 1);

        // it is listed under the signed-in guest, not under the details typed into the form
        mvc.perform(get("/my-bookings").session(guest))
                .andExpect(content().string(containsString(reference)))
                .andExpect(content().string(containsString("Kamala Silva")));

        // UPDATE: extend the stay by one night
        String newOut = LocalDate.now().plusDays(43).toString();
        mvc.perform(post("/my-bookings/" + reference).session(guest)
                        .param("checkIn", in).param("checkOut", newOut).param("guests", "2"))
                .andExpect(redirectedUrl("/my-bookings/" + reference));
        mvc.perform(get("/my-bookings/" + reference).session(guest))
                .andExpect(content().string(containsString("3 nights")));

        // another guest cannot see, change or cancel it
        MockHttpSession other = registerGuest("Other Person", "other.person@example.com");
        mvc.perform(get("/my-bookings/" + reference).session(other)).andExpect(status().isNotFound());
        mvc.perform(post("/my-bookings/" + reference + "/cancel").session(other)).andExpect(status().isNotFound());

        // DELETE is refused while the booking is active, allowed after cancelling
        mvc.perform(post("/my-bookings/" + reference + "/delete").session(guest)).andExpect(redirectedUrl("/my-bookings"));
        mvc.perform(get("/my-bookings/" + reference).session(guest)).andExpect(status().isOk());
        mvc.perform(post("/my-bookings/" + reference + "/cancel").session(guest)).andExpect(redirectedUrl("/my-bookings"));
        mvc.perform(post("/my-bookings/" + reference + "/delete").session(guest)).andExpect(redirectedUrl("/my-bookings"));
        mvc.perform(get("/my-bookings/" + reference).session(guest)).andExpect(status().isNotFound());

        // sign out, then sign in again with email + password
        mvc.perform(post("/logout").session(guest)).andExpect(redirectedUrl("/"));
        mvc.perform(get("/my-bookings").session(guest)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").session(guest).param("email", "kamala@example.com").param("password", "Secret123"))
                .andExpect(redirectedUrl("/my-bookings"));
        mvc.perform(post("/login").param("email", "kamala@example.com").param("password", "Wrong1234"))
                .andExpect(content().string(containsString("Incorrect email or password.")));
    }

    @Test
    void registrationValidatesPasswords() throws Exception {
        mvc.perform(post("/register")
                        .param("customer.fullName", "Weak Password").param("customer.email", "weak@example.com")
                        .param("customer.phone", "0771234567").param("customer.address", "1 Test Road")
                        .param("password", "short").param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Password must be 8-72 characters.")))
                .andExpect(content().string(containsString("The two passwords do not match.")));
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
        MockHttpSession admin = adminSession();
        mvc.perform(post("/admin/rooms").session(admin).param("roomNumber", "777").param("name", "Web Room").param("type", "STANDARD")
                        .param("description", "Created in a test").param("basePrice", "12000").param("capacity", "2")
                        .param("bedType", "Twin"))
                .andExpect(redirectedUrl("/admin/rooms"));

        mvc.perform(get("/admin/rooms").session(admin).param("q", "777"))
                .andExpect(content().string(containsString("Web Room")));

        mvc.perform(post("/admin/rooms").session(admin).param("roomNumber", "777").param("name", "Duplicate").param("type", "STANDARD")
                        .param("description", "x").param("basePrice", "100").param("capacity", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    void customerWithReservationCannotBeDeleted() throws Exception {
        MockHttpSession admin = adminSession();
        mvc.perform(post("/admin/reservations").session(admin).param("customerId", "C001").param("roomId", "R003")
                        .param("checkIn", IN).param("checkOut", OUT).param("guests", "1"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/customers/C001/delete").session(admin))
                .andExpect(redirectedUrl("/admin/customers"));
        mvc.perform(get("/admin/customers/C001").session(admin)).andExpect(status().isOk());   // still exists
    }
}
