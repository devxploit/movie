-- Drop existing movies table from V1 (replaced with new schema)
DROP TABLE IF EXISTS movies CASCADE;

-- Table: folders
CREATE TABLE IF NOT EXISTS folders (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(500) NOT NULL,
    "user" VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(path, "user")
);

CREATE INDEX idx_folders_path ON folders(path);
CREATE INDEX idx_folders_user ON folders("user");
CREATE INDEX idx_folders_status ON folders(status);

-- Table: genres
CREATE TABLE IF NOT EXISTS genres (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    UNIQUE(id, type)
);

CREATE INDEX idx_genres_type ON genres(type);

-- Table: movies
CREATE TABLE IF NOT EXISTS movies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    date DATE,
    tmdb_id BIGINT UNIQUE,
    poster_path VARCHAR(500),
    user_score NUMERIC(3,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movies_name ON movies(name);
CREATE INDEX idx_movies_date ON movies(date);
CREATE INDEX idx_movies_tmdb_id ON movies(tmdb_id);

-- Table: movie_genres (many-to-many relationship)
CREATE TABLE IF NOT EXISTS movie_genres (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

CREATE INDEX idx_movie_genres_movie ON movie_genres(movie_id);
CREATE INDEX idx_movie_genres_genre ON movie_genres(genre_id);

-- Table: movie_aliases
CREATE TABLE IF NOT EXISTS movie_aliases (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    alias VARCHAR(500) NOT NULL,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE INDEX idx_movie_aliases_movie ON movie_aliases(movie_id);

-- Table: movie_videos
CREATE TABLE IF NOT EXISTS movie_videos (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    resolution VARCHAR(50),
    url VARCHAR(1000) NOT NULL,
    size VARCHAR(100),
    size_in_bytes BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
);

CREATE INDEX idx_movie_videos_movie ON movie_videos(movie_id);

-- Table: tvshows
CREATE TABLE IF NOT EXISTS tvshows (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    date DATE,
    tmdb_id BIGINT UNIQUE,
    poster_path VARCHAR(500),
    user_score NUMERIC(3,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tvshows_name ON tvshows(name);
CREATE INDEX idx_tvshows_date ON tvshows(date);
CREATE INDEX idx_tvshows_tmdb_id ON tvshows(tmdb_id);

-- Table: tvshow_genres (many-to-many relationship)
CREATE TABLE IF NOT EXISTS tvshow_genres (
    tvshow_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (tvshow_id, genre_id),
    FOREIGN KEY (tvshow_id) REFERENCES tvshows(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

CREATE INDEX idx_tvshow_genres_tvshow ON tvshow_genres(tvshow_id);
CREATE INDEX idx_tvshow_genres_genre ON tvshow_genres(genre_id);

-- Table: tvshow_aliases
CREATE TABLE IF NOT EXISTS tvshow_aliases (
    id BIGSERIAL PRIMARY KEY,
    tvshow_id BIGINT NOT NULL,
    alias VARCHAR(500) NOT NULL,
    FOREIGN KEY (tvshow_id) REFERENCES tvshows(id) ON DELETE CASCADE
);

CREATE INDEX idx_tvshow_aliases_tvshow ON tvshow_aliases(tvshow_id);

-- Table: seasons
CREATE TABLE IF NOT EXISTS seasons (
    id BIGSERIAL PRIMARY KEY,
    tvshow_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    season_number INTEGER NOT NULL,
    date DATE,
    user_score NUMERIC(3,1),
    created_at TIMESTAMP,
    FOREIGN KEY (tvshow_id) REFERENCES tvshows(id) ON DELETE CASCADE
);

CREATE INDEX idx_seasons_tvshow ON seasons(tvshow_id);
CREATE INDEX idx_seasons_number ON seasons(season_number);

-- Table: episodes
CREATE TABLE IF NOT EXISTS episodes (
    id BIGSERIAL PRIMARY KEY,
    season_id BIGINT NOT NULL,
    episode_number INTEGER NOT NULL,
    name VARCHAR(500) NOT NULL,
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE CASCADE
);

CREATE INDEX idx_episodes_season ON episodes(season_id);
CREATE INDEX idx_episodes_number ON episodes(episode_number);

-- Table: episode_videos
CREATE TABLE IF NOT EXISTS episode_videos (
    id BIGSERIAL PRIMARY KEY,
    episode_id BIGINT NOT NULL,
    resolution VARCHAR(50),
    url VARCHAR(1000) NOT NULL,
    size VARCHAR(100),
    size_in_bytes BIGINT,
    created_at TIMESTAMP,
    FOREIGN KEY (episode_id) REFERENCES episodes(id) ON DELETE CASCADE
);

CREATE INDEX idx_episode_videos_episode ON episode_videos(episode_id);

