package poo.api.exceptions;

public class ForbiddenException extends RuntimeException {
  public static final int STATUS_CODE = 403;

  public ForbiddenException() {
    super("Acesso negado");
  }

  public ForbiddenException(String message) {
    super(message);
  }
}
