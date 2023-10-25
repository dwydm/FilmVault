package pl.ccteamone.filmvault.tvseries.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pl.ccteamone.filmvault.genre.service.GenreService;
import pl.ccteamone.filmvault.region.service.RegionService;
import pl.ccteamone.filmvault.tvseries.TvSeries;
import pl.ccteamone.filmvault.tvseries.dto.TvSeriesDto;
import pl.ccteamone.filmvault.tvseries.dto.TvSeriesDtoPage;
import pl.ccteamone.filmvault.tvseries.mapper.TvSeriesMapper;
import pl.ccteamone.filmvault.tvseries.repository.TvSeriesRepository;
import pl.ccteamone.filmvault.vodplatform.dto.FileVODPlatformDto;
import pl.ccteamone.filmvault.vodplatform.service.VODPlatformService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class TvSeriesService {
    private final TvSeriesRepository tvRepository;
    private final TvSeriesApiService tvSeriesApiService;
    private final TvSeriesMapper tvSeriesMapper;

    private final GenreService genreService;
    private final VODPlatformService vodPlatformService;
    private final RegionService regionService;

    private static final int PAGES_FROM_API = 1;
    private static final int DAYS_BETWEEN_UPDATES = 7;
    private static final int PAGE_SIZE = 20;


    public TvSeriesDto createTvSeries(TvSeriesDto create) {
        TvSeries tvSeriesFromDto = tvSeriesMapper.mapToTvSeries(create);
        return tvSeriesMapper.mapToTvSeriesDto(tvRepository.save(tvSeriesFromDto));
    }

    public List<TvSeriesDto> getTvSeriesList() {
        return StreamSupport.stream(tvRepository.findAll().spliterator(), false)
                .map(tvSeriesMapper::mapToTvSeriesDto).collect(Collectors.toList());
    }

    public TvSeriesDto getTvSeriesById(Long tvSeriesId) {
        TvSeriesDto tvSeries = tvSeriesMapper.mapToTvSeriesDto(tvRepository.findById(tvSeriesId)
                .orElseThrow(() -> new RuntimeException("Tv Series id=" + tvSeriesId + " not found")));

        if (isTvSeriesUpToDate(tvSeries)) {
            tvSeries = updateTvSeriesDataFromApi(tvSeriesId, tvSeries);
        }
        return tvSeries;
    }

    public TvSeriesDto updateTvSeries(Long tvseriesId, TvSeriesDto tvSeriesDto) {
        TvSeries tvSeries = tvRepository.findById(tvseriesId)
                .orElseThrow(() -> new EntityNotFoundException("Tv Series id=" + tvseriesId + " not found"));

        TvSeries series = tvSeriesMapper.mapToTvSeries(tvSeriesDto);

        if (series.getName() != null) {
            tvSeries.setName(series.getName());
        }
        if (series.getOverview() != null) {
            tvSeries.setOverview(series.getOverview());
        }
        if (series.getGenres() != null) {
            tvSeries.setGenres(series.getGenres());
        }
        if (series.getPosterPath() != null) {
            tvSeries.setPosterPath(series.getPosterPath());
        }
        if (!series.isAdult()) {
            tvSeries.setAdult(series.isAdult());
        }
        if (series.getOriginCountry() != null) {
            tvSeries.setOriginCountry(series.getOriginCountry());
        }
        if (series.getFirstAirDate() != null) {
            tvSeries.setFirstAirDate(series.getFirstAirDate());
        }
        if (series.getLastAirDate() != null) {
            tvSeries.setLastAirDate(series.getLastAirDate());
        }
        if (series.getSeasons() != 0) {
            tvSeries.setSeasons(series.getSeasons());
        }
        if (series.getEpisodes() != 0) {
            tvSeries.setEpisodes(series.getEpisodes());
        }
        if (series.getRegions() != null) {
            tvSeries.setRegions(series.getRegions());
        }
        if (series.getVodPlatforms() != null) {
            tvSeries.setVodPlatforms(series.getVodPlatforms());
        }
        if (series.getApiID() != null) {
            tvSeries.setApiID(series.getApiID());
        }
        return tvSeriesMapper.mapToTvSeriesDto(tvRepository.save(tvSeries));
    }

    public void deleteTvSeries(Long tvseriesId) {
        try {
            tvRepository.deleteById(tvseriesId);
        } catch (Exception e) {
            throw new EntityNotFoundException("Tv Series id=" + tvseriesId + " not found");
        }
    }

    public boolean existsByApiID(Long id) {
        return tvRepository.existsByApiID(id);
    }

    public TvSeriesDto findTvSeriesByApiID(Long id) {
        return tvSeriesMapper.mapToTvSeriesDto(tvRepository.findByApiID(id)
                .orElseThrow(() -> new RuntimeException("Tv Series not found")));
    }

    public List<TvSeriesDto> findTvSeriesByQuery(String query) {
        feedDBWithNewTvSeriesByQuery(query);
        List<TvSeries> tvSeries = tvRepository.findByNameContainingIgnoreCase(query.substring(0, 1));
        Set<TvSeriesDto> similarTvSeries = new HashSet<>();

        for (TvSeries tvSerie : tvSeries) {
            String name = tvSerie.getName().toLowerCase();
            String lowercaseQuery = query.toLowerCase();

            List<String> nameNGrams = generateNGrams(name, 2); // licznik prawdopodobieństwa dla title
            List<String> queryNGrams = generateNGrams(lowercaseQuery, 2);  // licznik prawdopodobieństwa dla query

            int commonNGrams = countCommonNGrams(nameNGrams, queryNGrams);

            if (commonNGrams >= 2) { // <-- Licznik prawdopodobieństwa
                similarTvSeries.addAll(tvSeries.stream()
                        .filter(match -> match.getName().equalsIgnoreCase(tvSerie.getName()))
                        .map(tvSeriesMapper::mapToTvSeriesDto)
                        .collect(Collectors.toList()));
            }

        }
        return similarTvSeries.stream()
                .collect(Collectors.toSet()).stream()
                .sorted(Comparator.comparing(tvSeriesDto -> {
                    String[] words = query.split(" ");
                    int count = 0;
                    for (String word : words) {
                        if(tvSeriesDto.getName().toLowerCase().contains(word.toLowerCase())){
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

    public List<TvSeriesDto> getNewestTvSeriesList(Integer page) {
        List<TvSeriesDto> tvSeries = tvSeriesApiService.getTvSeriesDiscoverList(page);
        tvSeries = tvSeries.stream()
                .filter(tvSeriesDto -> !existsByApiID(tvSeriesDto.getApiID()))
                .toList();
        persistTvSeriesDtoList(tvSeries);

        PageRequest pageRequest = PageRequest.of(page - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "firstAirDate"));
        Page<TvSeries> tvPage = tvRepository.findAll(pageRequest);
        return tvPage.stream()
                .toList().stream()
                .map(tvSeriesMapper::mapToTvSeriesDto)
                .toList();
    }

    private void feedDBWithNewTvSeriesByQuery(String phrase) {
        List<TvSeriesDto> tvSeriesBatch = new ArrayList<>();
        int pagesToSearch = PAGES_FROM_API;
        int topPageSearch = 3;
        for (int i = 1; i <= pagesToSearch; i++) {
            List<TvSeriesDto> pageContent = tvSeriesApiService.getTvSeriesSearchList(i, phrase).stream()
                    .filter(tvSeriesDto -> !existsByApiID(tvSeriesDto.getApiID()))
                    .collect(Collectors.toList());
            tvSeriesBatch.addAll(pageContent);

            if (tvSeriesBatch.size() < PAGE_SIZE && pagesToSearch < topPageSearch) {
                pagesToSearch++;
            }
        }
        persistTvSeriesDtoList(tvSeriesBatch);
    }

    private List<TvSeriesDto> persistTvSeriesDtoList(List<TvSeriesDto> tvSeries) {
        tvSeries = tvSeries.stream()
                /*             .filter(tvSeriesDto -> !existsByApiID(tvSeriesDto.getApiID()))
                             .toList().stream()*/
                .map(this::createTvSeries)
                .toList().stream()
                .map(tvUpdate -> updateTvSeriesDataFromApi(tvUpdate.getId(), tvUpdate))
                .collect(Collectors.toList());
        return tvSeries;
    }

    private TvSeriesDto updateTvSeriesDataFromApi(Long tvSeriesID, TvSeriesDto tvSeriesDto) {
        TvSeriesDto updateDto = tvSeriesApiService.getApiTvSeries(tvSeriesDto.getApiID());

        TvSeries tvSeries = tvRepository.findById(tvSeriesID)
                .orElseThrow(() -> new RuntimeException("Tv Series id=" + tvSeriesID + " not found"));

        tvSeries.setName(updateDto.getName());
        tvSeries.setPosterPath(updateDto.getPosterPath());
        tvSeries.setOverview(updateDto.getOverview());
        tvSeries.setAdult(updateDto.isAdult());
        tvSeries.setFirstAirDate(updateDto.getFirstAirDate());
        tvSeries.setLastAirDate(updateDto.getLastAirDate());
        tvSeries.setOriginLanguage(updateDto.getOriginLanguage());
        tvSeries.setSeasons(updateDto.getSeasons());
        tvSeries.setEpisodes(updateDto.getEpisodes());

        updateDto.setGenres(updateDto.getGenres().stream()
                .map(genreDto -> genreService.findByGenreName(genreDto.getName()))
                .collect(Collectors.toSet()));

        Map<String, List<FileVODPlatformDto>> regionPlatformMap = tvSeriesApiService.getRegionPlatformMapByApiID(tvSeriesDto.getApiID());

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
        TvSeries update = tvSeriesMapper.mapToTvSeries(updateDto);
        tvSeries.setGenres(update.getGenres());
        tvSeries.setVodPlatforms(update.getVodPlatforms());
        tvSeries.setRegions(update.getRegions());
        tvSeries.setLastUpdate(LocalDate.now());

        return tvSeriesMapper.mapToTvSeriesDto(tvRepository.save(tvSeries));
    }

    private boolean isTvSeriesUpToDate(TvSeriesDto series) {
        if (series == null || series.getLastUpdate() == null) {
            return false;
        }
        return LocalDate.now().minusDays(DAYS_BETWEEN_UPDATES).isBefore(series.getLastUpdate());
    }

    public List<TvSeriesDto> getPopularTvSeriesList(String lang, Integer page) {
        TvSeriesDtoPage tvSeriesPage = tvSeriesApiService.getTvSeriesPopularPage(lang, page);
        if (tvSeriesPage == null || tvSeriesPage.getTvSeries() == null) {
            throw new RuntimeException("Unable to retrieve popular TV Series batch");
        }

        persistTvSeriesDtoList(tvSeriesPage.getTvSeries().stream()
                .filter(tvSeriesDto -> !existsByApiID(tvSeriesDto.getApiID()))
                .collect(Collectors.toList()));

        PageRequest popularPageRequest = PageRequest.of(page - 1, PAGE_SIZE, Sort.Direction.DESC, "popularity");
        Page<TvSeries> popularTvSeriesPage = tvRepository.findAll(popularPageRequest);
        return popularTvSeriesPage.stream()
                .map(tvSeriesMapper::mapToTvSeriesDto)
                .collect(Collectors.toList());
    }
}
