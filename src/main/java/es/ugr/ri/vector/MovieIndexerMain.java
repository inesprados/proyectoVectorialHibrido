package es.ugr.ri.vector;
import java.nio.file.Path;
/** Indexador: argumento 1 = JSONL o sample; argumento 2 = directorio destino. */
public final class MovieIndexerMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: MovieIndexerMain <sample|peliculas.jsonl> <directorio-indices>");
        }
        RunConfig config = RunConfig.fromProperties();
        var movies = MovieRepository.load(args[0], config.limit());
        EmbeddingService embeddings = new EmbeddingService();   // el modelo ONNX se carga aquí, fuera de la medición
        try (IndexDirectories indexes = IndexDirectories.disk(Path.of(args[1]))) {
            long t0 = System.nanoTime();
            var stats = new MovieIndexer().build(movies, indexes,
                    new TextChunker(config.chunkWords(), config.chunkOverlap()), embeddings);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf("w=%d o=%d | %d películas, %d fragmentos | %d ms%n",
                    config.chunkWords(), config.chunkOverlap(), stats.movies(), stats.chunks(), ms);
    
        }
    }
}           