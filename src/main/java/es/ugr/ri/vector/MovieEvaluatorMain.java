
package es.ugr.ri.vector;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.lucene.index.DirectoryReader;

public final class MovieEvaluatorMain {

    private static final Path SAMPLE_QUERIES =
            Path.of("src/main/resources/evaluation/sample_queries.tsv");

    private static final Path SAMPLE_QRELS =
            Path.of("src/main/resources/evaluation/sample_qrels.txt");

    public static void main(String[] args) throws Exception {

        String source = args.length == 0 ? "sample" : args[0];
        boolean sample = source.equalsIgnoreCase("sample");

        if ((!sample && args.length != 3)
                || (sample && args.length != 1 && args.length != 3 && args.length != 0)) {

            throw new IllegalArgumentException(
                    "Uso: MovieEvaluatorMain sample [consultas.tsv qrels.txt] "
                    + "| <directorio-indices> <consultas.tsv> <qrels.txt>");
        }

        Path queries = args.length == 3
                ? Path.of(args[1]) : SAMPLE_QUERIES;

        Path qrels = args.length == 3
                ? Path.of(args[2]) : SAMPLE_QRELS;

        RunConfig config = RunConfig.fromProperties();
        EvaluationData data = EvaluationData.load(queries, qrels);

        try (IndexDirectories indexes = sample
                ? IndexDirectories.memory()
                : IndexDirectories.disk(Path.of(source))) {

            if (sample) {
                var movies = MovieData.sample();

                var stats = new MovieIndexer().build(
                        movies,
                        indexes,
                        new TextChunker(
                                config.chunkWords(),
                                config.chunkOverlap()),
                        new EmbeddingService());

                System.out.printf(
                        "Modo RAM: %d películas, %d fragmentos%n",
                        stats.movies(), stats.chunks());
            }

            // Obtener los identificadores de las películas indexadas.
            Set<String> ids = indexedMovieIds(indexes);

            // Restringir los juicios a las películas presentes en el índice.
            EvaluationData restricted = data.restrictTo(ids);

            int missing = data.relevant.values().stream()
                    .mapToInt(s -> (int) s.stream()
                            .filter(id -> !ids.contains(id))
                            .count())
                    .sum();

            if (missing > 0) {
                System.err.printf(
                        "AVISO: %d juicios positivos hacen referencia "
                        + "a películas ausentes del índice%n",
                        missing);
            }

            System.out.printf(
                    "Modo=%s | películas indexadas=%d | consultas=%s "
                    + "| qrels=%s | k=%d%n",
                    sample ? "RAM" : "DISCO",
                    ids.size(),
                    queries,
                    qrels,
                    config.k());

            try (MovieSearcher searcher =
                    new MovieSearcher(indexes, new EmbeddingService())) {

                int k = config.k();

                // Evaluación de los seis sistemas.
                List<MovieEvaluator.EvaluationResult> results = List.of(

                    MovieEvaluator.evaluateDetailed(
                            "BM25",
                            q -> searcher.bm25(q, k),
                            restricted, k),

                    MovieEvaluator.evaluateDetailed(
                            "VECTOR",
                            q -> searcher.vectorChunks(
                                    q, config.candidateChunks(), k),
                            restricted, k),

                    MovieEvaluator.evaluateDetailed(
                            "GLOBAL",
                            q -> searcher.vectorGlobal(q, k),
                            restricted, k),

                    MovieEvaluator.evaluateDetailed(
                            "RRF",
                            q -> searcher.hybridRrf(
                                    q, config.candidateChunks(), k),
                            restricted, k),

                    MovieEvaluator.evaluateDetailed(
                            "WEIGHTED RRF",
                            q -> RankFusion.rrfWeighted(
                                    searcher.bm25(q, k),
                                    searcher.vectorChunks(
                                            q, config.candidateChunks(), k),
                                    60, k, 0.7, 0.3),
                            restricted, k),

                    MovieEvaluator.evaluateDetailed(
                            "LINEAR",
                            q -> RankFusion.linear(
                                    searcher.bm25(q, k),
                                    searcher.vectorChunks(
                                            q, config.candidateChunks(), k),
                                    k, 0.5),
                            restricted, k)
                );

                // Tabla de resultados medios.
                System.out.println();
                System.out.println("=== RESULTADOS MEDIOS ===");

                System.out.printf(
                        "%-14s %5s %8s %8s %8s %8s %8s%n",
                        "SISTEMA", "N", "P@k", "R@k",
                        "F1@k", "MAP@k", "NDCG@k");

                System.out.println(
                        "-----------------------------------------------------------------------");

                for (var result : results) {

                    var r = result.summary();

                    System.out.printf(
                            Locale.ROOT,
                            "%-14s %5d %8.3f %8.3f %8.3f %8.3f %8.3f%n",
                            r.method(),
                            r.queries(),
                            r.p(),
                            r.r(),
                            r.f1(),
                            r.map(),
                            r.ndcg());
                }

                // Tabla de resultados individuales por consulta.
                System.out.println();
                System.out.println("=== RESULTADOS POR CONSULTA ===");

                System.out.printf(
                        "%-14s %-8s %8s %8s %8s %8s %8s%n",
                        "SISTEMA", "CONSULTA", "P@k", "R@k",
                        "F1@k", "AP@k", "NDCG@k");

                System.out.println(
                        "-----------------------------------------------------------------------");

                for (var result : results) {

                    for (var detail : result.details()) {

                        var s = detail.scores();

                        System.out.printf(
                                Locale.ROOT,
                                "%-14s %-8s %8.3f %8.3f %8.3f %8.3f %8.3f%n",
                                detail.method(),
                                detail.queryId(),
                                s.precision(),
                                s.recall(),
                                s.f1(),
                                s.ap(),
                                s.ndcg());
                    }
                }
            }
        }
    }

    private static Set<String> indexedMovieIds(
            IndexDirectories indexes) throws Exception {

        Set<String> ids = new HashSet<>();

        try (DirectoryReader reader =
                DirectoryReader.open(indexes.lexical)) {

            for (var leaf : reader.leaves()) {

                var live = leaf.reader().getLiveDocs();

                for (int doc = 0;
                        doc < leaf.reader().maxDoc();
                        doc++) {

                    if (live != null && !live.get(doc)) {
                        continue;
                    }

                    String id = leaf.reader()
                            .storedFields()
                            .document(doc)
                            .get("movie_id");

                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
        }

        return ids;
    }
}