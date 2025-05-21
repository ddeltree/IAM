package poo.api.exceptions;

public class NotFoundException extends RuntimeException {
  public static final int STATUS_CODE = 404;

  public NotFoundException() {
    super("Recurso não encontrado");
  }

  public NotFoundException(String message) {
    super(message);
  }
}
