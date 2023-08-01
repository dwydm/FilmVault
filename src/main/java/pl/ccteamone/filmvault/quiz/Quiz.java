package pl.ccteamone.filmvault.quiz;

import jakarta.persistence.*;
import lombok.*;
import pl.ccteamone.filmvault.media.Media;
import pl.ccteamone.filmvault.appuser.AppUser;
import pl.ccteamone.filmvault.quiz.question.Question;

import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Quiz {
    @Id
    private Long id;
    @ManyToOne
    private AppUser user;
    @ManyToOne
    private Media media;
    @OneToMany
    private Set<Question> questions;
}
