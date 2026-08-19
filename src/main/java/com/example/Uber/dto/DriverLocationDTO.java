package com.example.Uber.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocationDTO {
    private String driverId;
    private double latitude;
    private double longitude;

}
