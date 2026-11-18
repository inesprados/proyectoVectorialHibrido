package es.ugr.ri.vector;
import java.util.*;
/** EJERCICIO: implementación intencionadamente incompleta.
 * Solo conserva las primeras k palabras: pierde el resto del argumento.
 * Sustituir por una segmentación completa con solapamiento y, si es posible, por oraciones.
 */
public final class TextChunker {
    private final int k;
    public TextChunker(int k) { if (k < 1) throw new IllegalArgumentException("k debe ser positivo"); this.k = k; }
    public List<MovieChunk> split(Movie movie) {
        if (movie.plot() == null || movie.plot().isBlank()) return List.of();

        String[] words = movie.plot().trim().split("\\s+");
        List<MovieChunk> fragmentos = new ArrayList<>();
        int pasos = k;
        int fragmentoId = 0;

        for(int i= 0; i<words.length; i+=pasos){
            int end = Math.min(i+pasos, words.length);
            String text = String.join(" ", Arrays.copyOfRange(words, i, end));
            fragmentos.add(new MovieChunk(movie.id(), movie.title(), fragmentoId++, text));
            
            if(end == words.length ) break;
        }
        return fragmentos;
    }
}
