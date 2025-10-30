package com.moviesp.builder.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "seasons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tvshow_id", nullable = false)
    private TvShowEntity tvShow;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "user_score", precision = 3, scale = 1)
    private BigDecimal userScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EpisodeEntity> episodes;
}

