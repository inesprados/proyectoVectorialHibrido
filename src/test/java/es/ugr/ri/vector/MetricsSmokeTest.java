package es.ugr.ri.vector;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Ejecutar sin Maven: javac -d /tmp/metricstest ... && java -cp /tmp/metricstest ... */
public final class MetricsSmokeTest {
    public static void main(String[] args) {
        var s=RetrievalMetrics.calculate(List.of("A","X","B"),Set.of("A","B"),3);
        near(s.precision(),2.0/3); near(s.recall(),1.0); near(s.f1(),0.8);
        near(s.ap(),(1+2.0/3)/2);
        near(s.ndcg(),(1+1/ (Math.log(4)/Math.log(2)))/(1+1/(Math.log(3)/Math.log(2))));
        var f=RankFusion.rrf(List.of(new RankedMovie("A","A",2,-1,"") ),List.of(new RankedMovie("B","B",1,-1,"") ),60,2);
        if (f.size()!=2) throw new AssertionError();
        System.out.println("OK: métricas y RRF");
    }
    private static void near(double x,double y) { if(Math.abs(x-y)>1e-9) throw new AssertionError(x+" != "+y); }

    @Test
    void relevanteEnPrimeraPosicion() {
        var s = RetrievalMetrics.calculate(
                List.of("A", "B", "C"),
                Set.of("A"), 3);

        assertEquals(1.0 / 3, s.precision(), 1e-9);
        assertEquals(1.0, s.recall(), 1e-9);
        assertEquals(0.5, s.f1(), 1e-9);
        assertEquals(1.0, s.ap(), 1e-9);
        assertEquals(1.0, s.ndcg(), 1e-9);
    }

    @Test
    void relevanteEnUltimaPosicion() {
        var s = RetrievalMetrics.calculate(
                List.of("A", "B", "C"),
                Set.of("C"), 3);

        assertEquals(1.0 / 3, s.precision(), 1e-9);
        assertEquals(1.0, s.recall(), 1e-9);
        assertEquals(0.5, s.f1(), 1e-9);
        assertEquals(1.0 / 3, s.ap(), 1e-9);
        assertEquals(0.5, s.ndcg(), 1e-9);
    }

    @Test
    void variosDocumentosRelevantes() {
        var s = RetrievalMetrics.calculate(
                List.of("A", "B", "C", "D"),
                Set.of("B", "D", "E"), 4);

        assertEquals(0.5, s.precision(), 1e-9);
        assertEquals(2.0 / 3, s.recall(), 1e-9);
        assertEquals(4.0 / 7, s.f1(), 1e-9);
        assertEquals(1.0 / 3, s.ap(), 1e-9);
        assertEquals(0.4981892575, s.ndcg(), 1e-8);
    }

    @Test
    void conjuntoSinDocumentosRelevantes() {
        var s = RetrievalMetrics.calculate(
                List.of("A", "B", "C"),
                Set.of(), 3);

        assertEquals(0.0, s.precision(), 1e-9);
        assertEquals(0.0, s.recall(), 1e-9);
        assertEquals(0.0, s.f1(), 1e-9);
        assertEquals(0.0, s.ap(), 1e-9);
        assertEquals(0.0, s.ndcg(), 1e-9);
    }

    @Test
    void relevantesQueNoAparecenEnElRanking() {
        var s = RetrievalMetrics.calculate(
                List.of("A", "B", "C"),
                Set.of("D", "E"), 3);

        assertEquals(0.0, s.precision(), 1e-9);
        assertEquals(0.0, s.recall(), 1e-9);
        assertEquals(0.0, s.f1(), 1e-9);
        assertEquals(0.0, s.ap(), 1e-9);
        assertEquals(0.0, s.ndcg(), 1e-9);
    }
}