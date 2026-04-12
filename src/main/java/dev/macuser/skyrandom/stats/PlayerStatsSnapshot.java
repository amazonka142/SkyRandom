package dev.macuser.skyrandom.stats;

public record PlayerStatsSnapshot(int matches, int wins, int kills, int deaths, long totalRoundSeconds) {

    public double winRate() {
        if (matches <= 0) {
            return 0.0D;
        }
        return (wins * 100.0D) / matches;
    }

    public double averageRoundDurationSeconds() {
        if (matches <= 0 || totalRoundSeconds <= 0L) {
            return 0.0D;
        }
        return totalRoundSeconds / (double) matches;
    }
}
