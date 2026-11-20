
package es.ugr.ri.vector;

import java.util.*;

public final class MovieEvaluator {

    public record Summary(
            String method,
            int queries,
            double p,
            double r,
            double f1,
            double map,
            double ndcg) {}

    public record QueryResult(
            String method,
            String queryId,
            RetrievalMetrics.Scores scores) {}

    public record EvaluationResult(
            Summary summary,
            List<QueryResult> details) {}

    @FunctionalInterface
    public interface Retriever {
        List<RankedMovie> search(String query) throws Exception;
    }

    public static Summary evaluate(
            String method,
            Retriever retriever,
            EvaluationData data,
            int k) throws Exception {

        return evaluateDetailed(method, retriever, data, k).summary();
    }

    public static EvaluationResult evaluateDetailed(
            String method,
            Retriever retriever,
            EvaluationData data,
            int k) throws Exception {

        List<RetrievalMetrics.Scores> all = new ArrayList<>();
        List<QueryResult> details = new ArrayList<>();
        int skipped = 0;

        for (var entry : data.queries.entrySet()) {

            Set<String> relevant =
                    data.relevant.getOrDefault(entry.getKey(), Set.of());

            if (relevant.isEmpty()) {
                skipped++;
                continue;
            }

            List<String> ids = retriever.search(entry.getValue())
                    .stream()
                    .limit(k)
                    .map(RankedMovie::id)
                    .toList();

            RetrievalMetrics.Scores scores =
                    RetrievalMetrics.calculate(ids, relevant, k);

            all.add(scores);
            details.add(new QueryResult(method, entry.getKey(), scores));
        }

        if (skipped > 0) {
            System.out.printf(
                    "%s: %d consultas omitidas (sin relevantes en el corpus)%n",
                    method, skipped);
        }

        Summary summary = new Summary(
                method,
                all.size(),
                RetrievalMetrics.mean(all, RetrievalMetrics.Scores::precision),
                RetrievalMetrics.mean(all, RetrievalMetrics.Scores::recall),
                RetrievalMetrics.mean(all, RetrievalMetrics.Scores::f1),
                RetrievalMetrics.mean(all, RetrievalMetrics.Scores::ap),
                RetrievalMetrics.mean(all, RetrievalMetrics.Scores::ndcg)
        );

        return new EvaluationResult(summary, List.copyOf(details));
    }
}
