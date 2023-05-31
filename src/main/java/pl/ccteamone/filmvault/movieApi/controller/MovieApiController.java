package pl.ccteamone.filmvault.movieApi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.ccteamone.filmvault.movie.Movie;
import pl.ccteamone.filmvault.movie.repository.MovieRepository;
import pl.ccteamone.filmvault.movieApi.dto.MovieApiDto;
import pl.ccteamone.filmvault.movieApi.service.MovieApiService;

@RestController
@RequiredArgsConstructor
public class MovieApiController {

    private final MovieApiService movieApiService;
    private final MovieRepository movieRepository;

    @GetMapping("/api/movie")
    public Movie getApiMovie() {
        movieRepository.save(movieApiService.getApiMovie());
        return movieApiService.getApiMovie();
    }
}