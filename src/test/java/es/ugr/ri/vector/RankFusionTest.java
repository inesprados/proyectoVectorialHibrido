
package es.ugr.ri.vector;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RankFusionTest {

    private RankedMovie movie(String id, double score) {
        return new RankedMovie(id, id, score, -1, "");
    }

    @Test
    void testRrfManualScoresTiesAndMissingDocuments() {
        // Ranking 1: A, B, C
        List<RankedMovie> ranking1 = List.of(
                movie("A", 10),
                movie("B", 8),
                movie("C", 6)
        );

        // Ranking 2: B, A, D
        List<RankedMovie> ranking2 = List.of(
                movie("B", 10),
                movie("A", 8),
                movie("D", 6)
        );

        int c = 60;
        List<RankedMovie> result =
                RankFusion.rrf(ranking1, ranking2, c, 4);

        // A y B aparecen en ambos rankings y obtienen la misma puntuación
        double scoreAB = 0.5 / 61.0 + 0.5 / 62.0;

        // C y D aparecen solamente en uno de los rankings
        double scoreCD = 0.5 / 63.0;

        assertEquals(4, result.size());

        // Se comprueba el orden, incluyendo los empates.
        assertEquals("A", result.get(0).id());
        assertEquals("B", result.get(1).id());
        assertEquals("C", result.get(2).id());
        assertEquals("D", result.get(3).id());

        assertEquals(scoreAB, result.get(0).score(), 1e-9);
        assertEquals(scoreAB, result.get(1).score(), 1e-9);
        assertEquals(scoreCD, result.get(2).score(), 1e-9);
        assertEquals(scoreCD, result.get(3).score(), 1e-9);
    }

    @Test
    void testWeightedRrf() {
        List<RankedMovie> ranking1 = List.of(
                movie("A", 10),
                movie("B", 8)
        );

        List<RankedMovie> ranking2 = List.of(
                movie("B", 10),
                movie("A", 8)
        );

        List<RankedMovie> result =
                RankFusion.rrfWeighted(ranking1, ranking2, 60, 2, 0.7, 0.3);

        double scoreA = 0.7 / 61.0 + 0.3 / 62.0;
        double scoreB = 0.7 / 62.0 + 0.3 / 61.0;

        assertEquals("A", result.get(0).id());
        assertEquals("B", result.get(1).id());

        assertEquals(scoreA, result.get(0).score(), 1e-9);
        assertEquals(scoreB, result.get(1).score(), 1e-9);
    }

    @Test
    void testLinearFusion() {
        List<RankedMovie> bm25 = List.of(
                movie("A", 10),
                movie("B", 6)
        );

        List<RankedMovie> vector = List.of(
                movie("B", 8),
                movie("C", 4)
        );

        List<RankedMovie> result =
                RankFusion.linear(bm25, vector, 3, 0.7);

        // BM25 normalizado: A=1, B=0
        // Vector normalizado: B=1, C=0
        // Resultado: A=0.7, B=0.3, C=0
        assertEquals(3, result.size());

        assertEquals("A", result.get(0).id());
        assertEquals("B", result.get(1).id());
        assertEquals("C", result.get(2).id());

        assertEquals(0.7, result.get(0).score(), 1e-9);
        assertEquals(0.3, result.get(1).score(), 1e-9);
        assertEquals(0.0, result.get(2).score(), 1e-9);
    }

    @Test
    void testInvalidWeightedRrfParameters() {
        List<RankedMovie> ranking = List.of(movie("A", 1));

        assertThrows(IllegalArgumentException.class,
                () -> RankFusion.rrfWeighted(
                        ranking, ranking, 60, 1, 0.7, 0.5));

        assertThrows(IllegalArgumentException.class,
                () -> RankFusion.rrfWeighted(
                        ranking, ranking, 60, 1, -0.1, 1.1));
    }

    @Test
    void testInvalidLinearAlpha() {
        List<RankedMovie> ranking = List.of(movie("A", 1));

        assertThrows(IllegalArgumentException.class,
                () -> RankFusion.linear(ranking, ranking, 1, -0.1));

        assertThrows(IllegalArgumentException.class,
                () -> RankFusion.linear(ranking, ranking, 1, 1.1));
    }
}