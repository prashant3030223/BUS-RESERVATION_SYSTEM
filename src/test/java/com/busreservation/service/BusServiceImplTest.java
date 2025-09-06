package com.busreservation.service;

import com.busreservation.model.Bus;
import com.busreservation.repository.BusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Mockito ko JUnit 5 ke saath istemal karne ke liye
@ExtendWith(MockitoExtension.class)
class BusServiceImplTest {

    // Hum asli database (BusRepository) ki jagah ek nakli (mock) version banayenge
    @Mock
    private BusRepository busRepository;

    // Hum BusServiceImpl ko test karna chahte hain, aur Mockito ismein upar banaye gaye nakli repository ko inject kar dega
    @InjectMocks
    private BusServiceImpl busService;

    @Test
    void testGetBusById_WhenBusExists() {
        // 1. Arrange (Tayyari Karna)
        // Ek sample bus banayi
        Bus bus = new Bus();
        bus.setId(1L);
        bus.setBusName("Test Bus");

        // Mockito ko bataya ki jab bhi busRepository.findById(1L) call ho, toh upar banayi gayi sample bus return karna
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

        // 2. Act (Asli Method Call Karna)
        // busService ke method ko call kiya
        Optional<Bus> foundBus = busService.getBusById(1L);

        // 3. Assert (Result Check Karna)
        // Hum check kar rahe hain ki result sahi hai ya nahi
        assertTrue(foundBus.isPresent(), "Bus milni chahiye thi");
        assertEquals("Test Bus", foundBus.get().getBusName(), "Bus ka naam a a match hona chahiye");

        // Hum yeh bhi check kar sakte hain ki repository ka method sirf ek baar call hua
        verify(busRepository, times(1)).findById(1L);
    }

    @Test
    void testGetBusById_WhenBusDoesNotExist() {
        // 1. Arrange
        // Mockito ko bataya ki jab bhi busRepository.findById(2L) call ho, toh khaali (empty) result return karna
        when(busRepository.findById(2L)).thenReturn(Optional.empty());

        // 2. Act
        Optional<Bus> foundBus = busService.getBusById(2L);

        // 3. Assert
        assertFalse(foundBus.isPresent(), "Bus nahi milni chahiye thi");
    }
}