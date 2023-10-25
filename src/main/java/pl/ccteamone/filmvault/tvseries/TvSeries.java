package pl.ccteamone.filmvault.tvseries;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import pl.ccteamone.filmvault.genre.Genre;
import pl.ccteamone.filmvault.region.Region;
import pl.ccteamone.filmvault.vodplatform.VODPlatform;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TvSeries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2048)
    private String overview;
    private String posterPath;
    private boolean adult;
    private String originLanguage;
    private String originCountry;
    private LocalDate firstAirDate;
    private LocalDate lastAirDate;
    private int seasons;
    private int episodes;
    private Double popularity;
    private LocalDate lastUpdate;
    private Long apiID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany
    private Set<Region> regions;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany
    private Set<VODPlatform> vodPlatforms;

    @ManyToMany
    private Set<Genre> genres;


}
