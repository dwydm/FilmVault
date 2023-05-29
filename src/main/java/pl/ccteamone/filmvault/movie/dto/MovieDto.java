package pl.ccteamone.filmvault.movie.dto;


import lombok.Builder;
import lombok.Data;
import pl.ccteamone.filmvault.appuser.dto.AppUserDto;
import pl.ccteamone.filmvault.vodplatform.dto.VODPlatformDto;

import java.util.Set;

@Data
@Builder
public class MovieDto {
    private Long id;
    private String title;
    private String posterPath;
    private String overview;
    private String releaseDate;
    private String runtime;
    private String credits;
    private double rating;
    private Set<AppUserDto> appUsers;
    private Set<VODPlatformDto> vodPlatforms;
}