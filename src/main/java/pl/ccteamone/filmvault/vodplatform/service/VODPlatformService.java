package pl.ccteamone.filmvault.vodplatform.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.ccteamone.filmvault.vodplatform.VODPlatform;
import pl.ccteamone.filmvault.vodplatform.dto.VODPlatformDto;
import pl.ccteamone.filmvault.vodplatform.mapper.VODPlatformMapper;
import pl.ccteamone.filmvault.vodplatform.repository.VODPlatformRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class VODPlatformService {
    private final VODPlatformRepository platformRepository;
    private final VODPlatformMapper vodPlatformMapper;

    public VODPlatform save(VODPlatform platform) {
        return platformRepository.save(platform);
    }

    public VODPlatformDto createVODPlatform(VODPlatformDto platformDto) {
        VODPlatform platform = VODPlatform.builder()
                .name(platformDto.getName())
                .logoPath(platformDto.getLogoPath())
                .vodURL(platformDto.getVodURL())
                .active(platformDto.isActive())
                .build();
        return vodPlatformMapper.mapToVODPlatformDto(platformRepository.save(platform));
    }

    public List<VODPlatformDto> getVODPlatformDtoFullList() {
        return StreamSupport.stream(platformRepository.findAll().spliterator(), false)
                .map(vodPlatformMapper::mapToVODPlatformDto).collect(Collectors.toList());
    }

    public List<VODPlatformDto> getVODPlatformActiveList() {
        return vodPlatformMapper.mapToVODPlatformDtoSet(platformRepository.findAll().stream()
                .filter(vodPlatform -> vodPlatform.isActive()).collect(Collectors.toSet())).stream().toList();
    }

    public VODPlatformDto getVODPlatformDtoById(Long id) {
        Optional<VODPlatform> platform = platformRepository.findById(id);
        return vodPlatformMapper.mapToVODPlatformDto(platform.orElseThrow(() -> new RuntimeException("VOD Platform id=" + id + " not found")));
    }

    public VODPlatformDto updateVODPlatform(Long id, VODPlatformDto platformDto) {
        VODPlatform platform = platformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VOD Platform apiID=" + id + " not found"));

        VODPlatform vodPlatform = vodPlatformMapper.mapToVODPlatform(platformDto);

        if (vodPlatform.getName() != null) {
            platform.setName(vodPlatform.getName());
        }
        if (vodPlatform.getLogoPath() != null) {
            platform.setLogoPath(vodPlatform.getLogoPath());
        }
        if (vodPlatform.getVodURL() != null) {
            platform.setVodURL(vodPlatform.getVodURL());
        }
        if (!vodPlatform.isActive()) {
            platform.setActive(vodPlatform.isActive());
        }
        return vodPlatformMapper.mapToVODPlatformDto(platformRepository.save(platform));
    }

    public void deleteVODPlatformById(Long vodPlatformId) {
        try {
            platformRepository.deleteById(vodPlatformId);
        } catch (Exception e) {
            throw new EntityNotFoundException("VODPlatform id =" + vodPlatformId + " not found");
        }
    }

    public boolean existsByPlatformName(String name) {
        return platformRepository.existsByNameIgnoreCase(name);
    }
    public boolean existsByActivePlatformName(String name) {
        return platformRepository.existsByNameIgnoreCaseAndActiveIsTrue(name);
    }

    public Set<VODPlatformDto> getActiveVODPlatform() {
        return vodPlatformMapper.mapToVODPlatformDtoSet(platformRepository.findAllByActiveIsTrue());
    }

    public VODPlatformDto getActiveVODPlatformByName(String name) {
        return vodPlatformMapper.mapToVODPlatformDto(platformRepository.findByNameAndActiveIsTrue(name)
                .orElseThrow(() -> new RuntimeException("Platform not found or inactive")));
    }

}
