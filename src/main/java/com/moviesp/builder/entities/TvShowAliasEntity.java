package com.moviesp.builder.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tvshow_aliases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TvShowAliasEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tvshow_id", nullable = false)
    private TvShowEntity tvShow;

    @Column(nullable = false, length = 500)
    private String alias;
}

