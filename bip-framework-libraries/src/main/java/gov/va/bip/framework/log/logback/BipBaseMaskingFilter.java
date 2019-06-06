package gov.va.bip.framework.log.logback;

import static java.util.regex.Pattern.compile;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import gov.va.bip.framework.exception.BipRuntimeException;
import gov.va.bip.framework.log.BipLogger;
import gov.va.bip.framework.log.BipLoggerFactory;
import gov.va.bip.framework.log.logback.BipMaskRule.Definition;
import gov.va.bip.framework.messages.MessageKeys;
import gov.va.bip.framework.messages.MessageSeverity;

public abstract class BipBaseMaskingFilter extends EventEvaluatorBase<ILoggingEvent> {
	/** Class logger */
	private static final BipLogger LOGGER = BipLoggerFactory.getLogger(BipBaseMaskingFilter.class);

//	/** POJO with rule definition values in it */
//	BipBaseMaskingFilter.Definition definition;

	public BipBaseMaskingFilter() {
	}

	@Override
	public boolean evaluate(ILoggingEvent event) throws NullPointerException, EvaluationException {
		if (maskPattern == null) {
			this.maskPattern = parseAndCompile();
		}

		String message = apply(event.getMessage());
		updateMessage(event, message);

		Object[] args = event.getArgumentArray();
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				args[i] = apply((String) args[i]);
			}
		}
		updateArgs(event, args);

		return true;
	}

	/**
	 * Applies the masking rule to the input string.
	 *
	 * @param input - the PII that needs to be masked.
	 * @return the masked version of the input.
	 */
	String apply(String input) {
		Matcher matcher = this.maskPattern.matcher(input);
		if (matcher.find()) {
			String match = matcher.group(1);
			// matcher.group(x) can return null, even if matcher.find() succeeded
			if (match != null) {
				int unmaskedLen = (match.length() - this.unmasked < 0 ? 0 : match.length() - this.unmasked);
				String mask = StringUtils.repeat("*", Math.min(match.length(), unmaskedLen));
				String replacement = mask + match.substring(mask.length());
				return input.replace(match, replacement);
			}
		}
		return input;
	}

	protected void updateMessage(ILoggingEvent event, String updatedMessage)
			throws SecurityException, IllegalArgumentException {
		try {
			Field field = event.getClass().getDeclaredField("message");
			field.setAccessible(true);
			field.set(event, updatedMessage);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			LOGGER.error("Programming error: could not set log message due to " + e.getClass().getSimpleName());
			throw new BipRuntimeException(MessageKeys.BIP_DEV_ILLEGAL_INVOCATION, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST,
					e.getClass().getName(), "set()", "Field [message]", ILoggingEvent.class.getName());
		}
	}

	private void updateArgs(ILoggingEvent event, Object[] args) {
		try {
			Field field = event.getClass().getDeclaredField("argumentArray");
			field.setAccessible(true);
			field.set(event, args);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			LOGGER.error("Programming error: could not set log message due to " + e.getClass().getSimpleName());
			throw new BipRuntimeException(MessageKeys.BIP_DEV_ILLEGAL_INVOCATION, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST,
					e.getClass().getName(), "set()", "Field [argumentArray]", ILoggingEvent.class.getName());
		}
	}

//	/**
//	 * A simple POJO with data that can be used to create a new rule instance.
//	 * <p>
//	 * Logback expects specific fields with getters and setters to be available
//	 * in the Definition class: name, prefix, suffix, pattern, unmasked.
//	 * <p>
//	 * During initialization, logback uses the {@code bip-framework-logback-starter.xml}
//	 * tag names by convention to construct the names of methods and classes it will
//	 * expect to be available for its use.
//	 * In this case, the {@code <rule>} tag name is used to find the {@link BipMaskRules#addRule(BipMaskRule.Definition)},
//	 * from which this inner class name is determined.
//	 *
//	 * @see BipMaskRule
//	 * @see ch.qos.logback.classic.joran.JoranConfigurator
//	 */
//	public static class Definition {

	/** A friendly name for the rule */
	private String name;
	/** A regex prefix to the main pattern */
	private String prefix = "";
	/** A regex suffix to the main pattern */
	private String suffix = "";
	/** The regex by which matches are identified */
	private String pattern;
	/** The number of characters to be left unmasked */
	private int unmasked = 0;

	/** Compiled pattern used to remove braces from a string */
	Pattern bracesPattern = Pattern.compile("[{}]+");
	/** Compiled masking pattern */
	Pattern maskPattern;

//		/* ***************************** CONSTRUCTORS ***************************** */
//
//		/**
//		 * Instantiates a new rule definition POJO.
//		 * <p>
//		 * This is the constructor called by logback.
//		 * Values are set later in its process.
//		 */
//		public Definition() {
//			this("", "");
//		}
//
//		/**
//		 * Instantiates a new rule definition POJO.
//		 *
//		 * @param name - a friendly name for the rule.
//		 * @param pattern - a regular expression pattern to identify the personally identifiable information.
//		 */
//		public Definition(String name, String pattern) {
//			this(name, "", "", pattern, 0);
//		}
//
//		/**
//		 * Instantiates a new rule definition POJO.
//		 *
//		 * @param name - a friendly name for the rule.
//		 * @param prefix - a literal prefix preceding the actual search pattern
//		 * @param suffix - a literal suffix following the actual search pattern
//		 * @param pattern - a regular expression pattern to identify the personally identifiable information.
//		 * @param unmasked - the number of characters to leave unmasked.
//		 */
//		public Definition(String name, String prefix, String suffix, String pattern, int unmasked) {
//			setName(name);
//			setPrefix(prefix);
//			setSuffix(suffix);
//			setPattern(pattern);
//			setUnmasked(unmasked);
//		}

	/* ***************************** PROPERTIES ***************************** */

	/**
	 * The friendly rule name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * The friendly rule name.
	 * <p>
	 * Called by logback to set the value, if a {@code <name>} tag was provided in the config.
	 *
	 * @param name
	 *            the name to set
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The literal prefix preceding the actual search pattern
	 *
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * The literal prefix preceding the actual search pattern
	 * <p>
	 * Called by logback to set the value, if a {@code <prefix>} tag was provided in the config.
	 *
	 * @param prefix - the prefix to set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * The literal suffix following the actual search pattern
	 *
	 * @return the suffix
	 */
	public String getSuffix() {
		return suffix;
	}

	/**
	 * The literal suffix following the actual search pattern
	 * <p>
	 * Called by logback to set the value, if a {@code <suffix>} tag was provided in the config.
	 *
	 * @param suffix - the suffix to set
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * The regular expression pattern to identify the personally identifiable information.
	 *
	 * @return the regex pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * The regular expression pattern to identify the personally identifiable information.
	 * <p>
	 * Called by logback to set the value, if a {@code <pattern>} tag was provided in the config.
	 *
	 * @param pattern - the regex pattern to set
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * The number of characters to leave unmasked
	 *
	 * @return the unmasked
	 */
	public int getUnmasked() {
		return unmasked;
	}

	/**
	 * The number of characters to leave unmasked
	 * <p>
	 * Called by logback to set the value, if a {@code <unmasked>} tag was provided in the config.
	 *
	 * @param unmasked - the unmasked to set
	 */
	public void setUnmasked(int unmasked) {
		this.unmasked = unmasked;
	}

	/* ***************************** BEHAVIORS ***************************** */

//		/**
//		 * Create a Rule instance from this definition.
//		 * <p>
//		 * During initialization, logback uses the {@code bip-framework-logback-starter.xml}
//		 * tag names by convention to construct the names of methods and classes it will
//		 * expect to be available for its use.
//		 * In this case, the {@code <rule>} tag name prescribes the method to get a .
//		 *
//		 * @return the mask rule
//		 */
//		public BipMaskRule rule() {
//			if (StringUtils.isNotBlank(this.pattern)) {
//				this.maskPattern = parseAndCompile();
//			}
//			return new BipMaskRule(this);
//		}

	/**
	 * Places regex grouping parens around the pattern, and establishes the prefix as a regex lookbehind
	 * and the suffix as a regex lookahead. The result is a clearly defined grouping regex of one group.
	 *
	 * @return the complete pattern, including prefix and suffix
	 */
	private Pattern parseAndCompile() {
		String parsedPrefix = StringUtils.isBlank(this.prefix) ? "" : "(?<=" + this.prefix + ")(?:\\s*)";
		String parsedSuffix = StringUtils.isBlank(this.suffix) ? "" : "(?:\\s*)(?=" + this.suffix + ")";
		String validatedPattern = pattern.startsWith("(") ? this.pattern : "(" + this.pattern + ")";
		return compile(parsedPrefix + validatedPattern + parsedSuffix, Pattern.DOTALL | Pattern.MULTILINE);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
		result = prime * result + unmasked;
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Definition)) {
			return false;
		}
//			Definition other = (Definition) obj;
		BipBaseMaskingFilter other = (BipBaseMaskingFilter) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (pattern == null) {
			if (other.pattern != null) {
				return false;
			}
		} else if (!pattern.equals(other.pattern)) {
			return false;
		}
		if (prefix == null) {
			if (other.prefix != null) {
				return false;
			}
		} else if (!prefix.equals(other.prefix)) {
			return false;
		}
		if (suffix == null) {
			if (other.suffix != null) {
				return false;
			}
		} else if (!suffix.equals(other.suffix)) {
			return false;
		}
		if (unmasked != other.unmasked) {
			return false;
		}
		return true;
	}
}
//}
