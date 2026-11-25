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

    //Test de solapamiento
    @Test
    void fragmentosConsecutivosSeSolapan() {
        int w = 6, o = 2;
        TextChunker chunker = new TextChunker(w, o);
        List<MovieChunk> chunks = chunker.split(movieWith(30));

        for (int i = 1; i < chunks.size(); i++) {
            String[] anterior = chunks.get(i - 1).text().split("\\s+");
            String[] actual   = chunks.get(i).text().split("\\s+");

            String cola   = String.join(" ", Arrays.copyOfRange(anterior, Math.max(0, (anterior.length - o)), anterior.length));
            String cabeza = String.join(" ", Arrays.copyOfRange(actual, 0, o));

            assertEquals(cola, cabeza, "no hay solapamiento entre #" + (i - 1) + " y #" + i);
        }
    }

    //Tests de valor inválidos
    @Test
    void parametrosInvalidosLanzanExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(4, -1));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(4, 4));
    }

    @Test
    void limiteValidoNoLanzaExcepcion() {
        assertDoesNotThrow(() -> new TextChunker(4, 3));
    }

    @Test
    void chunkIdsSonConsecutivos() {
        TextChunker chunker = new TextChunker(6, 2);
        List<MovieChunk> chunks = chunker.split(movieWith(30));
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).chunkId());
        }
    }

  @Test
    void comprobarSinopsisNull() {
        TextChunker chunker = new TextChunker(4, 1);
        Movie mv = new Movie("1", "T", null);
        List<MovieChunk> chunks = chunker.split(mv);
        assertTrue(chunks.isEmpty());
    }

    /** Cinco oraciones de 3 palabras, cada una empieza en mayúscula. */
    private static final String CINCO = "A b c. D e f. G h i. J k l. M n o.";

    private static List<String> textos(List<MovieChunk> chunks) {
        List<String> out = new ArrayList<>();
        for (MovieChunk c : chunks) out.add(c.text());
        return out;
    }
    
    @Test void abreviaturasNoPartenOraciones() {
        List<String> ss = TextChunker.sentences("Mr. Smith met Dr. Jones. They left.");
        assertEquals(2, ss.size());
        assertEquals("Mr. Smith met Dr. Jones.", ss.get(0));
        assertEquals("They left.", ss.get(1));
    }

    @Test void sinSolapamientoLosFragmentosSonOracionesCompletas() {
        List<MovieChunk> chunks = new TextChunker(6, 0).split(new Movie("1", "T", CINCO));
        assertEquals(List.of("A b c. D e f.", "G h i. J k l.", "M n o."), textos(chunks));
    }

    @Test void solapamientoRepiteLaUltimaOracion() {
        List<MovieChunk> chunks = new TextChunker(6, 3).split(new Movie("1", "T", CINCO));
        assertEquals(List.of("A b c. D e f.", "D e f. G h i.", "G h i. J k l.", "J k l. M n o."), textos(chunks));
    }

    @Test void ningunFragmentoSuperaElMaximo() {
        int[][] configuraciones = {{3, 0}, {4, 1}, {6, 3}, {7, 3}, {9, 5}};
        for (int[] cfg : configuraciones) {
            for (MovieChunk c : new TextChunker(cfg[0], cfg[1]).split(new Movie("1", "T", CINCO))) {
                int n = c.text().split("\\s+").length;
                assertTrue(n <= cfg[0], "fragmento de " + n + " palabras con k=" + cfg[0]);
            }
        }
    }

    @Test void coberturaConOraciones() {
        List<MovieChunk> chunks = new TextChunker(6, 3).split(new Movie("1", "T", CINCO));
        Set<String> vistas = new HashSet<>();
        for (MovieChunk c : chunks) vistas.addAll(Arrays.asList(c.text().split("\\s+")));
        for (String palabra : CINCO.split("\\s+")) assertTrue(vistas.contains(palabra), "falta " + palabra);
    }

    @Test void oracionLargaSeTroceaYLasVecinasSeConservan() {
        String texto = "Aa bb. X0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10 x11 x12. Cc dd.";
        List<MovieChunk> chunks = new TextChunker(5, 2).split(new Movie("1", "T", texto));
        assertEquals(6, chunks.size());
        assertEquals("Aa bb.", chunks.get(0).text());
        assertEquals("Cc dd.", chunks.get(5).text());
        for (int i = 1; i <= 4; i++) assertTrue(chunks.get(i).text().split("\\s+").length <= 5);
    }

    @Test void sinopsisRealesCumplenLasPropiedades() {
        int w = 100;
        for (Movie m : MovieData.sample()) {
            List<MovieChunk> chunks = new TextChunker(w, 25).split(m);
            assertTrue(chunks.size() > 1, m.title() + " debería tener varios fragmentos");
            String[] palabras = m.plot().trim().split("\\s+");
            for (int i = 0; i < chunks.size(); i++) {
                assertEquals(i, chunks.get(i).chunkId());
                assertTrue(chunks.get(i).text().split("\\s+").length <= w);
            }
            assertTrue(chunks.get(chunks.size() - 1).text().endsWith(palabras[palabras.length - 1]),
                    "se pierde el final de " + m.title());
        }
    }
}