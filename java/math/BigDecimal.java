/* java.math.BigDecimal -- Arbitrary precision decimals.
   Copyright (C) 1999, 2000, 2001, 2003, 2005 Free Software Foundation, Inc.

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

package java.math;

public class BigDecimal extends Number implements Comparable<BigDecimal>
{
  private BigInteger intVal;
  private int scale;
  private MathContext mathContext;
  private int precision = 0;
  private static final long serialVersionUID = 6108874887143696463L;

  /**
   * The constant zero as a BigDecimal with scale zero.
   * @since 1.5
   */
  public static final BigDecimal ZERO = 
    new BigDecimal (BigInteger.valueOf (0), 0);

  /**
   * The constant one as a BigDecimal with scale zero.
   * @since 1.5
   */
  public static final BigDecimal ONE = 
    new BigDecimal (BigInteger.valueOf (1), 0);

  /**
   * The constant ten as a BigDecimal with scale zero.
   * @since 1.5
   */
  public static final BigDecimal TEN = 
    new BigDecimal (BigInteger.valueOf (10), 0);

  public static final int ROUND_UP = 0;
  public static final int ROUND_DOWN = 1;
  public static final int ROUND_CEILING = 2;
  public static final int ROUND_FLOOR = 3;
  public static final int ROUND_HALF_UP = 4;
  public static final int ROUND_HALF_DOWN = 5;
  public static final int ROUND_HALF_EVEN = 6;
  public static final int ROUND_UNNECESSARY = 7;

  /**
   * Constructs a new BigDecimal whose unscaled value is val and whose
   * scale is zero.
   * @param val the value of the new BigDecimal
   * @since 1.5
   */
  public BigDecimal (int val)
  {
    this.intVal = BigInteger.valueOf(val);
    this.scale = 0;
  }
  
  /**
   * Constructs a new BigDecimal whose unscaled value is val and whose
   * scale is zero.
   * @param val the value of the new BigDecimal
   */
  public BigDecimal (long val)
  {
    this.intVal = BigInteger.valueOf(val);
    this.scale = 0;
  }
  
  /**
   * Constructs a BigDecimal whose unscaled value is num and whose
   * scale is zero.
   * @param num the value of the new BigDecimal
   */
  public BigDecimal (BigInteger num) 
  {
    this (num, 0);
  }

  /**
   * Constructs a BigDecimal whose unscaled value is num and whose
   * scale is scale.
   * @param num
   * @param scale
   */
  public BigDecimal (BigInteger num, int scale)
  {
    this.intVal = num;
    this.scale = scale;
  }

  public BigDecimal (double num) throws NumberFormatException 
  {
    if (Double.isInfinite (num) || Double.isNaN (num))
      throw new NumberFormatException ("invalid argument: " + num);
    // Note we can't convert NUM to a String and then use the
    // String-based constructor.  The BigDecimal documentation makes
    // it clear that the two constructors work differently.

    final int mantissaBits = 52;
    final int exponentBits = 11;
    final long mantMask = (1L << mantissaBits) - 1;
    final long expMask = (1L << exponentBits) - 1;

    long bits = Double.doubleToLongBits (num);
    long mantissa = bits & mantMask;
    long exponent = (bits >>> mantissaBits) & expMask;
    boolean denormal = exponent == 0;
    // Correct the exponent for the bias.
    exponent -= denormal ? 1022 : 1023;
    // Now correct the exponent to account for the bits to the right
    // of the decimal.
    exponent -= mantissaBits;
    // Ordinary numbers have an implied leading `1' bit.
    if (! denormal)
      mantissa |= (1L << mantissaBits);

    // Shave off factors of 10.
    while (exponent < 0 && (mantissa & 1) == 0)
      {
	++exponent;
	mantissa >>= 1;
      }

    intVal = BigInteger.valueOf (bits < 0 ? - mantissa : mantissa);
    if (exponent < 0)
      {
	// We have MANTISSA * 2 ^ (EXPONENT).
	// Since (1/2)^N == 5^N * 10^-N we can easily convert this
	// into a power of 10.
	scale = (int) (- exponent);
	BigInteger mult = BigInteger.valueOf (5).pow (scale);
	intVal = intVal.multiply (mult);
      }
    else
      {
	intVal = intVal.shiftLeft ((int) exponent);
	scale = 0;
      }
  }

  public BigDecimal (String num) throws NumberFormatException 
  {
    int len = num.length();
    int start = 0, point = 0;
    int dot = -1;
    boolean negative = false;
    if (num.charAt(0) == '+')
      {
	++start;
	++point;
      }
    else if (num.charAt(0) == '-')
      {
	++start;
	++point;
	negative = true;
      }

    while (point < len)
      {
	char c = num.charAt (point);
	if (c == '.')
	  {
	    if (dot >= 0)
	      throw new NumberFormatException ("multiple `.'s in number");
	    dot = point;
	  }
	else if (c == 'e' || c == 'E')
	  break;
	else if (Character.digit (c, 10) < 0)
	  throw new NumberFormatException ("unrecognized character: " + c);
	++point;
      }

    String val;
    if (dot >= 0)
      {
	val = num.substring (start, dot) + num.substring (dot + 1, point);
	scale = point - 1 - dot;
      }
    else
      {
	val = num.substring (start, point);
	scale = 0;
      }
    if (val.length () == 0)
      throw new NumberFormatException ("no digits seen");

    if (negative)
      val = "-" + val;
    intVal = new BigInteger (val);

    // Now parse exponent.
    if (point < len)
      {
        point++;
        if (num.charAt(point) == '+')
          point++;

        if (point >= len )
          throw new NumberFormatException ("no exponent following e or E");
	
        try 
	  {
	    int exp = Integer.parseInt (num.substring (point));
	    exp -= scale;
	    if (signum () == 0)
	      scale = 0;
	    else if (exp > 0)
	      {
		intVal = intVal.multiply (BigInteger.valueOf (10).pow (exp));
		scale = 0;
	      }
	    else
	      scale = - exp;
	  }
        catch (NumberFormatException ex) 
	  {
	    throw new NumberFormatException ("malformed exponent");
	  }
      }
  }

  public static BigDecimal valueOf (long val) 
  {
    return valueOf (val, 0);
  }

  public static BigDecimal valueOf (long val, int scale) 
    throws NumberFormatException 
  {
    if ((scale == 0) && ((int)val == val))
      switch ((int) val)
	{
	case 0:
	  return ZERO;
	case 1:
	  return ONE;
	}

    return new BigDecimal (BigInteger.valueOf (val), scale);
  }

  public BigDecimal add (BigDecimal val) 
  {
    // For addition, need to line up decimals.  Note that the movePointRight
    // method cannot be used for this as it might return a BigDecimal with
    // scale == 0 instead of the scale we need.
    BigInteger op1 = intVal;
    BigInteger op2 = val.intVal;
    if (scale < val.scale)
      op1 = op1.multiply (BigInteger.valueOf (10).pow (val.scale - scale));
    else if (scale > val.scale)
      op2 = op2.multiply (BigInteger.valueOf (10).pow (scale - val.scale));

    return new BigDecimal (op1.add (op2), Math.max (scale, val.scale));
  }

  public BigDecimal subtract (BigDecimal val) 
  {
    return this.add(val.negate());
  }

  public BigDecimal multiply (BigDecimal val) 
  {
    return new BigDecimal (intVal.multiply (val.intVal), scale + val.scale);
  }

  public BigDecimal divide (BigDecimal val, int roundingMode) 
    throws ArithmeticException, IllegalArgumentException 
  {
    return divide (val, scale, roundingMode);
  }

  public BigDecimal divide(BigDecimal val, int newScale, int roundingMode)
    throws ArithmeticException, IllegalArgumentException 
  {
    if (roundingMode < 0 || roundingMode > 7)
      throw 
	new IllegalArgumentException("illegal rounding mode: " + roundingMode);

    if (newScale < 0)
      throw new ArithmeticException ("scale is negative: " + newScale);

    if (intVal.signum () == 0)	// handle special case of 0.0/0.0
      return newScale == 0 ? ZERO : new BigDecimal (ZERO.intVal, newScale);
    
    // Ensure that pow gets a non-negative value.
    BigInteger valIntVal = val.intVal;
    int power = newScale - (scale - val.scale);
    if (power < 0)
      {
	// Effectively increase the scale of val to avoid an
	// ArithmeticException for a negative power.
        valIntVal = valIntVal.multiply (BigInteger.valueOf (10).pow (-power));
	power = 0;
      }

    BigInteger dividend = intVal.multiply (BigInteger.valueOf (10).pow (power));
    
    BigInteger parts[] = dividend.divideAndRemainder (valIntVal);

    BigInteger unrounded = parts[0];
    if (parts[1].signum () == 0) // no remainder, no rounding necessary
      return new BigDecimal (unrounded, newScale);

    if (roundingMode == ROUND_UNNECESSARY)
      throw new ArithmeticException ("newScale is not large enough");

    int sign = intVal.signum () * valIntVal.signum ();

    if (roundingMode == ROUND_CEILING)
      roundingMode = (sign > 0) ? ROUND_UP : ROUND_DOWN;
    else if (roundingMode == ROUND_FLOOR)
      roundingMode = (sign < 0) ? ROUND_UP : ROUND_DOWN;
    else
      {
	// half is -1 if remainder*2 < positive intValue (*power), 0 if equal,
	// 1 if >. This implies that the remainder to round is less than,
	// equal to, or greater than half way to the next digit.
	BigInteger posRemainder
	  = parts[1].signum () < 0 ? parts[1].negate() : parts[1];
	valIntVal = valIntVal.signum () < 0 ? valIntVal.negate () : valIntVal;
	int half = posRemainder.shiftLeft(1).compareTo(valIntVal);

	switch(roundingMode)
	  {
	  case ROUND_HALF_UP:
	    roundingMode = (half < 0) ? ROUND_DOWN : ROUND_UP;
	    break;
	  case ROUND_HALF_DOWN:
	    roundingMode = (half > 0) ? ROUND_UP : ROUND_DOWN;
	    break;
	  case ROUND_HALF_EVEN:
	    if (half < 0)
	      roundingMode = ROUND_DOWN;
	    else if (half > 0)
	      roundingMode = ROUND_UP;
	    else if (unrounded.testBit(0)) // odd, then ROUND_HALF_UP
	      roundingMode = ROUND_UP;
	    else                           // even, ROUND_HALF_DOWN
	      roundingMode = ROUND_DOWN;
	    break;
	  }
      }

    if (roundingMode == ROUND_UP)
      unrounded = unrounded.add (BigInteger.valueOf (sign > 0 ? 1 : -1));

    // roundingMode == ROUND_DOWN
    return new BigDecimal (unrounded, newScale);
  }
    
  public int compareTo (BigDecimal val) 
  {
    if (scale == val.scale)
      return intVal.compareTo (val.intVal);

    BigInteger thisParts[] = 
      intVal.divideAndRemainder (BigInteger.valueOf (10).pow (scale));
    BigInteger valParts[] =
      val.intVal.divideAndRemainder (BigInteger.valueOf (10).pow (val.scale));
    
    int compare;
    if ((compare = thisParts[0].compareTo (valParts[0])) != 0)
      return compare;

    // quotients are the same, so compare remainders

    // remove trailing zeros
    if (thisParts[1].equals (BigInteger.valueOf (0)) == false)
      while (thisParts[1].mod (BigInteger.valueOf (10)).equals
	     (BigInteger.valueOf (0)))
      thisParts[1] = thisParts[1].divide (BigInteger.valueOf (10));
    // again...
    if (valParts[1].equals(BigInteger.valueOf (0)) == false)
      while (valParts[1].mod (BigInteger.valueOf (10)).equals
	     (BigInteger.valueOf (0)))
	valParts[1] = valParts[1].divide (BigInteger.valueOf (10));

    // and compare them
    return thisParts[1].compareTo (valParts[1]);
  }

  public boolean equals (Object o) 
  {
    return (o instanceof BigDecimal 
	    && scale == ((BigDecimal) o).scale
	    && compareTo ((BigDecimal) o) == 0);
  }

  public int hashCode() 
  {
    return intValue() ^ scale;
  }

  public BigDecimal max (BigDecimal val)
  {
    switch (compareTo (val)) 
      {
      case 1:
	return this;
      default:
	return val;
      }
  }

  public BigDecimal min (BigDecimal val) 
  {
    switch (compareTo (val)) 
      {
      case -1:
	return this;
      default:
	return val;
      }
  }

  public BigDecimal movePointLeft (int n)
  {
    return (n < 0) ? movePointRight (-n) : new BigDecimal (intVal, scale + n);
  }

  public BigDecimal movePointRight (int n)
  {
    if (n < 0)
      return movePointLeft (-n);

    if (scale >= n)
      return new BigDecimal (intVal, scale - n);

    return new BigDecimal (intVal.multiply 
			   (BigInteger.valueOf (10).pow (n - scale)), 0);
  }

  public int signum () 
  {
    return intVal.signum ();
  }

  public int scale () 
  {
    return scale;
  }
  
  public BigInteger unscaledValue()
  {
    return intVal;
  }

  public BigDecimal abs () 
  {
    return new BigDecimal (intVal.abs (), scale);
  }

  public BigDecimal negate () 
  {
    return new BigDecimal (intVal.negate (), scale);
  }
  
  /**
   * Returns this BigDecimal.  This is included for symmetry with the 
   * method negate().
   * @return this
   * @since 1.5
   */
  public BigDecimal plus()
  {
    return this;
  }
  
  /**
   * Returns a BigDecimal which is this BigDecimal rounded according to the
   * MathContext rounding settings.
   * @param mc the MathContext that tells us how to round
   * @return the rounded BigDecimal
   */
  public BigDecimal round(MathContext mc)
  {
    int mcPrecision = mc.getPrecision();
    int numToChop = precision - mcPrecision;
    // If mc specifies not to chop any digits or if we've already chopped 
    // enough digits (say by using a MathContext in the constructor for this
    // BigDecimal) then just return this.
    if (mcPrecision == 0 || numToChop <= 0)
      return this;
    
    // Make a new BigDecimal which is the correct power of 10 to chop off
    // the required number of digits and then call divide.
    BigDecimal div = 
      new BigDecimal(BigInteger.valueOf((long)Math.pow(10, numToChop)));
    BigDecimal rounded = divide(div, scale, mc.getRoundingMode().ordinal());
    rounded.scale -= numToChop;
    return rounded;
  }
  
  /**
   * Returns the precision of this BigDecimal (the number of digits in the
   * unscaled value).  The precision of a zero value is 1.
   * @return the number of digits in the unscaled value, or 1 if the value 
   * is zero.
   */
  public int precision()
  {
    if (precision == 0)
      precision = numDigitsInLong(intVal.longValue());
    return precision;
  }

  /**
   * This method determines the number of digits in the long value l. 
   * @param l the long value
   * @return the number of digits in l
   */
  private static int numDigitsInLong(long l)
  {
    // We divide up the range in a binary fashion, this first if
    // takes care of numbers with 1 to 9 digits.
    if (l < 1000000000L)
    {
      // This if is for numbers with 1 to 5 digits.
      if (l < 100000L)
        {
          if (l < 100L)
            return (l < 10L) ? 1 : 2;
          if (l < 10000L)
            return (l < 1000L) ? 3 : 4;
          return 5;
        }
      // Here we handle numbers with 6 to 9 digits.
      if (l < 10000000L)
        return (l < 1000000L) ? 6 : 7;
      return (l < 100000000L) ? 8 : 9;
    }
    // If we are at this point that means we didn't enter the loop for
    // numbers with 1 to 9 digits, so our number has 10 to 19 digits. 
    // This first if handles numbers with 10 to 14 digits.
    if (l < 100000000000000L)
      {
        // This handles numbers with 10 to 12 digits.
        if (l < 1000000000000L)
          {
            if (l < 100000000000L)
              return (l < 10000000000L) ? 10 : 11;
            return 12;
          }
        // This handles numbers with 13 or 14 digits.
        return (l < 10000000000000L) ? 13 : 14;
      }
    // Finally we handle numbers with 15 to 19 digits.
    if (l < 100000000000000000L)
      {
        // 15 to 17 digits.
        if (l < 1000000000000000L)
          return 15;
        return (l < 10000000000000000L) ? 16 : 17;
      }
    // 18 or 19 digits.
    return (l < 1000000000000000000L) ? 18 : 19;
  }
  
  public String toString () 
  {
    String bigStr = intVal.toString();
    if (scale == 0) 
      return bigStr;

    boolean negative = (bigStr.charAt(0) == '-');

    int point = bigStr.length() - scale - (negative ? 1 : 0);

    StringBuffer sb = new StringBuffer(bigStr.length() + 2 +
				       (point <= 0 ? (-point + 1) : 0));
    if (point <= 0)
      {
        if (negative)
          sb.append('-');
        sb.append('0').append('.');
        while (point < 0)
          {
            sb.append('0');
            point++;
          }
        sb.append(bigStr.substring(negative ? 1 : 0));
      }
    else
      {
	sb.append(bigStr);
	sb.insert(point + (negative ? 1 : 0), '.');
      }
    return sb.toString();
  }

  public BigInteger toBigInteger () 
  {
    return scale == 0 ? intVal :
      intVal.divide (BigInteger.valueOf (10).pow (scale));
  }

  public int intValue () 
  {
    return toBigInteger ().intValue ();
  }

  public long longValue ()
  {
    return toBigInteger().longValue();
  }

  public float floatValue() 
  {
    return Float.valueOf(toString()).floatValue();
  }

  public double doubleValue() 
  {
    return Double.valueOf(toString()).doubleValue();
  }

  public BigDecimal setScale (int scale) throws ArithmeticException
  {
    return setScale (scale, ROUND_UNNECESSARY);
  }

  public BigDecimal setScale (int scale, int roundingMode)
    throws ArithmeticException, IllegalArgumentException
  {
    return divide (ONE, scale, roundingMode);
  }
  
  /**
   * Returns a new BigDecimal constructed from the BigDecimal(String) 
   * constructor using the Double.toString(double) method to obtain
   * the String.
   * @param val the double value used in Double.toString(double)
   * @return a BigDecimal representation of val
   * @throws NumberFormatException if val is NaN or infinite
   * @since 1.5
   */
  public static BigDecimal valueOf(double val)
  {
    if (Double.isInfinite(val) || Double.isNaN(val))
      throw new NumberFormatException("argument cannot be NaN or infinite.");
    return new BigDecimal(Double.toString(val));
  }
}
