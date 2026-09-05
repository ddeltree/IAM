package poo.api.exceptions;

public class UnauthorizedException extends RuntimeException {
  public static final int STATUS_CODE = 401;

  public UnauthorizedException() {
    super("Você precisa se autenticar para acessar este recurso");
  }

  public UnauthorizedException(String message) {
    super(message);
  }
}
