package com.hotel.reservation.model;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.util.List;
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
@JsonSubTypes({@JsonSubTypes.Type(value=StandardRoom.class,name="STANDARD"),@JsonSubTypes.Type(value=DeluxeRoom.class,name="DELUXE"),@JsonSubTypes.Type(value=SuiteRoom.class,name="SUITE")})
public abstract class Room {
 private String id, number, description;
 private int capacity;
 private BigDecimal basePrice;
 private boolean maintenance;
 protected Room() {}
 protected Room(String id,String number,int capacity,BigDecimal basePrice,String description,boolean maintenance){this.id=id;this.number=number;this.capacity=capacity;this.basePrice=basePrice;this.description=description;this.maintenance=maintenance;}
 public abstract BigDecimal calculateNightlyRate();
 @com.fasterxml.jackson.annotation.JsonIgnore public abstract List<String> getRoomFeatures();
 @com.fasterxml.jackson.annotation.JsonIgnore public abstract String getCategory();
 public String getId(){return id;} public void setId(String value){id=value;}
 public String getNumber(){return number;} public void setNumber(String value){number=value;}
 public int getCapacity(){return capacity;} public void setCapacity(int value){capacity=value;}
 public BigDecimal getBasePrice(){return basePrice;} public void setBasePrice(BigDecimal value){basePrice=value;}
 public String getDescription(){return description;} public void setDescription(String value){description=value;}
 public boolean isMaintenance(){return maintenance;} public void setMaintenance(boolean value){maintenance=value;}
}
