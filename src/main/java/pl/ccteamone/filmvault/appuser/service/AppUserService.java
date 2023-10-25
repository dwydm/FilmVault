package pl.ccteamone.filmvault.appuser.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import pl.ccteamone.filmvault.appuser.AppUser;
import pl.ccteamone.filmvault.appuser.dto.AppUserDto;
import pl.ccteamone.filmvault.appuser.dto.AppUserProfileDto;
import pl.ccteamone.filmvault.appuser.mapper.AppUserMapper;
import pl.ccteamone.filmvault.appuser.repository.AppUserRepository;
import pl.ccteamone.filmvault.movie.Movie;
import pl.ccteamone.filmvault.movie.dto.MovieDto;
import pl.ccteamone.filmvault.movie.repository.MovieRepository;
import pl.ccteamone.filmvault.movie.service.MovieService;
import pl.ccteamone.filmvault.tvseries.dto.TvSeriesDto;
import pl.ccteamone.filmvault.tvseries.service.TvSeriesService;
import pl.ccteamone.filmvault.vodplatform.dto.VODPlatformDto;
import pl.ccteamone.filmvault.vodplatform.service.VODPlatformService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;
    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final TvSeriesService tvSeriesService;
    private final VODPlatformService platformService;

    public AppUserDto addMovieByTitle(String username, String movieTitle) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("AppUser not found, username: " + username));
        Movie movie = movieRepository.findByTitleContainingIgnoreCase(movieTitle)
                .stream().findAny()
                .orElseThrow(() -> new EntityNotFoundException("Movie not found, title: " + movieTitle));
        appUser.getMovies().add(movie);
        appUserRepository.save(appUser);
        return appUserMapper.mapToAppUserDto(appUser);
    }

    public AppUserDto addMovieByMovieIdToUser(String username, Long movieId) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("AppUser not found, username: " + username));
        Movie movie = movieRepository.findById(movieId)
                .stream().findAny()
                .orElseThrow(() -> new EntityNotFoundException("Movie not found, title: " + movieId));
        appUser.getMovies().add(movie);
        appUserRepository.save(appUser);
        return appUserMapper.mapToAppUserDto(appUser);
    }

    public AppUserDto addMovieByApiId(String username, Long movieApiId) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("AppUser not found, username: " + username));
        Movie movie = movieRepository.findByApiID(movieApiId)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found, title: " + movieApiId));
        appUser.getMovies().add(movie);
        appUserRepository.save(appUser);
        return appUserMapper.mapToAppUserDto(appUser);
    }

    public List<AppUserProfileDto> getUsersList() {
        return appUserRepository.findAll()
                .stream().map(appUserMapper::mapToAppUserProfileDto)
                .toList();
    }

    public AppUserProfileDto getUserById(Long userId) {
        return appUserRepository.findById(userId)
                .stream().map(appUserMapper::mapToAppUserProfileDto)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("AppUser not found, id: " + userId));
    }

    private AppUser getUserByUsername(String username) {
        return appUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Unable to find " + username + " in database"));
    }
    private AppUser getUserByUsernameProfileDTO(String username) {
        return appUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Unable to find " + username + " in database"));
    }

    public AppUserDto updateUser(Long userId, AppUserDto appUserDto) {
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("AppUser not found, name: " + userId));

        AppUser appUser = appUserMapper.mapToAppUser(appUserDto);
        if (appUser.getEmail() != null) {
            user.setEmail(appUser.getEmail());
        }
        if (appUser.getName() != null) {
            user.setName(appUser.getName());
        }
        if (appUser.getPassword() != null) {
            user.setPassword(appUser.getPassword());
        }
        if (appUser.getSurname() != null) {
            user.setSurname(appUser.getSurname());
        }
        if (appUser.getBirthDate() != null) {
            user.setBirthDate(appUser.getBirthDate());
        }
        if (appUser.getGender() != null) {
            user.setGender(appUser.getGender());
        }
        if (appUser.getProfilePic() != null) {
            user.setProfilePic(appUser.getProfilePic());
        }
        if (appUser.getRegion() != null) {
            user.setRegion(appUser.getRegion());
        }
        if (appUser.getMovies() != null) {
            user.setMovies(appUser.getMovies());
        }
        if (appUser.getVodPlatforms() != null) {
            user.setVodPlatforms(appUser.getVodPlatforms());
        }
        if (appUser.getTvSeries() != null) {
            user.setTvSeries(appUser.getTvSeries());
        }
        return appUserMapper.mapToAppUserDto(appUserRepository.save(user));
    }

    public void deleteUserById(Long userId) {
        try {
            appUserRepository.deleteById(userId);
        } catch (EmptyResultDataAccessException e) {
            throw new EntityNotFoundException("AppUser not found, id: " + userId);
        }
    }

    public AppUserDto addMovieByID(String username, Long movieID) {
        AppUserDto user = appUserMapper.mapToAppUserDto(getUserByUsername(username));
        MovieDto movie = movieService.getMovieById(movieID);
        Set<MovieDto> userMovies = user.getMovies();
        if (userMovies != null) {
            userMovies.add(movie);
        } else {
            userMovies = new HashSet<>();
            userMovies.add(movie);
            user.setMovies(userMovies);
        }
        return updateUser(user.getId(), user);
    }

    public AppUserDto addTvSeriesByID(String username, Long tvseriesID) {
        AppUserDto user = appUserMapper.mapToAppUserDto(getUserByUsername(username));
        TvSeriesDto tvSeries = tvSeriesService.getTvSeriesById(tvseriesID);
        Set<TvSeriesDto> userSeries = user.getTvSeries();
        if(userSeries != null) {
            userSeries.add(tvSeries);
        } else {
            userSeries = new HashSet<>();
            userSeries.add(tvSeries);
            user.setTvSeries(userSeries);
        }
        return updateUser(user.getId(), user);
    }

    public AppUserDto getUserDtoByUsername(String username) {
        return appUserMapper.mapToAppUserDto(getUserByUsername(username));
    }
    public AppUserProfileDto getUserDtoByUsernameProfileDTO(String username) {
        return appUserMapper.mapToAppUserProfileDto(getUserByUsernameProfileDTO(username));
    }
    public AppUserDto updateUserByUsername(String username, AppUserDto appUserDto) {
        if (!username.equals(appUserDto.getUsername())) {
            throw new RuntimeException("Username " + username + " database record mismatch");
        }
        return updateUser(appUserDto.getId(), appUserDto);
    }

    public AppUserDto addPlatformById(String username, Long id) {
        VODPlatformDto vodPlatformDto = platformService.getVODPlatformDtoById(id);
        AppUserDto userDto = getUserDtoByUsername(username);
        if(vodPlatformDto == null) {
            throw new RuntimeException("Unable to find VOD Platform");
        }

        userDto.getVodPlatforms().add(vodPlatformDto);
        return updateUser(userDto.getId(),userDto);
    }

    public AppUserDto removePlatformById(String username, Long id) {
        VODPlatformDto vodPlatformDto = platformService.getVODPlatformDtoById(id);
        AppUserDto userDto = getUserDtoByUsername(username);
        if(vodPlatformDto == null) {
            throw new RuntimeException("Unable to find VOD Platform");
        }

        Set<VODPlatformDto> platforms = userDto.getVodPlatforms().stream()
                .filter(platformDto -> !Objects.equals(platformDto.getId(), vodPlatformDto.getId()))
                .collect(Collectors.toSet());
        userDto.setVodPlatforms(platforms);
        return updateUser(userDto.getId(), userDto);
    }

}
