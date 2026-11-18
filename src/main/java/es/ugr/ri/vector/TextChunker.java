package es.ugr.ri.vector;
import java.util.*;
/** EJERCICIO: implementación intencionadamente incompleta.
 * Solo conserva las primeras k palabras: pierde el resto del argumento.
 * Sustituir por una segmentación completa con solapamiento y, si es posible, por oraciones.
 */
public final class TextChunker {
    private final int k;
    private final int overlap;
    public TextChunker(int k, int overlap) { if (k < 1 || 0 > overlap || overlap >= k) 
            throw new IllegalArgumentException("k y overlap deben ser positivos y 0 < overlap < k"); this.k = k; this.overlap = overlap; }
    public List<MovieChunk> split(Movie movie) {
        if (movie.plot() == null || movie.plot().isBlank()) return List.of();

        String[] words = movie.plot().trim().split("\\s+");
        List<MovieChunk> fragmentos = new ArrayList<>();
        int pasos = k - overlap;
        int fragmentoId = 0;

        for(int i= 0; i<words.length; i+=pasos){
            int end = Math.min(i+k, words.length);
            String text = String.join(" ", Arrays.copyOfRange(words, i, end));
            fragmentos.add(new MovieChunk(movie.id(), movie.title(), fragmentoId++, text));
            
            if(end == words.length ) break;
        }
        return fragmentos;
    }
}
