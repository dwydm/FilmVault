package pl.ccteamone.filmvault.tvseries.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.ccteamone.filmvault.tvseries.dto.ApiTvSeriesDto;
import pl.ccteamone.filmvault.tvseries.dto.ApiTvSeriesDtoPage;
import pl.ccteamone.filmvault.tvseries.dto.TvSeriesDtoPage;
import pl.ccteamone.filmvault.vodplatform.dto.FileVODPlatformDto;

import java.util.*;

@Component
@Slf4j
public class ApiTvSeriesClient {

    private static final String API_TVSERIES_URL = "https://api.themoviedb.org/3/tv/";
    private static final String API_DISCOVER_URL = "https://api.themoviedb.org/3/discover/tv";
    private static final String API_SEARCH_URL = "https://api.themoviedb.org/3/search/tv";
    @Value("${media.api.key}")
    private String API_KEY;


    private final RestTemplate restTemplate = new RestTemplate();



    public ApiTvSeriesDto getApiTvSeriesByTvSeriesId(Long id) {
        return callGetMethod(API_TVSERIES_URL, "{tvseries_id}?api_key={apiKey}", ApiTvSeriesDto.class, id, API_KEY);
    }
    public ApiTvSeriesDtoPage getTvSeriesDiscoverPage(Integer page) {
        return callGetMethod(API_DISCOVER_URL,"?api_key={apiKey}&popular?language=en-US&page={page}", ApiTvSeriesDtoPage.class, API_KEY,page);
    }

    public ApiTvSeriesDtoPage getTvSeriesTitleSearchPage(Integer page, String phrase) {
        return callGetMethod(API_SEARCH_URL, "?api_key={apiKey}&page={page}&query={phrase}", ApiTvSeriesDtoPage.class, API_KEY, page, phrase);
    }

    public Map<String, List<FileVODPlatformDto>> getRegionsOfPlatformsByTvSeriesApiID(Long id) {
        String response = callGetMethod(API_TVSERIES_URL, "{tvseries_id}/watch/providers?api_key={apiKey}", String.class, id, API_KEY);
        Map<String, List<FileVODPlatformDto>> resultMap = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> responseMap = mapper.readValue(response, new TypeReference<Map<String, Object>>() {
            });
            Map<String, Object> resultsMap = (Map<String, Object>) responseMap.get("results");
            for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
                String regionCode = entry.getKey();
                Map<String,Object> regionMap = (Map<String, Object>) entry.getValue();
                List<Map<String,Object>> platformsList = (List<Map<String, Object>>) regionMap.get("flatrate");
                List<FileVODPlatformDto> fileVODPlatformDtoList = new ArrayList<>();
                if(platformsList != null) {
                    for (Map<String, Object> platform : platformsList) {
                        FileVODPlatformDto fileVODPlatformDto = new FileVODPlatformDto();
                        fileVODPlatformDto.setName((String) platform.get("provider_name"));
                        fileVODPlatformDto.setLogoPath((String) platform.get("logo_path"));

                        fileVODPlatformDtoList.add(fileVODPlatformDto);
                    }
                }
                resultMap.put(regionCode,fileVODPlatformDtoList);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse API response");
        }
        return resultMap;
    }


    private <T> T callGetMethod(String TYPE_URL, String url, Class<T> responseType, Object... objects) {
        return restTemplate.getForObject(TYPE_URL + url, responseType, objects);
    }

    public ApiTvSeriesDtoPage getTvSeriesPopularPage(String lang, Integer page) {
        return callGetMethod(API_DISCOVER_URL,"?api_key={apiKey}&language={lang}&page={page}&popular&sort_by=popularity.desc",ApiTvSeriesDtoPage.class,API_KEY,lang,page);
    }
}
