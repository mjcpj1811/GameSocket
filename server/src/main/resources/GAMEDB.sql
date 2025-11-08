CREATE DATABASE IF NOT EXISTS MemoryGame;
USE MemoryGame;

-- ==========================
-- 1. Bảng User
-- ==========================
CREATE TABLE Users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE ,
    status VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================
-- 2. Bảng Match
-- ==========================
CREATE TABLE Matches (
    id VARCHAR(255) PRIMARY KEY,
    winner VARCHAR(255),
    FOREIGN KEY (winner) REFERENCES Users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- ==========================
-- 3. Bảng UserMatch (Bảng trung gian giữa User và Match)
-- ==========================
CREATE TABLE MatchDetails (
    UserId VARCHAR(255),
    MatchId VARCHAR(255),
    total_score INT NOT NULL ,
    play_time DOUBLE NOT NULL,
    PRIMARY KEY (UserId, MatchId),
    FOREIGN KEY (UserId) REFERENCES Users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (MatchId) REFERENCES Matches(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ==========================
-- 4. Bảng Round
-- ==========================
CREATE TABLE Rounds (
    id VARCHAR(255) PRIMARY KEY,
    number_round INT NOT NULL,
    sequence_shown VARCHAR(255) NOT NULL,
    player1_id VARCHAR(255) NOT NULL,
    player2_id VARCHAR(255) NOT NULL,
    answer_player1 VARCHAR(255) NOT NULL,
    answer_player2 VARCHAR(255) NOT NULL,
    score_player1 INT NOT NULL,
    score_player2 INT NOT NULL,
    time_spent_player1 DOUBLE NOT NULL,
    time_spent_player2 DOUBLE NOT NULL,
    MatchId VARCHAR(255),
    FOREIGN KEY (MatchId) REFERENCES Matches(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
