package com.hotel.reservation.dto;

import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Data Transfer Object for the add/edit room form.
 * Spring binds the HTML form fields to this object and checks the annotations;
 * the RoomService then turns a valid form into the correct Room subclass.
 */
public class RoomForm {

    @NotBlank(message = "Room number is required.")
    @Pattern(regexp = "[A-Za-z0-9-]{1,10}", message = "Use 1-10 letters, digits or dashes.")
    private String roomNumber;

    @NotBlank(message = "Room name is required.")
    @Size(max = 80, message = "Room name must be at most 80 characters.")
    private String name;

    @NotNull(message = "Please choose a room type.")
    private RoomType type;

    @NotBlank(message = "Description is required.")
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;

    @NotNull(message = "Price per night is required.")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero.")
    @DecimalMax(value = "10000000", message = "Price is too large.")
    @Digits(integer = 8, fraction = 2, message = "Use at most 2 decimal places.")
    private BigDecimal basePrice;

    @NotNull(message = "Capacity is required.")
    @Min(value = 1, message = "Capacity must be at least 1 guest.")
    @Max(value = Room.MAX_CAPACITY, message = "Capacity cannot exceed " + Room.MAX_CAPACITY + " guests.")
    private Integer capacity;

    @Size(max = 500, message = "Amenities list is too long.")
    private String amenities;

    @Size(max = 300, message = "Image path is too long.")
    private String imageUrl;

    private boolean underMaintenance;

    // ---- type-specific fields (validated in RoomService according to the chosen type) ----
    private String bedType = "Queen";
    private String view = "Garden";
    private boolean balcony;
    private Integer bedrooms = 1;
    private boolean butlerService;

    /** Fills the form with an existing room so it can be edited. */
    public static RoomForm fromRoom(Room room) {
        RoomForm form = new RoomForm();
        form.roomNumber = room.getRoomNumber();
        form.name = room.getName();
        form.type = room.getType();
        form.description = room.getDescription();
        form.basePrice = room.getBasePrice();
        form.capacity = room.getCapacity();
        form.amenities = String.join(", ", room.getAmenities());
        form.imageUrl = room.getImageUrl();
        form.underMaintenance = room.isUnderMaintenance();
        if (room instanceof StandardRoom standard) {
            form.bedType = standard.getBedType();
        } else if (room instanceof DeluxeRoom deluxe) {
            form.view = deluxe.getView();
            form.balcony = deluxe.hasBalcony();
        } else if (room instanceof SuiteRoom suite) {
            form.bedrooms = suite.getBedrooms();
            form.butlerService = suite.hasButlerService();
        }
        return form;
    }

    /** "Wi-Fi, Mini bar" -> ["Wi-Fi", "Mini bar"] */
    public List<String> amenityList() {
        if (amenities == null || amenities.isBlank()) {
            return List.of();
        }
        return Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isUnderMaintenance() { return underMaintenance; }
    public void setUnderMaintenance(boolean underMaintenance) { this.underMaintenance = underMaintenance; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public String getView() { return view; }
    public void setView(String view) { this.view = view; }
    public boolean isBalcony() { return balcony; }
    public void setBalcony(boolean balcony) { this.balcony = balcony; }
    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }
    public boolean isButlerService() { return butlerService; }
    public void setButlerService(boolean butlerService) { this.butlerService = butlerService; }
}
