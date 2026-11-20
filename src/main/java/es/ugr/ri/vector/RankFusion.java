
package es.ugr.ri.vector;

import java.util.*;

/** Fusion de rankings de películas (no de fragmentos) */
public final class RankFusion {

    private RankFusion() {}

    /**
     * RRF sin ponderar
     */
    public static List<RankedMovie> rrf(
            List<RankedMovie> a, List<RankedMovie> b, int c, int k) {

        return rrfWeighted(a, b, c, k, 0.5, 0.5);
    }

    /**
     * RRF ponderado
     * Los pesos deben ser no negativos y sumar 1
     */
    public static List<RankedMovie> rrfWeighted(
            List<RankedMovie> a, List<RankedMovie> b,
            int c, int k, double alphaA, double alphaB) {

        if (c < 1) throw new IllegalArgumentException("c debe ser positivo");
        if (k < 0) throw new IllegalArgumentException("k no puede ser negativo");
        validateWeights(alphaA, alphaB);

        Map<String, Double> scores = new HashMap<>();
        Map<String, RankedMovie> movies = new HashMap<>();

        addRrf(a, c, alphaA, scores, movies);
        addRrf(b, c, alphaB, scores, movies);

        return buildResults(scores, movies, k);
    }

    private static void validateWeights(double alphaA, double alphaB) {
        if (!Double.isFinite(alphaA) || !Double.isFinite(alphaB)
                || alphaA < 0 || alphaB < 0
                || Math.abs(alphaA + alphaB - 1.0) > 1e-9) {
            throw new IllegalArgumentException(
                    "Los pesos deben ser no negativos y sumar 1");
        }
    }

    private static void addRrf(
            List<RankedMovie> ranked, int c, double weight,
            Map<String, Double> scores,
            Map<String, RankedMovie> movies) {

        Set<String> seen = new HashSet<>();

        for (int i = 0; i < ranked.size(); i++) {
            RankedMovie r = ranked.get(i);

            if (!seen.add(r.id())) continue;

            scores.merge(r.id(), weight / (c + i + 1), Double::sum);
            movies.putIfAbsent(r.id(), r);
        }
    }

    /**
     * Fusion lineal con normalizacion min-max
     * Las películas ausentes de un ranking reciben puntuación 0 en ese ranking
     */
    public static List<RankedMovie> linear(
            List<RankedMovie> a, List<RankedMovie> b,
            int k, double alpha) {

        if (k < 0) throw new IllegalArgumentException("k no puede ser negativo");

        if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha debe estar entre 0 y 1");
        }

        Map<String, RankedMovie> movies = new HashMap<>();
        Map<String, Double> scoresA = normalize(a, movies);
        Map<String, Double> scoresB = normalize(b, movies);

        Map<String, Double> scores = new HashMap<>();

        for (String id : movies.keySet()) {
            double scoreA = scoresA.getOrDefault(id, 0.0);
            double scoreB = scoresB.getOrDefault(id, 0.0);

            scores.put(id, alpha * scoreA + (1 - alpha) * scoreB);
        }

        return buildResults(scores, movies, k);
    }

    /**
     * Normalizacion min-max de las puntuaciones de un ranking
     */
    private static Map<String, Double> normalize(
            List<RankedMovie> ranked,
            Map<String, RankedMovie> movies) {

        Map<String, Double> original = new HashMap<>();

        for (RankedMovie r : ranked) {
            original.putIfAbsent(r.id(), r.score());
            movies.putIfAbsent(r.id(), r);
        }

        Map<String, Double> normalized = new HashMap<>();

        if (original.isEmpty()) return normalized;

        double min = Collections.min(original.values());
        double max = Collections.max(original.values());

        for (Map.Entry<String, Double> entry : original.entrySet()) {
            double value = (max == min)
                    ? 0.0
                    : (entry.getValue() - min) / (max - min);

            normalized.put(entry.getKey(), value);
        }

        return normalized;
    }

    /**
     * Construye y ordena el ranking final
     */
    private static List<RankedMovie> buildResults(
            Map<String, Double> scores,
            Map<String, RankedMovie> movies,
            int k) {

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(k)
                .map(e -> {
                    RankedMovie m = movies.get(e.getKey());
                    return new RankedMovie(
                            m.id(), m.title(), e.getValue(), -1, "");
                })
                .toList();
    }
}