package es.ugr.ri.vector;
import java.util.*;

/** Comprobación obligatoria: inspección visual de la segmentación. */
public final class ChunkInspectorMain {
    public static void main(String[] args) throws Exception {
        RunConfig c = RunConfig.fromProperties();
        List<Movie> movies = MovieRepository.load(args.length > 0 ? args[0] : "sample", c.limit());
        TextChunker chunker = new TextChunker(c.chunkWords(), c.chunkOverlap());
        System.out.printf("Configuración: w=%d, o=%d%n", c.chunkWords(), c.chunkOverlap());

        for (Movie m : movies) {
            List<MovieChunk> chunks = chunker.split(m);
            String[] plot = m.plot().trim().split("\\s+");
            System.out.printf("%n%s: %d palabras -> %d fragmentos%n", m.title(), plot.length, chunks.size());
            String[] prev = null;
            for (MovieChunk ch : chunks) {
                String[] w = ch.text().split("\\s+");
                int shared = prev == null ? 0 : commonWords(prev, w);
                System.out.printf("  #%d (%d palabras) %s ... %s%n", ch.chunkId(), w.length, shared, 
                        firstWords(w, 8), lastWords(w, 8));

                if (w.length > c.chunkWords())
                    System.out.println("    !! supera el tamaño máximo");
                if (!Character.isUpperCase(w[0].charAt(0)) && !w[0].startsWith("\"") && !Character.isDigit(w[0].charAt(0)))
                    System.out.println("    ?? no empieza como una oración (puede ser un trozo de oración larga)");
                if (prev != null && c.chunkOverlap() > 0 && shared < c.chunkOverlap())
                    System.out.println("    ?? solape menor que o (no cabía, o junto a una oración larga)");
                prev = w;
            }
        }
    }

    /* Mayor número de palabras que son final de 'prev' y comienzo de 'cur'. */
    private static int commonWords(String[] prev, String[] cur) {
        int best = 0;
        for (int m = 1; m <= Math.min(prev.length, cur.length); m++) {
            boolean ok = true;
            for (int i = 0; i < m && ok; i++) ok = prev[prev.length - m + i].equals(cur[i]);
            if (ok) best = m;
        }
        return best;
    }

    private static String firstWords(String[] w, int n) {
        return String.join(" ", Arrays.copyOfRange(w, 0, Math.min(n, w.length)));
    }

    private static String lastWords(String[] w, int n) {
        return String.join(" ", Arrays.copyOfRange(w, Math.max(0, w.length - n), w.length));
    }
}

    