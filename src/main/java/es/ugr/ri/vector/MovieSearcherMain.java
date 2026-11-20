package es.ugr.ri.vector;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
/** Búsqueda: sample <consulta...> o <directorio-indices> <consulta...>. */
public final class MovieSearcherMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("Uso: MovieSearcherMain <sample|directorio-indices> <consulta...>");
        boolean sample = args[0].equalsIgnoreCase("sample");
        RunConfig config = RunConfig.fromProperties();
        try (IndexDirectories indexes = sample ? IndexDirectories.memory() : IndexDirectories.disk(Path.of(args[0]))) {
            if (sample) new MovieIndexer().build(MovieData.sample(), indexes,
                    new TextChunker(config.chunkWords(), config.chunkOverlap()), new EmbeddingService());
            String query = String.join(" ", java.util.Arrays.copyOfRange(args,1,args.length));
                        
            try (MovieSearcher searcher = new MovieSearcher(indexes, new EmbeddingService())) {

                int k = config.k();

                // Recuperación individual
                List<RankedMovie> bm25 = searcher.bm25(query, k);
                List<RankedMovie> vector =
                        searcher.vectorChunks(query, config.candidateChunks(), k);

                // Fusión RRF sin ponderar
                List<RankedMovie> rrf =
                        RankFusion.rrf(bm25, vector, 60, k);

                // Fusión RRF ponderada: 70% BM25 y 30% VECTOR
                List<RankedMovie> weightedRrf =
                        RankFusion.rrfWeighted(bm25, vector, 60, k, 0.7, 0.3);

                // Fusión lineal con alpha = 0.5
                List<RankedMovie> linear =
                        RankFusion.linear(bm25, vector, k, 0.5);

                // Mostrar los cinco métodos
                show("BM25", bm25);
                show("VECTOR", vector);
                show("RRF", rrf);
                show("WEIGHTED RRF", weightedRrf);
                show("LINEAR FUSION", linear);
            }
        }
    }
        
    private static void show(String method, List<RankedMovie> results) {
        System.out.println("\n" + method);

        for (int i = 0; i < results.size(); i++) {
            RankedMovie r = results.get(i);

            System.out.printf(Locale.ROOT,
                    "%2d %-22s score=%.4f id=%s chunk=%d%n",
                    i + 1, r.title(), r.score(), r.id(), r.chunkId());

            if (r.chunkId() >= 0 && r.excerpt() != null
                    && !r.excerpt().isBlank()) {
                System.out.println("    Fragmento: " + r.excerpt());
            }
        }
    }
}