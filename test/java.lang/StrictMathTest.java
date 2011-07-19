/* StrictMathTest.java -- tests StrictMath class
   Copyright (C) 2010 Free Software Foundation, Inc.

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


/**
 * @author Ivan Maidanski
 */
public class StrictMathTest {

  public static void main(String[] args) {
    acosTest();
    powTest(); // 2 tests
    IEEEremainderTest(); // 2 tests
    tanTest(); // 2 tests
    sinTest(); // 2 tests
    printStatus("StrictMath");
  }

  private static int passed;
  private static int failed;

  static void printStatus(String className) {
    if (failed != 0) {
      System.out.println(className + " test failures statistic (" + failed
                         + " fails and " + passed + " passes).");
    } else {
      System.out.println("PASSED: [" + className + "] All " + passed
                         + " tests.");
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

  static void assertEquals(long a, long b, String testName) {
    assertTrue(a == b, testName);
  }

  static void assertEquals(double a, double b, double prec, String testName) {
    assertTrue(Math.abs(a - b) <= prec, testName);
  }

  static void assertTaskNoTimeout(Runnable task, long timeoutMillis,
                                  String testName) {
    Thread thread = new Thread(task, "Task " + testName);
    thread.setDaemon(true);
    thread.start();
    try {
      thread.join(timeoutMillis);
    } catch (InterruptedException e) {
        // Should not happen.
        assertTrue(false, testName);
    }
    assertTrue(!thread.isAlive(), testName);
  }

  public static void acosTest() {
    assertEquals(2.2142974, StrictMath.acos(-0.6), 1e-5, "acos");
  }

  public static void powTest() {
    assertEquals(0, Double.doubleToRawLongBits(StrictMath.pow(-0.0, 0.5)),
                 "pow");
    assertEquals(~(-1L >>> 1),
                 Double.doubleToRawLongBits(StrictMath.pow(-0.0, 3)),
                 "pow #2");
  }

  public static void IEEEremainderTest() {
    assertEquals(~(-1L >>> 1),
                 Double.doubleToRawLongBits(
                                StrictMath.IEEEremainder(-0.0, 1)),
                 "IEEEremainder");
    assertEquals(-1.7999999, StrictMath.IEEEremainder(2, 3.8), 1E-5,
                 "IEEEremainder #2");
  }

  public static void sinTest() {
    assertTaskNoTimeout(new Runnable() {

      public void run() {
        assertEquals(0.6402884, StrictMath.sin(1.9e209), 1e-5, "sin #2");
      }
    }, 1000, "sin");
  }

  public static void tanTest() {
    assertEquals(-0.0290081, StrictMath.tan(-0.029), 1e-5, "tan");
    assertEquals(34.2325327, StrictMath.tan(-1.6), 1e-5, "tan #2");
  }
}
