package pl.ccteamone.filmvault.appuser.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.ccteamone.filmvault.appuser.dto.AppUserDto;
import pl.ccteamone.filmvault.appuser.dto.AppUserProfileDto;
import pl.ccteamone.filmvault.appuser.security.config.JwtService;
import pl.ccteamone.filmvault.appuser.service.AppUserService;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class AppUserController {
    private final AppUserService appUserService;
    private final JwtService jwtService;

    public AppUserController(AppUserService appUserService, JwtService jwtService) {
        this.appUserService = appUserService;
        this.jwtService = jwtService;
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/add-movie-title")
    public AppUserDto addMovieByTitleToUser(@PathVariable String username, @RequestParam String movieTitle,
                                            @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.addMovieByTitle(username, movieTitle);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/add-movie-id")
    public AppUserDto addMovieByMovieIdToUser(@PathVariable String username, @RequestParam Long movieId,
                                              @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.addMovieByMovieIdToUser(username, movieId);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/add-movieid")
    public AppUserDto addMovieByApiIdToUser(@PathVariable String username, @RequestParam Long movieApiId,
                                            @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.addMovieByApiId(username, movieApiId);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }


    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/add/movie")
    public AppUserDto addMovieByID(@PathVariable(value = "username") String username, @RequestParam("movieid") Long movieID,
                                   @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.addMovieByID(username, movieID);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/add/tvseries") // <-- MUSI BYC USERNAME
    public AppUserDto addTvSeriesByID(@PathVariable(value = "username") String username, @RequestParam("tvseriesid") Long tvseriesID,
                                      @RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.addTvSeriesByID(username, tvseriesID);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }

    //TODO: Set public fuction for searching users (public profile)
    // FUTURE -> Change appUserDTO to profileDTO with hidden private informations

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping()
    public List<AppUserProfileDto> getUsersList() {
        log.info("someone asked for an appUsers list");
        return appUserService.getUsersList();
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @GetMapping("/{userId}")
    public AppUserProfileDto getUserById(@PathVariable Long userId) {
        log.info("someone asked for user with id - {}", userId);
        return appUserService.getUserById(userId);
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @GetMapping("/userdata")
    public AppUserDto getUserByUsername(@RequestParam String username,
                                        @RequestHeader("Authorization") String bearerToken) {
        log.info("someone asked for user with name - {}", username);
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.getUserDtoByUsername(username);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PatchMapping("/update")
    public AppUserDto updateUser(@RequestParam String username, @RequestBody AppUserDto request,
                                 @RequestHeader("Authorization") String bearerToken) {
        log.info("user update with id - {} has been triggered, data: {}", username, request);
        String token = bearerToken.substring(7);
        String extractedUsername = jwtService.extractUserName(token);
        if (extractedUsername.equals(username)) {
            return appUserService.updateUserByUsername(username, request);
        } else {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/platforms/add")
    public AppUserDto addPlatform(@PathVariable(name = "username") String username, @RequestParam(name = "id") Long id) {
        return appUserService.addPlatformById(username, id);
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @PostMapping("/{username}/platforms/remove")
    public AppUserDto removePlatform(@PathVariable(name = "username") String username, @RequestParam(name = "id") Long id) {
        return appUserService.removePlatformById(username, id);
    }
    /*   @PatchMapping("/platforms")*/

    //TODO: logic and scope of deleted entities and inapp content (set movie tables/ratings created by Anonymous)
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{userId}")
    public void deleteUserById(@PathVariable Long userId) {
        log.info("someone ask to delete user with id - {}", userId);
        appUserService.deleteUserById(userId);
    }
}
