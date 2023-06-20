package pl.ccteamone.filmvault.movie.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import pl.ccteamone.filmvault.genre.service.GenreService;
import pl.ccteamone.filmvault.movie.Movie;
import pl.ccteamone.filmvault.movie.dto.CreditDto;
import pl.ccteamone.filmvault.movie.dto.MovieDto;
import pl.ccteamone.filmvault.movie.mapper.MovieMapper;
import pl.ccteamone.filmvault.movie.repository.MovieRepository;
import pl.ccteamone.filmvault.region.service.RegionService;
import pl.ccteamone.filmvault.vodplatform.dto.FileVODPlatformDto;
import pl.ccteamone.filmvault.vodplatform.service.VODPlatformService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MovieApiService movieApiService;
    private final MovieMapper movieMapper;

    private final GenreService genreService;
    private final VODPlatformService vodPlatformService;
    private final RegionService regionService;

    private final Integer PAGES_FROM_API = 5;
    private final Integer DAYS_BETWEEN_UPDATES = 7;

    public MovieDto createMovie(MovieDto create) {
        Movie movieFromDto = movieMapper.mapToMovie(create);
        return movieMapper.mapToMovieDto(movieRepository.save(movieFromDto));
    }

    public List<MovieDto> getMovieList() {
        return movieRepository.findAll().stream().map(movieMapper::mapToMovieDto).collect(Collectors.toList());
    }

    public MovieDto getMovieById(Long movieId) {
        MovieDto movie = movieMapper.mapToMovieDto(movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie id=" + movieId + " not found")));

        if (movie.getLastUpdate() == null || LocalDate.now().minusDays(DAYS_BETWEEN_UPDATES).isAfter(movie.getLastUpdate())) {
            movie = updateMovieDataFromApi(movieId, movie);
        }
        return movie;
    }

    public MovieDto updateMovie(Long movieId, MovieDto update) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie id=" + movieId + " not found"));

        if (update.getTitle() != null) {
            movie.setTitle(update.getTitle());
        }
        if (update.getPosterPath() != null) {
            movie.setPosterPath(update.getPosterPath());
        }
        if (update.getOverview() != null) {
            movie.setOverview(update.getOverview());
        }
        if (update.getReleaseDate() != null) {
            movie.setReleaseDate(update.getReleaseDate());
        }
        if (update.getRuntime() != null) {
            movie.setRuntime(update.getRuntime());
        }
        if (update.getRating() != null) {
            movie.setRating(update.getRating());
        }
        if (update.getApiID() != null) {
            movie.setApiID(update.getApiID());
        }
        if (update.getApiID() != null) {
            movie.setApiID(update.getApiID());
        } else {
            update.setApiID(movie.getApiID());
        }

        movie.setLastUpdate(LocalDate.now());
        return movieMapper.mapToMovieDto(movieRepository.save(movie));
    }

    public void deleteMovieById(Long movieId) {
        try {
            movieRepository.deleteById(movieId);
        } catch (EmptyResultDataAccessException e) {
            throw new EntityNotFoundException("Movie not found with id: " + movieId);
        }
    }

    public boolean existsByApiID(Long id) {
        return movieRepository.existsByApiID(id);
    }

    public MovieDto findMovieByApiID(Long apiID) {
        return movieMapper.mapToMovieDto(movieRepository.findByApiID(apiID)
                .orElseThrow(() -> new RuntimeException("Movie not found")));
    }

    // NGRAM SEARCH -> *** *** *** ***
    public Set<MovieDto> findMovieByQuery(String query) {
        feedDBWithNewMoviesByQuery(query);
        List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(query.substring(0, 1));
        Set<MovieDto> similarMovies = new HashSet<>();

        for (Movie movie : movies) {
            String title = movie.getTitle().toLowerCase();
            String lowercaseQuery = query.toLowerCase();

            List<String> titleNGrams = generateNGrams(title, 2); // licznik prawdopodobieństwa dla title
            List<String> queryNGrams = generateNGrams(lowercaseQuery, 2);  // licznik prawdopodobieństwa dla query

            int commonNGrams = countCommonNGrams(titleNGrams, queryNGrams);

            if (commonNGrams >= 2) { // <-- Licznik prawdopodobieństwa
                similarMovies.add(movies.stream()
                        .filter(match -> match.getTitle().equalsIgnoreCase(movie.getTitle()))
                        .findFirst()
                        .map(movieMapper::mapToMovieDto)
                        .orElseThrow(() -> new RuntimeException("Unable to match movie by title")));
            }
            if (similarMovies.size() == 20) {
                break;
            }
        }
        return similarMovies;
    }

    private List<String> generateNGrams(String input, int n) {
        List<String> nGrams = new ArrayList<>();

        for (int i = 0; i <= input.length() - n; i++) {
            String nGram = input.substring(i, i + n);
            nGrams.add(nGram);
        }

        return nGrams;
    }

    private int countCommonNGrams(List<String> nGrams1, List<String> nGrams2) {
        Set<String> set1 = new HashSet<>(nGrams1);
        Set<String> set2 = new HashSet<>(nGrams2);

        set1.retainAll(set2);

        return set1.size();
    }
    // <- NGRAM SEARCH *** *** *** ***

    public List<MovieDto> getNewestMovieList(Integer page) {
        List<MovieDto> movies = movieApiService.getMovieDiscoverList(page);




        return persistMovieDtoList(movies);
    }

    private void feedDBWithNewMoviesByQuery(String phrase) {
        List<MovieDto> movieBatch = new ArrayList<>();
        for (int i = 1; i <= PAGES_FROM_API; i++) {
            movieBatch.addAll(movieApiService.getMovieSearchList(i, phrase));
        }
        persistMovieDtoList(movieBatch);
    }

    private List<MovieDto> persistMovieDtoList(List<MovieDto> movies) {
        movies = movies.stream()
                .filter(movieDto -> !existsByApiID(movieDto.getApiID()))
                .toList().stream()
                .map(this::createMovie)
                .toList().stream()
                .map(movieUpdate -> updateMovieDataFromApi(movieUpdate.getId(),movieUpdate))
                .collect(Collectors.toList());
        return movies;
    }

    private MovieDto updateMovieDataFromApi(Long movieID, MovieDto movieDto) {
        MovieDto updateDto = movieApiService.getApiMovie(movieDto.getApiID());

        Movie movie = movieRepository.findById(movieID)
                .orElseThrow(() -> new RuntimeException("Movie id=" + movieID + " not found"));

        movie.setTitle(updateDto.getTitle());
        movie.setPosterPath(updateDto.getPosterPath());
        movie.setOverview(updateDto.getOverview());
        movie.setReleaseDate(updateDto.getReleaseDate());
        movie.setRuntime(updateDto.getRuntime());
        movie.setRating(updateDto.getRating());

        updateDto.setGenres(updateDto.getGenres().stream()
                .map(genreDto -> genreService.findByGenreName(genreDto.getName()))
                .collect(Collectors.toSet()));

        Map<String, List<FileVODPlatformDto>> regionPlatformMap = movieApiService.getRegionPlatformMapByApiID(movieDto.getApiID());

        updateDto.setVodPlatforms(regionPlatformMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .stream()
                .filter(platform -> vodPlatformService.existsByActivePlatformName(platform.getName()))
                .map(platform -> vodPlatformService.getActiveVODPlatformByName(platform.getName()))
                .collect(Collectors.toSet()));

        updateDto.setRegions(regionPlatformMap.keySet().stream()
                .filter(regionService::doesRegionExistsByCountryCode)
                .map(regionService::getRegionByCountryCode)
                .collect(Collectors.toSet()));
        Movie update = movieMapper.mapToMovie(updateDto);
        movie.setGenres(update.getGenres());
        movie.setVodPlatforms(update.getVodPlatforms());
        movie.setRegions(update.getRegions());
        movie.setLastUpdate(LocalDate.now());

        return movieMapper.mapToMovieDto(movieRepository.save(movie));
    }

    private boolean isMovieUpToDate(MovieDto movie) {
        return LocalDate.now().minusDays(7).isBefore(movie.getLastUpdate());
    }

    public CreditDto getCreditsByApiID(Long movieID) {
        return movieApiService.getApiCreditsForMovie(getMovieById(movieID).getApiID());
    }
}
