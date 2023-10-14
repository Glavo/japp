/**
 * This package provides a glob style pattern matcher for strings.  Globs are often used in Unix command lines and
 * in the SQL like matcher.
 *
 * <pre>
 *     import com.hrakaroo.glob.*;
 *
 *     // By default this will use '*' as the match-zero-or-more character, and '?' as the match-exactly-one character.
 *     MatchingEngine m = GlobPattern.compile("*.txt");
 *     for (String s : new String[]{"file.txt", "file.png"}) {
 *         if (m.matches(s)) {
 *             System.out.println("This is a text file: " + s);
 *         }
 *     }
 * </pre>
 *
 *  @author Joshua Gerth
 */

package org.glavo.japp.thirdparty.glob;