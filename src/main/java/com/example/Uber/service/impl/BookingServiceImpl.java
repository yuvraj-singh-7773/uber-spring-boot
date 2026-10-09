package com.example.Uber.service.impl;

import com.example.Uber.client.GrpcClient;
import com.example.Uber.dto.BookingRequest;
import com.example.Uber.dto.BookingResponse;
import com.example.Uber.dto.DriverLocationDTO;
import com.example.Uber.entity.Booking;
import com.example.Uber.entity.Driver;
import com.example.Uber.entity.Passenger;
import com.example.Uber.mapper.BookingMapper;
import com.example.Uber.repository.BookingRepository;
import com.example.Uber.repository.DriverRepository;
import com.example.Uber.repository.PassengerRepository;
import com.example.Uber.service.BookingService;
import com.example.Uber.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final BookingMapper bookingMapper;
    private final GrpcClient grpcClient;
    private final LocationService locationService;
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BookingResponse> findById(Long id) {
        return bookingRepository.findById(id)
                .map(bookingMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll().stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findByPassengerId(Long passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + passengerId));
        return bookingRepository.findByPassenger(passenger).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findByDriverId(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId));
        return bookingRepository.findByDriver(driver).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public BookingResponse create(BookingRequest request) {
        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + request.getPassengerId()));

        Driver driver = null;
        Booking.BookingStatus status = Booking.BookingStatus.PENDING;

        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + request.getDriverId()));

            // Check if driver is available
            if (!driver.getIsAvailable()) {
                throw new IllegalStateException("Driver with id " + request.getDriverId() + " is not available");
            }

            // Assign driver and mark as unavailable
            driver.setIsAvailable(false);
            driverRepository.save(driver);
            status = Booking.BookingStatus.CONFIRMED;

        }
        String pickupLat = request.getPickupLocationLatitude() != null
                ? request.getPickupLocationLatitude().toString()
                : null;
        String pickupLng = request.getPickupLocationLongitude() != null
                ? request.getPickupLocationLongitude().toString()
                : null;

        if (pickupLat == null || pickupLng == null) {
            throw new IllegalArgumentException("Pickup location latitude and longitude are required");
        }

        // // Set default fare if not provided
        BigDecimal fare = request.getFare();
        if (fare == null) {
            fare = BigDecimal.ZERO; // Default fare, can be calculated later
        }
        // // Create booking
        Booking newBooking = Booking.builder()
                .passenger(passenger)
                .driver(driver)
                .pickupLocationLatitude(pickupLat)
                .pickupLocationLongitude(pickupLng)
                .dropoffLocation(request.getDropoffLocation())
                .status(status)
                .fare(fare)
                .scheduledPickupTime(request.getScheduledPickupTime())
                .build();

        Booking savedBooking = bookingRepository.save(newBooking);

        List<DriverLocationDTO> nearbyDrivers = locationService.getNearbyDrivers(Double.parseDouble(pickupLat), Double.parseDouble(pickupLng), 10.0);

        grpcClient.notifyDriversForNewRide(pickupLat, pickupLng, Integer.parseInt(savedBooking.getId().toString()), nearbyDrivers.stream().map(DriverLocationDTO::getDriverId).collect(Collectors.toList()));
        return bookingMapper.toResponse(savedBooking);

    }
    
    @Override
    public BookingResponse update(Long id, BookingRequest request) {
//        Booking booking = bookingRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
//
//        Passenger passenger = passengerRepository.findById(request.getPassengerId())
//                .orElseThrow(() -> new IllegalArgumentException("Passenger not found with id: " + request.getPassengerId()));
//
//        Driver driver = null;
//        if (request.getDriverId() != null) {
//            driver = driverRepository.findById(request.getDriverId())
//                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + request.getDriverId()));
//        }
//
//        // Handle driver availability when updating
//        Driver previousDriver = booking.getDriver();
//        if (previousDriver != null && !previousDriver.equals(driver)) {
//            previousDriver.setIsAvailable(true);
//            driverRepository.save(previousDriver);
//        }
//
//        if (driver != null && !driver.equals(previousDriver)) {
//            if (!driver.getIsAvailable()) {
//                throw new IllegalArgumentException("Driver with id " + request.getDriverId() + " is not available");
//            }
//            driver.setIsAvailable(false);
//            driverRepository.save(driver);
//        }
//
//        bookingMapper.updateEntity(booking, request, passenger, driver);
//        Booking updatedBooking = bookingRepository.save(booking);
//        return bookingMapper.toResponse(updatedBooking);
        return null;
    }
    
    @Override
    public BookingResponse updateStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        
        booking.setStatus(status);
        
        // Handle status-specific logic
        if (status == Booking.BookingStatus.IN_PROGRESS && booking.getActualPickupTime() == null) {
            booking.setActualPickupTime(LocalDateTime.now());
        } else if (status == Booking.BookingStatus.COMPLETED) {
            booking.setCompletedAt(LocalDateTime.now());
            // Release driver
            if (booking.getDriver() != null) {
                Driver driver = booking.getDriver();
                driver.setIsAvailable(true);
                driverRepository.save(driver);
            }
        } else if (status == Booking.BookingStatus.CANCELLED) {
            // Release driver
            if (booking.getDriver() != null) {
                Driver driver = booking.getDriver();
                driver.setIsAvailable(true);
                driverRepository.save(driver);
            }
        }
        
        Booking updatedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(updatedBooking);
    }

    @Override
    public Boolean acceptRide(Long id, Integer driverId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId));

        booking.setDriver(driver);
        driver.setIsAvailable(false);
        driverRepository.save(driver);

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return true;
    }

    @Override
    public void deleteById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        
        // Release driver if assigned
        if (booking.getDriver() != null) {
            Driver driver = booking.getDriver();
            driver.setIsAvailable(true);
            driverRepository.save(driver);
        }
        
        bookingRepository.deleteById(id);
    }
}

