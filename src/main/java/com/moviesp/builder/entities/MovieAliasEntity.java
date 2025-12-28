package com.moviesp.builder.entities;

import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "movie_aliases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieAliasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private MovieEntity movie;

    @Column(nullable = false, length = 500)
    private String alias;
}
