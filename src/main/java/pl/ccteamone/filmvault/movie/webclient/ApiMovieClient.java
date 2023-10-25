package pl.ccteamone.filmvault.movie.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.ccteamone.filmvault.movie.dto.CreditDto;
import pl.ccteamone.filmvault.movie.dto.ApiMovieDtoPage;
import pl.ccteamone.filmvault.movie.dto.ApiMovieDto;
import pl.ccteamone.filmvault.vodplatform.dto.FileVODPlatformDto;

import java.util.*;

@Component
@Slf4j
public class ApiMovieClient {

    private static final String API_MOVIE_URL = "https://api.themoviedb.org/3/movie/";
    private static final String API_DISCOVER_URL = "https://api.themoviedb.org/3/discover/movie";
    private static final String API_SEARCH_URL = "https://api.themoviedb.org/3/search/movie";
    @Value("${media.api.key}")
    private String API_KEY;

    private RestTemplate restTemplate = new RestTemplate();


    public ApiMovieDto getApiMovieByMovieId(Long id) {
        return callGetMethod(API_MOVIE_URL, "{movie_id}?api_key={apiKey}", ApiMovieDto.class, id, API_KEY);
    }

    public CreditDto getApiCreditsByMovieId(Long id) {
        return callGetMethod(API_MOVIE_URL, "{movie_id}/credits?api_key={apiKey}", CreditDto.class, id, API_KEY);
    }

    public ApiMovieDtoPage getMoviesDiscoverPage(Integer page) {
        return callGetMethod(API_DISCOVER_URL, "?api_key={apiKey}&popular?language=en-US&page={page}", ApiMovieDtoPage.class, API_KEY, page);
    }

    public ApiMovieDtoPage getMoviesTitleSearchPage(Integer page, String phrase) {
        return callGetMethod(API_SEARCH_URL, "?api_key={apiKey}&page={page}&query={phrase}", ApiMovieDtoPage.class, API_KEY, page, phrase);
    }

    public ApiMovieDtoPage getPopularMoviesPage(String lang, Integer page) {
        return callGetMethod(API_DISCOVER_URL, "?api_key={apiKey}&language={lang}&page={page}&sort_by=popularity.desc", ApiMovieDtoPage.class, API_KEY, lang, page);
    }

    public Map<String, List<FileVODPlatformDto>> getRegionsOfPlatformsByMovieApiID(Long id) {
        String response = callGetMethod(API_MOVIE_URL, "{movie_id}/watch/providers?api_key={apiKey}", String.class, id, API_KEY);
        Map<String, List<FileVODPlatformDto>> resultMap = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> responseMap = mapper.readValue(response, new TypeReference<Map<String, Object>>() {
            });
            Map<String, Object> resultsMap = (Map<String, Object>) responseMap.get("results");
            for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
                String regionCode = entry.getKey();
                Map<String, Object> regionMap = (Map<String, Object>) entry.getValue();
                List<Map<String, Object>> platformsList = (List<Map<String, Object>>) regionMap.get("flatrate");
                List<FileVODPlatformDto> fileVODPlatformDtoList = new ArrayList<>();
                if (platformsList != null) {
                    for (Map<String, Object> platform : platformsList) {
                        FileVODPlatformDto fileVODPlatformDto = new FileVODPlatformDto();
                        fileVODPlatformDto.setName((String) platform.get("provider_name"));
                        fileVODPlatformDto.setLogoPath((String) platform.get("logo_path"));

                        fileVODPlatformDtoList.add(fileVODPlatformDto);
                    }
                }
                resultMap.put(regionCode, fileVODPlatformDtoList);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse API response");
        }
        return resultMap;
    }

    private <T> T callGetMethod(String TYPE_URL, String url, Class<T> responseType, Object... objects) {
        return restTemplate.getForObject(TYPE_URL + url, responseType, objects);
    }
}
