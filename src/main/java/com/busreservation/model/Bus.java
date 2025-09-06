package com.busreservation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Entity
@Table(name = "buses")
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotEmpty(message = "Bus name cannot be empty")
    private String busName;
    @NotEmpty(message = "Bus number cannot be empty")
    @Size(min = 5, max = 15, message = "Bus number must be between 5 and 15 characters")
    private String busNumber;
    @Min(value = 10, message = "Total seats must be at least 10")
    private int totalSeats;
    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL)
    private List<Schedule> schedules;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBusName() { return busName; }
    public void setBusName(String busName) { this.busName = busName; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public List<Schedule> getSchedules() { return schedules; }
    public void setSchedules(List<Schedule> schedules) { this.schedules = schedules; }
}