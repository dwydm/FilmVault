package pl.ccteamone.filmvault.media;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Media {
    @Id
    private Long id;
}
