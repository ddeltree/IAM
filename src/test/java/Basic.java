// src/test/java/com/exemplo/TesteBasico.java

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Basic {

  @Test
  public void testSoma() {
    int resultado = 2 + 2;
    assertEquals(4, resultado);
  }
}
