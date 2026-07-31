package com.localrent.dto;

import com.localrent.model.RentalCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class ListingDtos {

    private ListingDtos() {
    }

    public record ListingRequest(
            @NotBlank String title,
            String description,
            @NotNull RentalCategory category,
            @DecimalMin("0.0") double pricePerDay,
            @NotBlank String areaName,
            String city,
            String addressLine,
            @NotNull Double latitude,
            @NotNull Double longitude,
            List<String> imageUrls,
            Boolean available,
            String contactPhone) {
    }

    public record ListingResponse(
            String id,
            String ownerId,
            String ownerName,
            String title,
            String description,
            RentalCategory category,
            double pricePerDay,
            String areaName,
            String city,
            String addressLine,
            double latitude,
            double longitude,
            List<String> imageUrls,
            boolean available,
            String contactPhone,
            Double distanceKm) {
    }
}
