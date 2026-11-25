package es.ugr.ri.vector;
import java.text.BreakIterator;
import java.util.*;
/** EJERCICIO: implementación intencionadamente incompleta.
 * Solo conserva las primeras k palabras: pierde el resto del argumento.
 * Sustituir por una segmentación completa con solapamiento y, si es posible, por oraciones.
 */
public final class TextChunker {
    private final int k;
    private final int overlap;
    
    private static final Set<String> ABBREVIATIONS =Set.of("Dr.", "Mr.", "Mrs.", "Ms.", "St.", "Jr.", "Sr.");
    
    private static boolean endsWithAbbreviation(String sentence) {
        String[] words = sentence.split("\\s+");
        return ABBREVIATIONS.contains(words[words.length - 1]);
    }
    
    public TextChunker(int k, int overlap) { if (k < 1 || 0 > overlap || overlap >= k) 
            throw new IllegalArgumentException("k y overlap deben ser positivos y 0 <= overlap < k"); this.k = k; this.overlap = overlap; }
    

    static List<String> sentences(String text) {
        
        BreakIterator it = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        it.setText(text);
        List<String> out = new ArrayList<>();
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String s = text.substring(start, end);
            String t = s.trim();

            if (t.isEmpty()) continue;
            if (!out.isEmpty() && endsWithAbbreviation(out.get(out.size() - 1))) {
                out.set(out.size() - 1, out.get(out.size() - 1) + " " + t);
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private List<MovieChunk> splitByWords(Movie movie) {
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



    public List<MovieChunk> split(Movie movie) {
        if (movie.plot() == null || movie.plot().isBlank()) return List.of();
        List<String> sents = sentences(movie.plot());
        List<MovieChunk> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentWords = 0;
        int newCount = 0;
        for (String s : sents) {
            int n = s.trim().split("\\s+").length;

            // oración más larga que k -> se trocea por palabras con solape exacto
            if (n > k) {
                if (newCount > 0) {
                    chunks.add(new MovieChunk(movie.id(), movie.title(), chunks.size(), String.join(" ", current)));
                }
                current = new ArrayList<>();
                currentWords = 0;
                newCount = 0;
                String[] words = s.trim().split("\\s+");
                int paso = k - overlap;
                for (int i = 0; i < words.length; i += paso) {
                    int end = Math.min(i + k, words.length);
                    String text = String.join(" ", Arrays.copyOfRange(words, i, end));
                    chunks.add(new MovieChunk(movie.id(), movie.title(), chunks.size(), text));
                    if (end == words.length) break;
                }
                continue;
            }

            if (currentWords + n > k && newCount > 0) {
                chunks.add(new MovieChunk(movie.id(), movie.title(), chunks.size(), String.join(" ", current)));
                List<String> carry = new ArrayList<>();
                int carryWords = 0;
                for (int i = current.size() - 1; i >= 0 && carryWords < overlap; i--) {
                    carry.add(0, current.get(i));
                    carryWords += current.get(i).trim().split("\\s+").length;
                }
                while (!carry.isEmpty() && carryWords + n > k) {
                    carryWords -= carry.remove(0).trim().split("\\s+").length;
                }
                current = carry;
                currentWords = carryWords;
                newCount = 0;
            }
            current.add(s);
            currentWords += n;
            newCount++;
        }
        if (newCount > 0)
            chunks.add(new MovieChunk(movie.id(), movie.title(), chunks.size(), String.join(" ", current)));
        return chunks;
    }
}