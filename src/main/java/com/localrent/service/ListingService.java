package com.localrent.service;

import com.localrent.dto.ListingDtos.ListingRequest;
import com.localrent.dto.ListingDtos.ListingResponse;
import com.localrent.model.Listing;
import com.localrent.model.RentalCategory;
import com.localrent.model.Role;
import com.localrent.model.User;
import com.localrent.repository.ListingRepository;
import com.localrent.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListingService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final double defaultRadiusKm;
    private final double maxRadiusKm;

    public ListingService(
            ListingRepository listingRepository,
            UserRepository userRepository,
            MongoTemplate mongoTemplate,
            @Value("${localrent.search.default-radius-km}") double defaultRadiusKm,
            @Value("${localrent.search.max-radius-km}") double maxRadiusKm) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.defaultRadiusKm = defaultRadiusKm;
        this.maxRadiusKm = maxRadiusKm;
    }

    public ListingResponse create(String ownerId, ListingRequest request) {
        User owner = requireOwner(ownerId);
        Listing listing = new Listing();
        listing.setOwnerId(owner.getId());
        apply(listing, request);
        listingRepository.save(listing);
        return toResponse(listing, owner.getName(), null);
    }

    public ListingResponse update(String ownerId, String listingId, ListingRequest request) {
        User owner = requireOwner(ownerId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own listings");
        }
        apply(listing, request);
        listingRepository.save(listing);
        return toResponse(listing, owner.getName(), null);
    }

    public void delete(String ownerId, String listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own listings");
        }
        listingRepository.delete(listing);
    }

    public List<ListingResponse> myListings(String ownerId) {
        List<Listing> listings = listingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        return withOwnerNames(listings, null, null);
    }

    public ListingResponse get(String listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        String ownerName = userRepository.findById(listing.getOwnerId()).map(User::getName).orElse(null);
        return toResponse(listing, ownerName, null);
    }

    /**
     * Search listings either by coordinates (radius in km, defaults to 20) or by area/city name.
     */
    public List<ListingResponse> search(
            RentalCategory category,
            Double latitude,
            Double longitude,
            Double radiusKm,
            String area,
            Double maxPrice) {
        Query query = new Query();
        query.addCriteria(Criteria.where("available").is(true));
        if (category != null) {
            query.addCriteria(Criteria.where("category").is(category));
        }
        if (maxPrice != null) {
            query.addCriteria(Criteria.where("pricePerDay").lte(maxPrice));
        }

        Double effectiveRadiusKm = null;
        if (latitude != null && longitude != null) {
            effectiveRadiusKm = Math.min(radiusKm == null ? defaultRadiusKm : radiusKm, maxRadiusKm);
            query.addCriteria(Criteria.where("location")
                    .withinSphere(new org.springframework.data.geo.Circle(
                            new Point(longitude, latitude),
                            new Distance(effectiveRadiusKm, Metrics.KILOMETERS))));
        } else if (area != null && !area.isBlank()) {
            String escaped = java.util.regex.Pattern.quote(area.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("areaName").regex(escaped, "i"),
                    Criteria.where("city").regex(escaped, "i")));
        }

        List<Listing> listings = mongoTemplate.find(query, Listing.class);
        return withOwnerNames(listings, latitude, longitude);
    }

    private List<ListingResponse> withOwnerNames(List<Listing> listings, Double latitude, Double longitude) {
        List<String> ownerIds = listings.stream().map(Listing::getOwnerId).distinct().toList();
        Map<String, String> ownerNames = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        List<ListingResponse> responses = new ArrayList<>();
        for (Listing listing : listings) {
            Double distanceKm = null;
            if (latitude != null && longitude != null && listing.getLocation() != null) {
                distanceKm = haversineKm(
                        latitude, longitude, listing.getLocation().getY(), listing.getLocation().getX());
            }
            responses.add(toResponse(listing, ownerNames.get(listing.getOwnerId()), distanceKm));
        }
        if (latitude != null && longitude != null) {
            responses.sort(Comparator.comparing(
                    ListingResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return responses;
    }

    private User requireOwner(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owners can manage listings");
        }
        return user;
    }

    private void apply(Listing listing, ListingRequest request) {
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setCategory(request.category());
        listing.setPricePerDay(request.pricePerDay());
        listing.setAreaName(request.areaName());
        listing.setCity(request.city());
        listing.setAddressLine(request.addressLine());
        listing.setLocation(new GeoJsonPoint(request.longitude(), request.latitude()));
        listing.setImageUrls(request.imageUrls() == null ? List.of() : request.imageUrls());
        listing.setAvailable(request.available() == null || request.available());
        listing.setContactPhone(request.contactPhone());
    }

    private ListingResponse toResponse(Listing listing, String ownerName, Double distanceKm) {
        return new ListingResponse(
                listing.getId(),
                listing.getOwnerId(),
                ownerName,
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getPricePerDay(),
                listing.getAreaName(),
                listing.getCity(),
                listing.getAddressLine(),
                listing.getLocation() == null ? 0 : listing.getLocation().getY(),
                listing.getLocation() == null ? 0 : listing.getLocation().getX(),
                listing.getImageUrls(),
                listing.isAvailable(),
                listing.getContactPhone(),
                distanceKm == null ? null : Math.round(distanceKm * 100) / 100.0);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static RentalCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return RentalCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category: " + raw);
        }
    }
}
