package com.moviesp.builder.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "episode_videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeVideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private EpisodeEntity episode;

    @Column(length = 50)
    private String resolution;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 100)
    private String size;

    @Column(name = "size_in_bytes")
    private Long sizeInBytes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

