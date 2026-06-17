package net.createmod.catnip.data;

import java.util.regex.PatternSyntaxException;

public class Glob {
   private static final String REGEX_META_CHARS = ".^$+{[]|()";
   private static final String GLOB_META_CHARS = "\\*?[{";
   private static final char EOL = '\u0000';

   public static boolean isRegexMeta(char c) {
      return ".^$+{[]|()".indexOf(c) != -1;
   }

   public static boolean isGlobMeta(char c) {
      return "\\*?[{".indexOf(c) != -1;
   }

   private static char next(String glob, int i) {
      return i < glob.length() ? glob.charAt(i) : '\u0000';
   }

   public static String toRegexPattern(String globPattern) throws PatternSyntaxException {
      boolean inGroup = false;
      StringBuilder regex = new StringBuilder("^");
      int i = 0;
      boolean isNegativeLookaround = false;
      boolean isAnchored = true;

      label152:
      while (i < globPattern.length()) {
         char c = globPattern.charAt(i++);
         switch (c) {
            case '*':
               regex.append(".*");
               if (!inGroup) {
                  isAnchored = false;
               }
               break;
            case ',':
               if (inGroup) {
                  regex.append("|");
               } else {
                  regex.append(',');
                  isAnchored = true;
               }
               break;
            case '?':
               regex.append(".");
               if (!inGroup) {
                  isAnchored = true;
               }
               break;
            case '[':
               if (next(globPattern, i) == ']' || next(globPattern, i) == '!' && next(globPattern, i + 1) == ']') {
                  throw new PatternSyntaxException("Cannot have set with no entries", globPattern, i);
               }

               regex.append("[");
               if (next(globPattern, i) == '^') {
                  regex.append("\\^");
                  i++;
               } else {
                  if (next(globPattern, i) == '!') {
                     regex.append('^');
                     i++;
                  }

                  if (next(globPattern, i) == '-') {
                     regex.append('-');
                     i++;
                  }
               }

               boolean hasRangeStart = false;
               char last = 0;

               while (true) {
                  if (i < globPattern.length()) {
                     c = globPattern.charAt(i++);
                     if (c != ']') {
                        if (c == '\\') {
                           if (i == globPattern.length()) {
                              throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
                           }

                           if (next(globPattern, i) == ']' || next(globPattern, i) == '-' || next(globPattern, i) == '\\') {
                              regex.append('\\');
                           }

                           regex.append(next(globPattern, i++));
                           continue;
                        }

                        regex.append(c);
                        if (c != '-') {
                           hasRangeStart = true;
                           last = c;
                           continue;
                        }

                        if (!hasRangeStart) {
                           throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
                        }

                        if ((c = next(globPattern, i++)) != 0 && c != ']') {
                           if (c < last) {
                              throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
                           }

                           regex.append(c);
                           hasRangeStart = false;
                           continue;
                        }
                     }
                  }

                  if (c != ']') {
                     throw new PatternSyntaxException("Missing ']'", globPattern, i - 1);
                  }

                  regex.append("]");
                  if (!inGroup) {
                     isAnchored = true;
                  }
                  continue label152;
               }
            case '\\':
               if (i == globPattern.length()) {
                  throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
               }

               char next = globPattern.charAt(i++);
               if (isGlobMeta(next) || isRegexMeta(next)) {
                  regex.append('\\');
               }

               regex.append(next);
               if (!inGroup) {
                  isAnchored = true;
               }
               break;
            case '{':
               if (inGroup) {
                  throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
               }

               regex.append("(?");
               if (next(globPattern, i) == '!') {
                  isNegativeLookaround = true;
                  if (!isAnchored) {
                     regex.append('<');
                  }

                  regex.append('!');
                  i++;
               } else {
                  isNegativeLookaround = false;
                  regex.append(":");
               }

               inGroup = true;
               break;
            case '}':
               if (inGroup) {
                  regex.append(")");
                  if (isAnchored && isNegativeLookaround) {
                     regex.append(".*");
                     isAnchored = false;
                  }

                  inGroup = false;
               } else {
                  regex.append('}');
                  isAnchored = true;
               }
               break;
            default:
               if (isRegexMeta(c)) {
                  regex.append('\\');
               }

               regex.append(c);
               if (!inGroup) {
                  isAnchored = true;
               }
         }
      }

      if (inGroup) {
         throw new PatternSyntaxException("Missing '}'", globPattern, i - 1);
      } else {
         return regex.append('$').toString();
      }
   }

   public static String toRegexPattern(String globPattern, String defaultPatternIfError) {
      try {
         return toRegexPattern(globPattern);
      } catch (PatternSyntaxException var3) {
         return defaultPatternIfError;
      }
   }
}
