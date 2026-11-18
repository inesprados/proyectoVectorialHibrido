package es.ugr.ri.vector;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    /** Crea una película cuya sinopsis tiene n palabras: "w0 w1 w2 ... w(n-1)". */
    private static Movie movieWith(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append("w").append(i).append(" ");
        return new Movie("1", "T", sb.toString().trim());
    }

    @Test
    void sinopsisVaciaNoGeneraFragmentos() {
        TextChunker chunker = new TextChunker(4, 0);
        assertEquals(0, chunker.split(movieWith(0)).size());
    }

    @Test
    void sinopsisDeLongitudWGeneraUnFragmento() {
        TextChunker chunker = new TextChunker(4, 0);
        List<MovieChunk> chunks = chunker.split(movieWith(4));
        assertEquals(1, chunks.size());
        assertEquals("w0 w1 w2 w3", chunks.get(0).text());
    }

    @Test
    void sinopsisDeLongitudWMenos1GeneraUnFragmento() {
        TextChunker chunker = new TextChunker(4, 0);
        List<MovieChunk> chunks = chunker.split(movieWith(3));
        assertEquals(1, chunks.size());
        assertEquals("w0 w1 w2", chunks.get(0).text());
    }

    @Test
    void sinopsisDeLongitudWMasUnoConSolapamiento() {
        TextChunker chunker = new TextChunker(4, 1);
        List<MovieChunk> chunks = chunker.split(movieWith(5));
        assertEquals(2, chunks.size());
        assertEquals("w0 w1 w2 w3", chunks.get(0).text());
        assertEquals("w3 w4", chunks.get(1).text());
    }

    @Test
     void sinopsisDeLongitud2WMasUnoConSolapamiento() {
        TextChunker chunker = new TextChunker(4, 1);
        List<MovieChunk> chunks = chunker.split(movieWith(9));
        assertEquals(3, chunks.size());
        assertEquals("w0 w1 w2 w3", chunks.get(0).text());
        assertEquals("w3 w4 w5 w6", chunks.get(1).text());
        assertEquals("w6 w7 w8", chunks.get(2).text());


    }

    //Comprobación de la cobertura
    @Test
    void todasLasPalabrasAparecenEnAlgunFragmento() {
        int n = 23;
        TextChunker chunker = new TextChunker(5, 2);
        List<MovieChunk> chunks = chunker.split(movieWith(n));

        Set<String> vistas = new HashSet<>();
        for (MovieChunk ch : chunks) {
            vistas.addAll(Arrays.asList(ch.text().split("\\s+")));
        }

        for (int i = 0; i < n; i++) {
            assertTrue(vistas.contains("w"+i), "falta la palabra w" + i);
        }
    }
}