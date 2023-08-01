package pl.ccteamone.filmvault.movie;

import jakarta.persistence.*;
import lombok.*;
import pl.ccteamone.filmvault.media.Media;
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
//@DiscriminatorValue("Movie")
public class Movie extends Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;
    private String title;
    private String posterPath;
    @Column(length = 2048)
    private String overview;
    private LocalDate releaseDate;
    private Integer runtime;
    private Double popularity;
    private Double rating;
    private Integer voteCount;
    private LocalDate lastUpdate;
    private Long apiID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany
    private Set<VODPlatform> vodPlatforms;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany
    private Set<Region> regions;

    @ManyToMany
    private Set<Genre> genres;

}

