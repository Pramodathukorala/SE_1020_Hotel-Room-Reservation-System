package com.hotel.reservation.controller.admin;

import java.util.LinkedHashMap;
import java.util.Map;

/** Room photos bundled with the application (static/images/rooms), offered in the room form. */
final class RoomImages {

    static final Map<String, String> OPTIONS = new LinkedHashMap<>();

    static {
        OPTIONS.put("/images/rooms/standard-garden.jpg", "Standard - garden");
        OPTIONS.put("/images/rooms/standard-city.jpg", "Standard - city");
        OPTIONS.put("/images/rooms/standard-twin.jpg", "Standard - twin");
        OPTIONS.put("/images/rooms/deluxe-king.jpg", "Deluxe - king");
        OPTIONS.put("/images/rooms/deluxe-garden.jpg", "Deluxe - garden");
        OPTIONS.put("/images/rooms/deluxe-terrace.jpg", "Deluxe - terrace");
        OPTIONS.put("/images/rooms/suite-panorama.jpg", "Suite - panorama");
        OPTIONS.put("/images/rooms/suite-heritage.jpg", "Suite - heritage");
        OPTIONS.put("/images/rooms/suite-royal.jpg", "Suite - royal");
    }

    private RoomImages() {
    }
}
