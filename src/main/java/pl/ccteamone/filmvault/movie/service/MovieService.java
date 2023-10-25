package pl.ccteamone.filmvault.movie.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pl.ccteamone.filmvault.genre.service.GenreService;
import pl.ccteamone.filmvault.movie.Movie;
import pl.ccteamone.filmvault.movie.dto.CreditDto;
import pl.ccteamone.filmvault.movie.dto.MovieDto;
import pl.ccteamone.filmvault.movie.dto.MovieDtoPage;
import pl.ccteamone.filmvault.movie.mapper.MovieMapper;
import pl.ccteamone.filmvault.movie.repository.MovieRepository;
import pl.ccteamone.filmvault.region.service.RegionService;
import pl.ccteamone.filmvault.vodplatform.dto.FileVODPlatformDto;
import pl.ccteamone.filmvault.vodplatform.service.VODPlatformService;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private static final int PAGES_FROM_API = 1;
    private static final int DAYS_BETWEEN_UPDATES = 7;
    private static final int PAGE_SIZE = 20;


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

        if (isMovieUpToDate(movie)) {
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
        if (update.getVoteCount() != null) {
            movie.setVoteCount(update.getVoteCount());
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
    public List<MovieDto> findMovieByQuery(String query) {
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
                //TODO: zmienić tak, alby zwracał filmy dokładnie o tym samym tytule
                similarMovies.addAll(movies.stream()
                        .filter(match -> match.getTitle().equalsIgnoreCase(movie.getTitle()))
                        .map(movieMapper::mapToMovieDto)
                        .collect(Collectors.toList()));
            }
//            if (similarMovies.size() == PAGE_SIZE) {
//                break;
//            }
        }
        return similarMovies.stream()
                .collect(Collectors.toSet()).stream()
                .sorted(Comparator.comparing(movieDto -> {
                    String[] words = query.split(" ");
                    int count = 0;
                    for (String word : words) {
                        if(movieDto.getTitle().toLowerCase().contains(word.toLowerCase())){
                            count++;
                        }
                    }
                    return count;
                },Comparator.reverseOrder()))
                .toList().stream()
                .limit(40)
                .collect(Collectors.toList());
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
    //NGRAM search below

    public List<MovieDto> getNewestMovieList(Integer page) {
        List<MovieDto> movies = movieApiService.getMovieDiscoverList(page);
        movies = movies.stream()
                .filter(movieDto -> !existsByApiID(movieDto.getApiID()))
                .toList();
        persistMovieDtoList(movies);

        PageRequest pageRequest = PageRequest.of(page - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "releaseDate"));
        Page<Movie> moviePage = movieRepository.findAll(pageRequest);
        return moviePage.stream()
/*                .toList().stream()*/
                .map(movieMapper::mapToMovieDto)
                .toList();
    }

    private void feedDBWithNewMoviesByQuery(String phrase) {
        List<MovieDto> movieBatch = new ArrayList<>();
        int pagesToSearch = PAGES_FROM_API;
        int topPageSearch = 3;
        for (int i = 1; i <= pagesToSearch; i++) {
            List<MovieDto> pageContent = movieApiService.getMovieSearchList(i, phrase).stream()
                    .filter(movieDto -> !existsByApiID(movieDto.getApiID()))
                    .collect(Collectors.toList());
            movieBatch.addAll(pageContent);

            if (movieBatch.size() < PAGE_SIZE && pagesToSearch < topPageSearch) {
                pagesToSearch++;
            }
        }
        persistMovieDtoList(movieBatch);
    }

    private List<MovieDto> persistMovieDtoList(List<MovieDto> movies) {
        movies = movies.stream()
                .map(this::createMovie)
                .toList().stream()
                .map(movieUpdate -> updateMovieDataFromApi(movieUpdate.getId(), movieUpdate))
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
        if (movie == null || movie.getLastUpdate() == null) {
            return false;
        }
        return LocalDate.now().minusDays(DAYS_BETWEEN_UPDATES).isBefore(movie.getLastUpdate());
    }

    public CreditDto getCreditsByApiID(Long movieID) {
        return movieApiService.getApiCreditsForMovie(getMovieById(movieID).getApiID());
    }

    public MovieDto addRating(Long movieId, int rating) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found with id: " + movieId));

        try {
            if (1 <= rating && rating <= 10) {
                if (movie.getRating() == 0.0 || movie.getVoteCount() == 0) {
                    movie.setRating((double) rating);
                    movie.setVoteCount(1);
                    return movieMapper.mapToMovieDto(movieRepository.save(movie));
                }
                DecimalFormat df = new DecimalFormat("#.##");
                double v = movie.getRating() * movie.getVoteCount(); // sum of votes
                double newRatingCount = v + rating;
                int newVoteCount = movie.getVoteCount() + 1;
                double newRating = newRatingCount / newVoteCount;
                String formattedNumber = df.format(newRating);
                movie.setRating(df.parse(formattedNumber).doubleValue());
                movie.setVoteCount(newVoteCount);
            } else {
                throw new Exception("wrong rating given");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return movieMapper.mapToMovieDto(movieRepository.save(movie));
    }

    public List<MovieDto> getPopularMovieList(Integer page, String lang) {
        MovieDtoPage moviePage = movieApiService.getMoviePopularPage(lang, page);
        if (moviePage == null || moviePage.getMovies() == null) {
            throw new RuntimeException("Unable to retrieve popular movie batch");
        }

        persistMovieDtoList(moviePage.getMovies().stream()
                .filter(movieDto -> !existsByApiID(movieDto.getApiID()))
                .collect(Collectors.toList()));

        PageRequest popularPageRequest = PageRequest.of(page - 1, PAGE_SIZE, Sort.Direction.DESC, "popularity");
        Page<Movie> popularMoviesPage = movieRepository.findAll(popularPageRequest);
        return popularMoviesPage.stream()
                .map(movieMapper::mapToMovieDto)
                .collect(Collectors.toList());
    }
}
