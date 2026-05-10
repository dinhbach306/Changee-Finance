package org.dinhb.microservice.core.user.web;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.dinhb.microservice.core.common.security.HeaderNames;
import org.dinhb.microservice.core.user.domain.UserProfile;
import org.dinhb.microservice.core.user.domain.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping
@RateLimiter(name = "userServiceRl")
public class UserProfileController {

    private final UserProfileRepository repository;

    public UserProfileController(UserProfileRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader(HeaderNames.X_USER_ID) UUID userId) {
        UserProfile profile = repository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return UserProfileResponse.from(profile);
    }

    @PutMapping("/me")
    @Transactional
    public UserProfileResponse update(
            @RequestHeader(HeaderNames.X_USER_ID) UUID userId,
            @Valid @RequestBody UpdateProfileRequest req) {
        UserProfile profile = repository.findById(userId)
                .orElseGet(() -> repository.save(new UserProfile(userId, req.fullName())));
        profile.update(req.fullName(), req.phone(), req.address(), req.dateOfBirth());
        return UserProfileResponse.from(profile);
    }

    @GetMapping("/{id}")
    public UserProfileResponse byId(@PathVariable UUID id) {
        UserProfile profile = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return UserProfileResponse.from(profile);
    }

    public record UpdateProfileRequest(
            @jakarta.validation.constraints.NotBlank String fullName,
            String phone,
            String address,
            LocalDate dateOfBirth
    ) {}

    public record UserProfileResponse(
            UUID userId,
            String fullName,
            String phone,
            String address,
            LocalDate dateOfBirth,
            String kycStatus
    ) {
        public static UserProfileResponse from(UserProfile p) {
            return new UserProfileResponse(
                    p.getUserId(), p.getFullName(), p.getPhone(),
                    p.getAddress(), p.getDateOfBirth(), p.getKycStatus().name());
        }
    }
}
