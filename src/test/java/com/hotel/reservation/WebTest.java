package com.hotel.reservation;

import com.hotel.reservation.model.*;
import com.hotel.reservation.service.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class WebTest {
    static final Path DATA;
    static {try{DATA=Files.createTempDirectory(Path.of("target"),"aurelia-web-test-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    @DynamicPropertySource static void settings(DynamicPropertyRegistry registry){registry.add("hotel.data-dir",()->DATA.toString());}
    @Autowired MockMvc mvc;
    @Autowired HotelService hotel;
    @Test void allPagesAndFormsRenderAndBookingFlowWorks() throws Exception {
        Customer c=hotel.saveCustomer(null,"Web Guest","web@example.com","0771234567");Room r=hotel.rooms().getFirst();
        for(String path:new String[]{"/","/rooms","/rooms/"+r.getId(),"/customers/register","/book","/admin","/admin/rooms","/admin/rooms/new","/admin/rooms/"+r.getId()+"/edit","/admin/customers","/admin/customers/"+c.getId(),"/admin/customers/"+c.getId()+"/edit","/reservations"})
            mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(post("/book").param("roomId",r.getId()).param("customerId",c.getId()).param("checkIn",LocalDate.now().plusDays(2).toString()).param("checkOut",LocalDate.now().plusDays(4).toString()).param("guests","2")).andExpect(status().is3xxRedirection());
        Reservation b=hotel.reservations().getFirst();
        mvc.perform(get("/confirmation/"+b.getId())).andExpect(status().isOk()).andExpect(content().string(containsString("240.00")));
        mvc.perform(get("/reservations")).andExpect(status().isOk()).andExpect(content().string(containsString("Web Guest")));
        mvc.perform(get("/book").param("id",b.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\""+b.getCheckIn()+"\"")));
        mvc.perform(post("/reservations/"+b.getId()+"/cancel")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/reservations/"+b.getId()+"/delete")).andExpect(status().is3xxRedirection());
    }
    @Test void invalidInputsShowHelpfulErrors() throws Exception {
        mvc.perform(post("/customers").param("name","").param("email","bad").param("phone","1")).andExpect(status().isBadRequest()).andExpect(content().string(containsString("Enter a name")));
        mvc.perform(get("/rooms").param("in","bad")).andExpect(status().isBadRequest());
        mvc.perform(get("/rooms/missing")).andExpect(status().isBadRequest()).andExpect(content().string(containsString("Room not found")));
    }
}
