package pl.ccteamone.filmvault.media;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@DiscriminatorColumn(name = "media_type")
public abstract class Media {
    @Id
    private Long id;
}
