package org.mbachran.server.custom.util;

/**
 * Constant Strings that are used as delimiter for parsing or construction.
 * Might be extended with growing features of parsers and builders. Its use is optional.
 */
public interface Delimiters
{
    String CR_LF = "\r\n";

    String EMPTY_LINE = CR_LF + CR_LF;

    String COLON = ":";

    String SEMI_COLON = ";";

    String SP = " ";

    String HT = "\t";

    byte CR = 13;

    byte LF = 10;
}
