/**
 * Generic runtime exception.
 */
package net.ex337.hotrepart.loadtester;

public class LoadTesterRuntimeException extends RuntimeException {
	public LoadTesterRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
	public LoadTesterRuntimeException(String message) {
		super(message);
	}
	public LoadTesterRuntimeException(Throwable cause) {
		super(cause);
	}
}