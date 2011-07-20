/* SimpleDateFormatTest.java -- tests java.text.SimpleDateFormat class
   Copyright (C) 2011 Free Software Foundation, Inc.

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * @author Ivan Maidanski
 */
public class SimpleDateFormatTest {

  public static void parseAtZeroGmtTest() {
    String input = "20091105234352GMT+00:00";
    SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz");
    TimeZone tz = new SimpleTimeZone(0, "Z");
    dateF.setTimeZone(tz);
    Date date;
    try {
      date = dateF.parse(input);
    } catch (ParseException e) {
      failed++;
      System.err.println(e);
      return;
    }
    Calendar calendar = new GregorianCalendar(tz);
    calendar.setTime(date);
    assertTrue(calendar.get(Calendar.YEAR) == 2009
                 && calendar.get(Calendar.MONTH) == 11 - 1
                 && calendar.get(Calendar.DATE) == 5
                 && calendar.get(Calendar.HOUR_OF_DAY) == 23
                 && calendar.get(Calendar.MINUTE) == 43
                 && calendar.get(Calendar.SECOND) == 52,
               "parse(" + input + ")=" + date
                 + ", year=" + calendar.get(Calendar.YEAR)
                 + ", month=" + calendar.get(Calendar.MONTH)
                 + ", date=" + calendar.get(Calendar.DATE)
                 + ", hour=" + calendar.get(Calendar.HOUR_OF_DAY)
                 + ", minute=" + calendar.get(Calendar.MINUTE)
                 + ", second=" + calendar.get(Calendar.SECOND));
  }

  public static void main(String[] args) {
    parseAtZeroGmtTest();
    printStatus("java.text.SimpleDateFormat");
  }

  private static int passed;
  private static int failed;

  static void printStatus(String className) {
    if (failed != 0) {
      System.out.println("FAILED: [" + className + "] " + failed
                         + " tests failed and " + passed + " passed.");
    } else {
      System.out.println("PASSED: [" + className + "] All " + passed
                         + " tests passed.");
    }
  }

  static void assertTrue(boolean cond, String testName) {
    if (cond) {
      passed++;
    } else {
      failed++;
      System.out.println("FAILED " + testName);
    }
  }
}
