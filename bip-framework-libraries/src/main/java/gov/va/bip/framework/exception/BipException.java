package gov.va.bip.framework.exception;

import org.springframework.http.HttpStatus;

import gov.va.bip.framework.messages.MessageKey;
import gov.va.bip.framework.messages.MessageKeys;
import gov.va.bip.framework.messages.MessageSeverity;

/**
 * The root BIP class for managing <b>checked</b> exceptions.
 * <p>
 * To support the requirements of consumer responses, all BIP checked Exception classes
 * that will be handled internally by the service should extend this class.
 *
 * @see BipExceptionExtender
 * @see Exception
 *
 * @author aburkholder
 */
public class BipException extends Exception implements BipExceptionExtender {
	private static final long serialVersionUID = 4717771104509731434L;

	/** The consumer facing identity key */
	private final MessageKey key;
	/** Any values needed to fill in params (e.g. value for {0}) in the MessageKey message */
	private final Object[] params;
	/** The severity of the event: FATAL (500 series), ERROR (400 series), WARN (200 series), or INFO/DEBUG/TRACE */
	private final MessageSeverity severity;
	/** The best-fit HTTP Status, see <a href="https://tools.ietf.org/html/rfc7231">https://tools.ietf.org/html/rfc7231</a> */
	private final HttpStatus status;

	/**
	 * Constructs a new <b>checked</b> Exception with the specified detail key, message, severity, and status.
	 * The cause is not initialized, and may subsequently be initialized by
	 * a call to {@link #initCause}.
	 *
	 * @see Exception#Exception(String)
	 *
	 * @param key - the consumer-facing key that can uniquely identify the nature of the exception
	 * @param params - arguments to fill in any params in the MessageKey message (e.g. value for {0})
	 * @param severity - the severity of the event: FATAL (500 series), ERROR (400 series), WARN (200 series), or INFO/DEBUG/TRACE
	 * @param status - the HTTP Status code that applies best to the encountered problem, see
	 *            <a href="https://tools.ietf.org/html/rfc7231">https://tools.ietf.org/html/rfc7231</a>
	 * @param params - arguments to fill in any params in the MessageKey message (e.g. value for {0})
	 */
	public BipException(final MessageKey key, final MessageSeverity severity, final HttpStatus status, Object... params) {
		this(key, severity, status, null, params);
	}

	/**
	 * Constructs a new <b>checked</b> Exception with the specified detail key, message, severity, status, and cause.
	 *
	 * @see Exception#Exception(String, Throwable)
	 *
	 * @param key - the consumer-facing key that can uniquely identify the nature of the exception
	 * @param message - the detail message
	 * @param severity - the severity of the event: FATAL (500 series), ERROR (400 series), WARN (200 series), or INFO/DEBUG/TRACE
	 * @param status - the HTTP Status code that applies best to the encountered problem, see
	 *            <a href="https://tools.ietf.org/html/rfc7231">https://tools.ietf.org/html/rfc7231</a>
	 * @param cause - the throwable that caused this throwable
	 * @param params - arguments to fill in any params in the MessageKey message (e.g. value for {0})
	 */
	public BipException(final MessageKey key, final MessageSeverity severity, final HttpStatus status,
			final Throwable cause, final Object... params) {
		super((key == null ? MessageKeys.NO_KEY.toString() : key.getMessage(params)), cause);
		this.key = (key == null ? MessageKeys.NO_KEY : key);
		this.params = params;
		this.severity = severity;
		this.status = status;
	}

	@Override
	public MessageKey getMessageKey() {
		return key;
	}

	@Override
	public String getKey() {
		return key.getKey();
	}

	@Override
	public Object[] getParams() {
		return params;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public MessageSeverity getSeverity() {
		return severity;
	}

	@Override
	public String getServerName() {
		return System.getProperty("server.name");
	}
}