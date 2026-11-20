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
                show("BM25", searcher.bm25(query, config.k()));
                show("VECTOR", searcher.vectorChunks(query, config.candidateChunks(), config.k()));
                show("FRAGMENTOS SIN AGRUPAR", searcher.vectorChunkHits(query, config.candidateChunks()));
                show("GLOBAL", searcher.vectorGlobal(query, config.k()));
                show("RRF", searcher.hybridRrf(query, config.candidateChunks(), config.k()));
            
                for (int candidates : new int[]{20, 100, 300}) {
                    System.out.println("\nCANDIDATES = " + candidates);

                    long inicioMax = System.nanoTime();
                    List<RankedMovie> max = searcher.vectorChunks(query, candidates, config.k());
                    long tiempoMax = (System.nanoTime() - inicioMax) / 1_000_000;

                    long inicioAvg2 = System.nanoTime();
                    List<RankedMovie> avg2 = searcher.vectorChunksAvg2(query, candidates, config.k());
                    long tiempoAvg2 = (System.nanoTime() - inicioAvg2) / 1_000_000;

                    System.out.println("MAX: películas distintas = " + max.size()
                            + ", tiempo = " + tiempoMax + " ms");
                    show("RESULTADOS MAX", max);

                    System.out.println("AVG2: películas distintas = " + avg2.size()
                            + ", tiempo = " + tiempoAvg2 + " ms");
                    show("RESULTADOS AVG2", avg2);
                }
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