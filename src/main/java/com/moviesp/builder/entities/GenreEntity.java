package com.moviesp.builder.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "genres", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "id", "type" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @ManyToMany(mappedBy = "genres")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<MovieEntity> movies;

    @ManyToMany(mappedBy = "genres")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<TvShowEntity> tvShows;
}
