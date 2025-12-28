package com.moviesp.builder.entities;

import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieVideoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private MovieEntity movie;

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
