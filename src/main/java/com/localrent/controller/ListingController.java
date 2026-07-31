package com.localrent.controller;

import com.localrent.dto.ListingDtos.ListingRequest;
import com.localrent.dto.ListingDtos.ListingResponse;
import com.localrent.service.ListingService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/search")
    public List<ListingResponse> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Double maxPrice) {
        return listingService.search(
                ListingService.parseCategory(category), lat, lng, radiusKm, area, maxPrice);
    }

    @GetMapping("/mine")
    public List<ListingResponse> myListings(Principal principal) {
        return listingService.myListings(principal.getName());
    }

    @GetMapping("/{id}")
    public ListingResponse get(@PathVariable String id) {
        return listingService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(Principal principal, @Valid @RequestBody ListingRequest request) {
        return listingService.create(principal.getName(), request);
    }

    @PutMapping("/{id}")
    public ListingResponse update(
            Principal principal, @PathVariable String id, @Valid @RequestBody ListingRequest request) {
        return listingService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable String id) {
        listingService.delete(principal.getName(), id);
    }
}
