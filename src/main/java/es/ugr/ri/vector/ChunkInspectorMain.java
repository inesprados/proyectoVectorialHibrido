package es.ugr.ri.vector;
import java.util.*;

/** Comprobación obligatoria: inspección visual de la segmentación. */
public final class ChunkInspectorMain {
    public static void main(String[] args) throws Exception {
        RunConfig c = RunConfig.fromProperties();
        List<Movie> movies = MovieRepository.load(args.length > 0 ? args[0] : "sample", c.limit());
        TextChunker chunker = new TextChunker(c.chunkWords(), c.chunkOverlap());

        for (Movie m : movies) {
            List<MovieChunk> chunks = chunker.split(m);
            int totalWords = m.plot().isBlank() ? 0 : m.plot().trim().split("\\s+").length;
            System.out.printf("%n%s: %d palabras -> %d fragmentos%n", m.title(), totalWords, chunks.size());
            for (MovieChunk ch : chunks) {
                String[] w = ch.text().split("\\s+");
                System.out.printf("  #%d (%d palabras) %s ... %s%n", ch.chunkId(), w.length,
                        firstWords(w, 8), lastWords(w, 8));
            }
        }
    }

    private static String firstWords(String[] w, int n) {
        return String.join(" ", Arrays.copyOfRange(w, 0, Math.min(n, w.length)));
    }

    private static String lastWords(String[] w, int n) {
        return String.join(" ", Arrays.copyOfRange(w, Math.max(0, w.length - n), w.length));
    }
}