package com.example.Uber.service.impl;

import com.example.Uber.service.BookingService;
import com.example.ubersocket.RideAcceptanceRequest;
import com.example.ubersocket.RideAcceptanceResponse;
import com.example.ubersocket.RideServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideServiceImpl extends RideServiceGrpc.RideServiceImplBase{

    private final BookingService bookingService;

    @Override
    public void acceptRide(RideAcceptanceRequest request, StreamObserver<RideAcceptanceResponse> responseObserver) {
        // Call the BookingService to update the ride with the new driver id.
        Boolean success = bookingService.acceptRide(
                Long.parseLong("" + request.getBookingId()),
                request.getDriverId()
        );

        RideAcceptanceResponse response = RideAcceptanceResponse.newBuilder()
                .setSuccess(success)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
