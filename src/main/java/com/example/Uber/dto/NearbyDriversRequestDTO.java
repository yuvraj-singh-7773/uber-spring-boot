package com.example.Uber.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyDriversRequestDTO {
    private Double latitude;
    private Double longitude;
    private Double radius;
}
