package com.busreservation.config;

import com.busreservation.model.*;
import com.busreservation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StaffRepository staffRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setFullName("Admin User");
            admin.setEmail("admin@test.com");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);

            User user = new User();
            user.setFullName("Normal User");
            user.setEmail("user@test.com");
            user.setPassword(passwordEncoder.encode("password"));
            user.setRole("ROLE_USER");
            userRepository.save(user);

            Bus bus1 = new Bus();
            bus1.setBusName("Volvo AC Sleeper");
            bus1.setBusNumber("UP85-AB1234");
            bus1.setTotalSeats(30);
            busRepository.save(bus1);

            Bus bus2 = new Bus();
            bus2.setBusName("Express Non-AC");
            bus2.setBusNumber("DL01-CD5678");
            bus2.setTotalSeats(45);
            busRepository.save(bus2);

            Schedule schedule1 = new Schedule();
            schedule1.setBus(bus1);
            schedule1.setSource("Mathura");
            schedule1.setDestination("Delhi");
            schedule1.setDepartureTime(LocalDateTime.now().plusHours(2));
            schedule1.setArrivalTime(LocalDateTime.now().plusHours(5));
            schedule1.setPrice(550.00);
            schedule1.setAvailableSeats(bus1.getTotalSeats());
            scheduleRepository.save(schedule1);

            System.out.println("Default users and sample data loaded!");
        }

        if (staffRepository.count() == 0) {
            Staff driver1 = new Staff(null, "Ramesh Kumar", "ramesh.k@bus.com", "9876543210", "Driver", "DL-2023-12345",
                    "Active", null);
            staffRepository.save(driver1);

            Staff driver2 = new Staff(null, "Suresh Singh", "suresh.s@bus.com", "9876543211", "Driver", "DL-2022-67890",
                    "On Leave", null);
            staffRepository.save(driver2);

            Staff conductor1 = new Staff(null, "Mahesh Yadav", "mahesh.y@bus.com", "9876543212", "Conductor", null,
                    "Active", null);
            staffRepository.save(conductor1);

            Staff conductor2 = new Staff(null, "Rajesh Gupta", "rajesh.g@bus.com", "9876543213", "Conductor", null,
                    "Active", null);
            staffRepository.save(conductor2);

            Staff manager = new Staff(null, "Vikram Malhotra", "vikram.m@bus.com", "9876543214", "Manager", null,
                    "Active", null);
            staffRepository.save(manager);

            System.out.println("Sample staff data loaded!");
        }
    }
}