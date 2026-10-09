package com.example.Uber.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocationDTO {
    private Integer driverId;
    private double latitude;
    private double longitude;

}
