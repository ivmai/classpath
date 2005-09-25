/* Formatter.java -- printf-style formatting
   Copyright (C) 2005 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.
 
GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */


package java.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/** @since 1.5 */
public class Formatter implements Closeable, Flushable
{
  private Appendable out;
  private Locale locale;
  private boolean closed;
  private IOException ioException;

  // Some state used when actually formatting.
  private String format;
  private int index;
  private int length;
  private Locale fmtLocale;

  // Note that we include '-' twice.  The flags are ordered to
  // correspond to the values in FormattableFlags, and there is no
  // flag (in the sense of this field used when parsing) for
  // UPPERCASE; the second '-' serves as a placeholder.
  private static final String FLAGS = "--#+ 0,(";

  private static final String lineSeparator
    = System.getProperty("line.separator");

  public enum BigDecimalLayoutForm
  {
    DECIMAL_FLOAT,
    SCIENTIFIC
  }

  public Formatter()
  {
    this.out = new StringBuilder();
  }

  public Formatter(Locale loc)
  {
    this.out = new StringBuilder();
    this.locale = loc;
  }

  public Formatter(Appendable app)
  {
    this(app, null);
  }

  public Formatter(Appendable app, Locale loc)
  {
    this.out = app == null ? new StringBuilder() : app;
    this.locale = loc;
  }

  public Formatter(File file) throws FileNotFoundException
  {
    this.out = new OutputStreamWriter(new FileOutputStream(file));
  }

  public Formatter(File file, String charset)
    throws FileNotFoundException, UnsupportedEncodingException
  {
    this(file, charset, null);
  }

  public Formatter(File file, String charset, Locale loc)
    throws FileNotFoundException, UnsupportedEncodingException
  {
    this.out = new OutputStreamWriter(new FileOutputStream(file), charset);
  }

  public Formatter(OutputStream out)
  {
    this.out = new OutputStreamWriter(out);
  }

  public Formatter(OutputStream out, String charset)
    throws UnsupportedEncodingException
  {
    this(out, charset, null);
  }

  public Formatter(OutputStream out, String charset, Locale loc)
    throws UnsupportedEncodingException
  {
    this.out = new OutputStreamWriter(out, charset);
    this.locale = loc;
  }

  public Formatter(PrintStream out)
  {
    this.out = out;
  }

  public Formatter(String file) throws FileNotFoundException
  {
    this.out = new OutputStreamWriter(new FileOutputStream(file));
  }

  public Formatter(String file, String charset)
    throws FileNotFoundException, UnsupportedEncodingException
  {
    this(file, charset, null);
  }

  public Formatter(String file, String charset, Locale loc)
    throws FileNotFoundException, UnsupportedEncodingException
  {
    this.out = new OutputStreamWriter(new FileOutputStream(file), charset);
    this.locale = loc;
  }

  public void close()
  {
    if (closed)
      return;
    try
      {
	if (out instanceof Closeable)
	  ((Closeable) out).close();
      }
    catch (IOException _)
      {
	// FIXME: do we ignore these or do we set ioException?
	// The docs seem to indicate that we should ignore.
      }
    closed = true;
  }

  public void flush()
  {
    if (closed)
      throw new FormatterClosedException();
    try
      {
	if (out instanceof Flushable)
	  ((Flushable) out).flush();
      }
    catch (IOException _)
      {
	// FIXME: do we ignore these or do we set ioException?
	// The docs seem to indicate that we should ignore.
      }
  }

  /**
   * Return the name corresponding to a flag.
   */
  private String getName(int flags)
  {
    // FIXME: do we want all the flags in here?
    // Or should we redo how this is reported?
    int bit = Integer.numberOfTrailingZeros(flags);
    return FLAGS.substring(bit, bit + 1);
  }

  /**
   * Verify the flags passed to a conversion.
   */
  private void checkFlags(int flags, int allowed, char conversion)
  {
    flags &= ~allowed;
    if (flags != 0)
      throw new FormatFlagsConversionMismatchException(getName(flags),
						       conversion);
  }

  /**
   * Throw an exception is a precision was specified.
   */
  private void noPrecision(int precision)
  {
    if (precision != -1)
      throw new IllegalFormatPrecisionException(precision);
  }

  /**
   * A helper method that handles emitting a String after applying
   * precision, width, justification, and upper case flags.
   */
  private void genericFormat(String arg, int flags, int width, int precision)
    throws IOException
  {
    if (precision >= 0 && arg.length() > precision)
      arg = arg.substring(0, precision);
    boolean left = (flags & FormattableFlags.LEFT_JUSTIFY) != 0;
    if (left && width == -1)
      throw new MissingFormatWidthException("fixme");
    if (! left && arg.length() < width)
      {
	for (int i = arg.length() - width; i >= 0; --i)
	  out.append(' ');
      }
    if ((flags & FormattableFlags.UPPERCASE) != 0)
      {
	if (fmtLocale == null)
	  arg = arg.toUpperCase();
	else
	  arg = arg.toUpperCase(fmtLocale);
      }
    out.append(arg);
    if (left && arg.length() < width)
      {
	for (int i = arg.length() - width; i >= 0; --i)
	  out.append(' ');
      }
  }

  /** Emit a boolean.  */
  private void booleanFormat(Object arg, int flags, int width, int precision,
			     char conversion)
    throws IOException
  {
    checkFlags(flags,
	       FormattableFlags.LEFT_JUSTIFY | FormattableFlags.UPPERCASE,
	       conversion);
    String result;
    if (arg instanceof Boolean)
      result = String.valueOf((Boolean) arg);
    else
      result = arg == null ? "false" : "true";
    genericFormat(result, flags, width, precision);
  }

  /** Emit a hash code.  */
  private void hashCodeFormat(Object arg, int flags, int width, int precision,
			      char conversion)
    throws IOException
  {
    checkFlags(flags,
	       FormattableFlags.LEFT_JUSTIFY | FormattableFlags.UPPERCASE,
	       conversion);
    genericFormat(arg == null ? "null" : Integer.toHexString(arg.hashCode()),
		  flags, width, precision);
  }

  /** Emit via a String or Formattable conversion.  */
  private void stringFormat(Object arg, int flags, int width, int precision,
			    char conversion)
    throws IOException
  {
    if (arg instanceof Formattable)
      {
	checkFlags(flags,
		   (FormattableFlags.LEFT_JUSTIFY
		    | FormattableFlags.UPPERCASE
		    | FormattableFlags.ALTERNATE),
		   conversion);
	Formattable fmt = (Formattable) arg;
	fmt.formatTo(this, flags, width, precision);
      }
    else
      {
	checkFlags(flags,
		   FormattableFlags.LEFT_JUSTIFY | FormattableFlags.UPPERCASE,
		   conversion);
	genericFormat(arg == null ? "null" : arg.toString(), flags, width,
		      precision);
      }
  }

  /** Emit a character value.  */
  private void characterFormat(Object arg, int flags, int width, int precision,
			       char conversion)
    throws IOException
  {
    checkFlags(flags,
	       FormattableFlags.LEFT_JUSTIFY | FormattableFlags.UPPERCASE,
	       conversion);
    noPrecision(precision);

    int theChar;
    if (arg instanceof Character)
      theChar = ((Character) arg).charValue();
    else if (arg instanceof Byte)
      theChar = (char) (((Byte) arg).byteValue ());
    else if (arg instanceof Short)
      theChar = (char) (((Short) arg).shortValue ());
    else if (arg instanceof Integer)
      {
	theChar = ((Integer) arg).intValue();
	if (! Character.isValidCodePoint(theChar))
	  throw new IllegalFormatCodePointException(theChar);
      }
    else
      throw new IllegalFormatConversionException(conversion, arg.getClass());
    String result = new String(Character.toChars(theChar));
    genericFormat(result, flags, width, precision);
  }

  /** Emit a '%'.  */
  private void percentFormat(int flags, int width, int precision)
    throws IOException
  {
    checkFlags(flags, FormattableFlags.LEFT_JUSTIFY, '%');
    noPrecision(precision);
    genericFormat("%", flags, width, precision);
  }

  /** Emit a newline.  */
  private void newLineFormat(int flags, int width, int precision)
    throws IOException
  {
    checkFlags(flags, 0, 'n');
    noPrecision(precision);
    if (width != -1)
      throw new IllegalFormatWidthException(width);
    genericFormat(lineSeparator, flags, width, precision);
  }

  /** Emit a hex or octal value.  */
  private void hexOrOctalConversion(Object arg, int flags, int width,
				    int precision, int radix,
				    char conversion)
    throws IOException
  {
    assert radix == 8 || radix == 16;
    noPrecision(precision);

    // Some error checking.
    if ((flags & FormattableFlags.ZERO) != 0
	&& (flags & FormattableFlags.LEFT_JUSTIFY) == 0)
      throw new IllegalFormatFlagsException(getName(flags));
    if ((flags & FormattableFlags.PLUS) != 0
	&& (flags & FormattableFlags.SPACE) == 0)
      throw new IllegalFormatFlagsException(getName(flags));

    if ((flags & FormattableFlags.LEFT_JUSTIFY) != 0 && width == -1)
      throw new MissingFormatWidthException("fixme");

    // Do the base translation of the value to a string.
    String result;
    if (arg instanceof BigInteger)
      {
	checkFlags(flags,
		   (FormattableFlags.LEFT_JUSTIFY
		    // We already handled any possible error when
		    // parsing.
		    | FormattableFlags.UPPERCASE
		    | FormattableFlags.ALTERNATE
		    | FormattableFlags.PLUS
		    | FormattableFlags.SPACE
		    | FormattableFlags.ZERO
		    | FormattableFlags.PAREN),
		   conversion);
	BigInteger bi = (BigInteger) arg;
	result = bi.toString(radix);
      }
    else if (arg instanceof Number
	     && ! (arg instanceof Float)
	     && ! (arg instanceof Double))
      {
	checkFlags(flags,
		   (FormattableFlags.LEFT_JUSTIFY
		    // We already handled any possible error when
		    // parsing.
		    | FormattableFlags.UPPERCASE
		    | FormattableFlags.ALTERNATE
		    | FormattableFlags.ZERO),
		   conversion);
	long value = ((Number) arg).longValue ();
	result = (radix == 8 ? Long.toOctalString(value)
		  : Long.toHexString(value));
      }
    else
      throw new IllegalFormatConversionException(conversion, arg.getClass());

    // We use a string builder to do further manipulations.
    StringBuilder builder = new StringBuilder(result);
    int insertPoint = 0;

    // Insert the sign.
    if (builder.charAt(0) == '-')
      {
	// Already inserted.  Note that we don't insert a sign, since
	// the only case where it is needed it BigInteger, and it has
	// already been inserted by toString.
	++insertPoint;
      }
    else if ((flags & FormattableFlags.PLUS) != 0)
      {
	builder.insert(insertPoint, '+');
	++insertPoint;
      }
    else if ((flags & FormattableFlags.SPACE) != 0)
      {
	builder.insert(insertPoint, ' ');
	++insertPoint;
      }

    // Insert the radix prefix.
    if ((flags & FormattableFlags.ALTERNATE) != 0)
      {
	builder.insert(insertPoint, radix == 8 ? "0" : "0x");
	insertPoint += radix == 8 ? 1 : 2;
      }

    // Now justify the result.
    int resultWidth = result.length();
    if (resultWidth < width)
      {
	char fill = ((flags & FormattableFlags.ZERO) != 0) ? '0' : ' ';
	if ((flags & FormattableFlags.LEFT_JUSTIFY) == 0)
	  {
	    // Right justify.
	    insertPoint = builder.length();
	  }
	else if (fill == ' ')
	  {
	    // Insert spaces before the radix prefix and sign.
	    insertPoint = 0;
	  }
	while (resultWidth++ < width)
	  builder.insert(insertPoint, fill);
      }

    result = builder.toString();
    if ((flags & FormattableFlags.UPPERCASE) != 0)
      {
	if (fmtLocale == null)
	  result = result.toUpperCase();
	else
	  result = result.toUpperCase(fmtLocale);
      }

    out.append(result);
  }


  /**
   * Advance the internal parsing index, and throw an exception
   * on overrun.
   */
  private void advance()
  {
    ++index;
    if (index >= length)
      {
	// FIXME: what exception here?
	throw new IllegalArgumentException();
      }
  }

  /**
   * Parse an integer appearing in the format string.  Will return -1
   * if no integer was found.
   */
  private int parseInt()
  {
    int start = index;
    while (Character.isDigit(format.charAt(index)))
      advance();
    if (start == index)
      return -1;
    return Integer.decode(format.substring(start, index));
  }

  /**
   * Parse the argument index.  Returns -1 if there was no index, 0 if
   * we should re-use the previous index, and a positive integer to
   * indicate an absolute index.
   */
  private int parseArgumentIndex()
  {
    int result = -1;
    int start = index;
    if (format.charAt(index) == '<')
      {
	result = 0;
	advance();
      }
    else if (Character.isDigit(format.charAt(index)))
      {
	result = parseInt();
	if (format.charAt(index) == '$')
	  advance();
	else
	  {
	    // Reset.
	    index = start;
	    result = -1;
	  }
      }
    return result;
  }

  /**
   * Parse a set of flags and return a bit mask of values from
   * FormattableFlags.  Will throw an exception if a flag is
   * duplicated.
   */
  private int parseFlags()
  {
    int value = 0;
    int start = index;
    while (true)
      {
	int x = FLAGS.indexOf(format.charAt(index));
	if (x == -1)
	  break;
	int newValue = 1 << x;
	if ((value & newValue) != 0)
	  throw new DuplicateFormatFlagsException(format.substring(start,
								   index + 1));
	value |= newValue;
	advance();
      }
    return value;
  }

  /**
   * Parse the width part of a format string.  Returns -1 if no width
   * was specified.
   */
  private int parseWidth()
  {
    return parseInt();
  }

  /**
   * If the current character is '.', parses the precision part of a
   * format string.  Returns -1 if no precision was specified.
   */
  private int parsePrecision()
  {
    if (format.charAt(index) != '.')
      return -1;
    advance();
    int precision = parseInt();
    if (precision == -1)
      // FIXME
      throw new IllegalArgumentException();
    return precision;
  }

  public Formatter format(Locale loc, String fmt, Object... args)
  {
    if (closed)
      throw new FormatterClosedException();

    // Note the arguments are indexed starting at 1.
    int implicitArgumentIndex = 1;
    int previousArgumentIndex = 0;

    try
      {
	format = fmt;
	length = format.length();
	for (index = 0; index < length; ++index)
	  {
	    char c = format.charAt(index);
	    if (c != '%')
	      {
		out.append(c);
		continue;
	      }

	    int start = index;
	    advance();

	    // We do the needed post-processing of this later, when we
	    // determine whether an argument is actually needed by
	    // this conversion.
	    int argumentIndex = parseArgumentIndex();

	    int flags = parseFlags();
	    int width = parseWidth();
	    int precision = parsePrecision();
	    char origConversion = format.charAt(index);
	    char conversion = origConversion;
	    if (Character.isUpperCase(conversion))
	      {
		flags |= FormattableFlags.UPPERCASE;
		conversion = Character.toLowerCase(conversion);
	      }

	    Object argument = null;
	    if (conversion == '%' || conversion == 'n')
	      {
		if (argumentIndex != -1)
		  {
		    // FIXME: not sure about this.
		    throw new UnknownFormatConversionException("FIXME");
		  }
	      }
	    else
	      {
		if (argumentIndex == -1)
		  argumentIndex = implicitArgumentIndex++;
		else if (argumentIndex == 0)
		  argumentIndex = previousArgumentIndex;
		// Argument indices start at 1 but array indices at 0.
		--argumentIndex;
		if (argumentIndex < 0 || argumentIndex >= args.length)
		  throw new MissingFormatArgumentException(format.substring(start, index));
		argument = args[argumentIndex];
	      }

	    switch (conversion)
	      {
	      case 'b':
		booleanFormat(argument, flags, width, precision,
			      origConversion);
		break;
	      case 'h':
		hashCodeFormat(argument, flags, width, precision,
			       origConversion);
		break;
	      case 's':
		stringFormat(argument, flags, width, precision,
			     origConversion);
		break;
	      case 'c':
		characterFormat(argument, flags, width, precision,
				origConversion);
		break;
	      case 'd':
		// decimalFormat();
		break;
	      case 'o':
		checkFlags(flags & FormattableFlags.UPPERCASE, 0, 'o');
		hexOrOctalConversion(argument, flags, width, precision, 8,
				     origConversion);
		break;
	      case 'x':
		hexOrOctalConversion(argument, flags, width, precision, 16,
				     origConversion);
	      case 'e':
		// scientificNotationConversion();
		break;
	      case 'f':
		// floatingDecimalConversion();
		break;
	      case 'g':
		// smartFloatingConversion();
		break;
	      case 'a':
		// hexFloatingConversion();
		break;
	      case 't':
// 		char format = parseDateTimeFormat();
// 		timeDateFormat();
		break;
	      case '%':
		percentFormat(flags, width, precision);
		break;
	      case 'n':
		newLineFormat(flags, width, precision);
		break;
	      default:
		throw new UnknownFormatConversionException(String.valueOf(origConversion));
	      }
	  }
      }
    catch (IOException exc)
      {
	ioException = exc;
      }
    return this;
  }

  public Formatter format(String format, Object... args)
  {
    return format(locale, format, args);
  }

  public IOException ioException()
  {
    return ioException;
  }

  public Locale locale()
  {
    if (closed)
      throw new FormatterClosedException();
    return locale;
  }

  public Appendable out()
  {
    if (closed)
      throw new FormatterClosedException();
    return out;
  }

  public String toString()
  {
    if (closed)
      throw new FormatterClosedException();
    return out.toString();
  }
}
