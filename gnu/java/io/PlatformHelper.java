/* PlatformHelper.java -- Isolate OS-specific IO helper methods and variables
   Copyright (C) 1998, 2002, 2006, 2010 Free Software Foundation, Inc.

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

package gnu.java.io;

import gnu.classpath.SystemProperties;

/**
 * We had many changes in File.java, URLStreamHandler.java etc. to handle
 * path representations on different platforms (Windows/Unix-family).
 * Finally we'd like to collect all these ad hoc codes into this utility class.
 *       --Gansha
 */
public final class PlatformHelper
{

  private static final String separator
    = SystemProperties.getProperty("file.separator");

  private static final char separatorChar = separator.charAt(0);

  /**
   * On most platforms 260 is equal or greater than a max path value,
   * so we can set the initial buffer size of StringBuffer to half of this value
   * to improve performance.
   */
  public static final int INITIAL_MAX_PATH = 260/2;

  private PlatformHelper() {} // Prohibits instantiation.

  /**
   * This routine checks the input param "path" whether it begins with root
   * path prefix. Note that even if a path begins with a root prefix, it may
   * denote a relative path on some platforms (e.g., Windows). On the other
   * hand, a root prefix may denote an absolute path's beginning but itself
   * does not end with a file separator character.
   *
   * @return the length of root path's prefix (1 for Unix-family platforms
   * and 1..3 (or more) for Windows platforms), or 0 if missing
   */
  public static final int beginWithRootPathPrefix(String path)
  {
    int len = path.length();
    if (len == 0)
      return 0;

    if (separatorChar != '\\')
      {
        // Handle UNIX path.
        return path.charAt(0) == separatorChar ? 1 : 0;
      }

    int pos = 0; // length of the prefix
    // Handle Windows path.
    if (path.charAt(0) == '\\')
      {
        pos = 1;
        if (len != 1 && path.charAt(1) == '\\')
          {
            // Handle UNC path.
            while (++pos < len)
              if (path.charAt(pos) == '\\')
                break;
            if (pos < len)
              {
                char ch = '\0';
                while (++pos < len)
                  if ((ch = path.charAt(pos)) == '\\' || ch == ':')
                    break;
                if (pos < len)
                  { // matches "\\server\share\" or "\\nwserver\volume:"
                    pos++;
                    if (ch == ':' && pos < len && path.charAt(pos) == '\\')
                      pos++; // matches "\\nwserver\volume:\"
                  }
              }
          }
      }
    else
      {
        pos = path.indexOf(':', 0) + 1;
        if (pos > 1)
          {
            // Handle path starting with a drive prefix.
            int i;
            if (path.lastIndexOf('.', pos - 2) >= 0
                || ((i = path.lastIndexOf('\\', pos - 2)) > 0
                    && path.lastIndexOf('\\', i - 1) >= 0))
              { // matches "file.ext:ntstream" or "path\dir\file:ntstream"
                pos = 0;
              }
            else
              {
                if (pos < len)
                  {
                    if (path.charAt(pos) == '\\')
                      pos++; // matches "d:\", "volume:\", "nwserver\volume:\"
                    else
                      {
                        // Handle "Alternate data stream" name suffix.
                        if (pos > 2 && path.indexOf(':', pos) < 0
                            && path.indexOf('\\', pos + 1) < 0)
                          pos = 0; // matches "file:ntstream"
                      }
                  }
              }
          }
      }
    return pos;
  }

  /**
   * This routine checks the input param "path" whether it's root directory.
   * For Unix-family platforms, the root directory is "/".
   * For Windows platforms, the root directory is "\", "d:", "d:\" or
   * "\\server\share\". For Novel NetWare clients, the root directory also
   * could be in the format of "volume:", "volume:\", "nwserver\volume:",
   * "nwserver\volume:\", "\\nwserver\volume:" or "\\nwserver\volume:\".
   */
  public static final boolean isRootDirectory(String path)
  {
    int len = path.length();
    return len > 0 && beginWithRootPathPrefix(path) == len;
  }

  /**
   * This routine checks whether input param "path" ends with separator
   */
  public static final boolean endWithSeparator(String path)
  {
    return path.endsWith(separator) || path.endsWith("/");
  }

  /**
   * This routine removes from input param "path" the tail separator if it exists,
   * and return the remain part.
   */
  public static final String removeTailSeparator(String path)
  {
    if (endWithSeparator(path) && !isRootDirectory(path))
      return path.substring(0, path.length() - 1);

    return path;
  }

  /**
   * This routine returns last index of separator in input param "path",
   * and return it.
   */
  public static final int lastIndexOfSeparator(String path)
  {
    int pos = path.lastIndexOf(separatorChar);
    if (separatorChar != '/' && pos < path.length() - 1)
      {
        int pos2 = path.lastIndexOf('/');
        if (pos <= pos2)
          pos = pos2;
      }
    return pos;
  }

}
