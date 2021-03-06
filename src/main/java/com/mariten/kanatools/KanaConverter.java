package com.mariten.kanatools;
import com.mariten.kanatools.KanaAppraiser;

import java.util.HashMap;
import java.util.Map;

/**
  * Provides easy, automatic string conversions often necessary when dealing with Japanese text
  *
  * Port of PHP's "mb_convert_kana" function for Java.
  * http://www.php.net/manual/en/function.mb-convert-kana.php
  */
public class KanaConverter
{
    // Conversion Operations Types
    //// Matched numeric values to originals in PHP's source code
    //// https://github.com/php/php-src/blob/a84e5dc37dc0ff8c313164d9db141d3d9f2b2730/ext/mbstring/mbstring.c#L3434
    public static final int OP_HAN_ASCII_TO_ZEN_ASCII      = 0x00000001;
    public static final int OP_HAN_LETTER_TO_ZEN_LETTER    = 0x00000002;
    public static final int OP_HAN_NUMBER_TO_ZEN_NUMBER    = 0x00000004;
    public static final int OP_HAN_SPACE_TO_ZEN_SPACE      = 0x00000008;
    public static final int OP_HAN_KATA_TO_ZEN_KATA        = 0x00000100;
    public static final int OP_HAN_KATA_TO_ZEN_HIRA        = 0x00000200;
    public static final int OP_KEEP_DIACRITIC_MARKS_APART  = 0x00100000;
    public static final int OP_ZEN_ASCII_TO_HAN_ASCII      = 0x00000010;
    public static final int OP_ZEN_LETTER_TO_HAN_LETTER    = 0x00000020;
    public static final int OP_ZEN_NUMBER_TO_HAN_NUMBER    = 0x00000040;
    public static final int OP_ZEN_SPACE_TO_HAN_SPACE      = 0x00000080;
    public static final int OP_ZEN_KATA_TO_HAN_KATA        = 0x00001000;
    public static final int OP_ZEN_HIRA_TO_HAN_KATA        = 0x00002000;
    public static final int OP_ZEN_HIRA_TO_ZEN_KATA        = 0x00010000;
    public static final int OP_ZEN_KATA_TO_ZEN_HIRA        = 0x00020000;

    //// Maintain backwards compatibility (based on mb_convert_kana's "$option" parameter from PHP)
    //// Details: http://php.net/manual/en/function.mb-convert-kana.php
    public static final Map<Character, Integer> LETTER_OP_CODE_LOOKUP;
    static {
        LETTER_OP_CODE_LOOKUP = new HashMap<Character, Integer>();
        LETTER_OP_CODE_LOOKUP.put('A', OP_HAN_ASCII_TO_ZEN_ASCII);
        LETTER_OP_CODE_LOOKUP.put('a', OP_ZEN_ASCII_TO_HAN_ASCII);
        LETTER_OP_CODE_LOOKUP.put('C', OP_ZEN_HIRA_TO_ZEN_KATA);
        LETTER_OP_CODE_LOOKUP.put('c', OP_ZEN_KATA_TO_ZEN_HIRA);
        LETTER_OP_CODE_LOOKUP.put('H', OP_HAN_KATA_TO_ZEN_HIRA);
        LETTER_OP_CODE_LOOKUP.put('h', OP_ZEN_HIRA_TO_HAN_KATA);
        LETTER_OP_CODE_LOOKUP.put('K', OP_HAN_KATA_TO_ZEN_KATA);
        LETTER_OP_CODE_LOOKUP.put('k', OP_ZEN_KATA_TO_HAN_KATA);
        LETTER_OP_CODE_LOOKUP.put('N', OP_HAN_NUMBER_TO_ZEN_NUMBER);
        LETTER_OP_CODE_LOOKUP.put('n', OP_ZEN_NUMBER_TO_HAN_NUMBER);
        LETTER_OP_CODE_LOOKUP.put('R', OP_HAN_LETTER_TO_ZEN_LETTER);
        LETTER_OP_CODE_LOOKUP.put('r', OP_ZEN_LETTER_TO_HAN_LETTER);
        LETTER_OP_CODE_LOOKUP.put('S', OP_HAN_SPACE_TO_ZEN_SPACE);
        LETTER_OP_CODE_LOOKUP.put('s', OP_ZEN_SPACE_TO_HAN_SPACE);
    }


    //{{{ String convertKana(String, int, String)
    /**
      * Converts a string containing kana or other characters used in Japanese text input
      * according to one or more requested conversion methods.
      *
      * @param  original_string  Input string to perform conversion on
      * @param  conversion_ops   Flag-based integer indicating which type of conversions to perform
      * @param  chars_to_ignore  Each character in this string will be excluded from conversion
      * @return Content of "original_string" with specified conversions performed
      */
    public static String convertKana(String original_string, int conversion_ops, String chars_to_ignore)
    {
        // Don't perform conversions on empty string
        if(original_string.equals("")) {
            return "";
        }

        // Return original if no conversion requested
        if(conversion_ops <= 0) {
            return original_string;
        }

        boolean do_collapse_on_hankaku_diacritic = true;
        if((conversion_ops & OP_KEEP_DIACRITIC_MARKS_APART) != 0) {
            // Do not glue hankaku katakana diacritic symbols when converting to zenkaku.
            // Use with 'K' or 'H'
            do_collapse_on_hankaku_diacritic = false;
        }

        int char_count = original_string.length();
        StringBuffer new_string = new StringBuffer();
        int i = 0;
        while(i < char_count) {
            // Init char holders for this round
            char this_char = original_string.charAt(i);
            char current_char = this_char;
            char hankaku_diacritic_suffix = 0;
            char next_char = 0;
            if(i < (char_count - 1)) {
                next_char = original_string.charAt(i + 1);
            }

            // Skip all conversions if character is on the excluded chars list
            boolean is_ignore_char = isIgnoreChar(current_char, chars_to_ignore);
            if(is_ignore_char) {
                new_string.append(current_char);
                i++;
                continue;
            }

            // Order of conversion operations written to be similar to original PHP
            //// Source: https://github.com/php/php-src/blob/128eda843f7dff487fff529a384fee3c5494e0f6/ext/mbstring/libmbfl/filters/mbfilter_tl_jisx0201_jisx0208.c#L41
            if(0 != (conversion_ops & OP_HAN_ASCII_TO_ZEN_ASCII)) {
                current_char = convertHankakuAsciiToZenkakuAscii(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_HAN_LETTER_TO_ZEN_LETTER)) {
                current_char = convertHankakuLetterToZenkakuLetter(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_HAN_NUMBER_TO_ZEN_NUMBER)) {
                current_char = convertHankakuNumberToZenkakuNumber(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_HAN_SPACE_TO_ZEN_SPACE)) {
                current_char = convertHankakuSpaceToZenkakuSpace(current_char);
            }

            if(current_char == this_char
            && (0 != (conversion_ops & OP_HAN_KATA_TO_ZEN_KATA)
            ||  0 != (conversion_ops & OP_HAN_KATA_TO_ZEN_HIRA))) {
                char collapsed_char_for_check = current_char;
                boolean performed_hankaku_conversion = false;
                if(do_collapse_on_hankaku_diacritic) {
                    // Check if current character requires the collapsing of a diacritic mark
                    collapsed_char_for_check = convertDiacriticHankakuKanaToZenkaku(current_char, next_char);
                }

                if(collapsed_char_for_check != current_char) {
                    // Use collapsed result
                    current_char = collapsed_char_for_check;
                    performed_hankaku_conversion = true;

                    // Do not include next character in final result string because
                    // it is a hankaku-only diacritic mark that isn't needed after conversion to zenkaku
                    i++;
                }
                else {
                    // Use result from hankaku-kana unvoiced mapping
                    char converted_current_char = convertUnvoicedHankakuKanaToZenkaku(current_char);
                    if(converted_current_char != current_char) {
                        current_char = converted_current_char;
                        performed_hankaku_conversion = true;
                    }
                }

                if(performed_hankaku_conversion
                && 0 == (conversion_ops & OP_HAN_KATA_TO_ZEN_KATA)) {
                    // If request is not for katakana, perform additional kata->hira conversion
                    current_char = convertZenkakuKatakanaToZenkakuHiragana(current_char);
                }
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_ZEN_ASCII_TO_HAN_ASCII)) {
                current_char = convertZenkakuAsciiToHankakuAscii(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_ZEN_LETTER_TO_HAN_LETTER)) {
                current_char = convertZenkakuLetterToHankakuLetter(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_ZEN_NUMBER_TO_HAN_NUMBER)) {
                current_char = convertZenkakuNumberToHankakuNumber(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_ZEN_SPACE_TO_HAN_SPACE)) {
                current_char = convertZenkakuSpaceToHankakuSpace(current_char);
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_ZEN_KATA_TO_HAN_KATA)) {
                hankaku_diacritic_suffix = determineHankakuDiacriticSuffix(current_char);
                current_char = convertZenkakuKatakanaToHankakuKatakana(current_char);
            }

            // Check if current character is a zenkaku katakana character
            char full_katakana_to_hiragana_result = convertZenkakuKatakanaToZenkakuHiragana(current_char);

            // Do not enter this block if the current character is a zenkaku katakana character, no matter the flags
            // Protects against katakana characters being incorrectly converted by zen-hiragana to han-katakana logic
            if(current_char == this_char
            && full_katakana_to_hiragana_result == current_char
            && (0 != (conversion_ops & OP_ZEN_HIRA_TO_ZEN_KATA)
            ||  0 != (conversion_ops & OP_ZEN_HIRA_TO_HAN_KATA))) {
                // First convert from full hiragana to full katakana
                current_char = convertZenkakuHiraganaToZenkakuKatakana(current_char);

                if(0 != (conversion_ops & OP_ZEN_HIRA_TO_HAN_KATA)) {
                    // Proceed to convert to hankaku if requested (skip if zen-kata to han-kata conversion was already performed)
                    hankaku_diacritic_suffix = determineHankakuDiacriticSuffix(current_char);
                    current_char = convertZenkakuKatakanaToHankakuKatakana(current_char);
                }
            }

            if(current_char == this_char
            && 0 != (conversion_ops & OP_ZEN_KATA_TO_ZEN_HIRA)) {
                current_char = full_katakana_to_hiragana_result;
            }

            // Add converted character to output string buffer
            new_string.append(current_char);

            // Add hankaku diacritic mark if necessary (only for zen-to-han kana conversions)
            if(hankaku_diacritic_suffix == HANKAKU_VOICED_MARK
            || hankaku_diacritic_suffix == HANKAKU_ASPIRATED_MARK) {
                new_string.append(hankaku_diacritic_suffix);
            }

            // Proceed with loop
            i++;
        }

        return new_string.toString();
    }
    //}}}
    //{{{ String convertKana(String, int)
    /**
      * Converts a string containing kana or other characters used in Japanese text input
      * according to one or more requested conversion methods.
      *
      * @param  original_string  Input string to perform conversion on
      * @param  conversion_ops   Flag-based integer indicating which type of conversions to perform
      * @return Content of "original_string" with specified conversions performed
      */
    public static String convertKana(String original_string, int conversion_ops)
    {
        return convertKana(original_string, conversion_ops, "");
    }
    //}}}
    //{{{ String convertKana(String, String, String)
    /**
      * Converts a string containing kana or other characters used in Japanese text input
      * according to one or more requested conversion methods.
      *
      * @param  original_string         Input string to perform conversion on
      * @param  conversion_ops_string   PHP mb_convert_kana style string specifying desired conversions
      * @param  chars_to_ignore         Each character in this string will be excluded from conversion
      * @return Content of "original_string" with specified conversions performed
      */
    public static String convertKana(String original_string, String conversion_ops_string, String chars_to_ignore)
    {
        int conversion_ops = createOpsArrayFromString(conversion_ops_string);
        return convertKana(original_string, conversion_ops, chars_to_ignore);
    }
    //}}}
    //{{{ String convertKana(String, String)
    /**
      * Converts a string containing kana or other characters used in Japanese text input
      * according to one or more requested conversion methods.
      *
      * @param  original_string         Input string to perform conversion on
      * @param  conversion_ops_string   PHP mb_convert_kana style string specifying desired conversions
      * @return Content of "original_string" with specified conversions performed
      */
    public static String convertKana(String original_string, String conversion_ops_string)
    {
        return convertKana(original_string, conversion_ops_string, "");
    }
    //}}}


    //{{{ Hankaku Katakana related mappings
    // Diacritic constants
    public static final char HANKAKU_VOICED_MARK    = '???';  // dakuten
    public static final char HANKAKU_ASPIRATED_MARK = '???';  // handakuten

    protected static final Map<Character, Character> MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED;
    static {
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED = new HashMap<Character, Character>();
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.put('???', '???');
    }

    protected static final Map<Character, Character> MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED;
    static {
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED = new HashMap<Character, Character>();
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.put('???', '???');
    }

    protected static final Map<Character, Character> MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED;
    static {
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED = new HashMap<Character, Character>();
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.put('???', '???');
        MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.put('???', '???');
    }

    protected static final Map<Character, Character> MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA;
    static {
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA = new HashMap<Character, Character>();
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
        MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.put('???', '???');
    }

    protected static final Map<Character, Character> MAPPING_HANKAKU_DIACRITIC_SUFFIXES;
    static {
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES = new HashMap<Character, Character>();
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_VOICED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_ASPIRATED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_ASPIRATED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_ASPIRATED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_ASPIRATED_MARK);
        MAPPING_HANKAKU_DIACRITIC_SUFFIXES.put('???', HANKAKU_ASPIRATED_MARK);
    }
    //}}}


    // Connect mapping of hiragana and katakana char codes
    public static final int OFFSET_ZENKAKU_HIRAGANA_TO_ZENKAKU_KATAKANA =
    (KanaAppraiser.ZENKAKU_KATAKANA_FIRST - KanaAppraiser.ZENKAKU_HIRAGANA_FIRST);

    // Connect mapping of regular ASCII characters to Zenkaku ASCII characters
    public static final int OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII =
    (KanaAppraiser.ZENKAKU_ASCII_FIRST - KanaAppraiser.HANKAKU_ASCII_FIRST);


    //{{{ char convertHankakuAsciiToZenkakuAscii(char)
    protected static char convertHankakuAsciiToZenkakuAscii(char target)
    {
        if(KanaAppraiser.isHankakuAscii(target)) {
            return (char)(target + OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII);
        } else if(target == KanaAppraiser.HANKAKU_SPACE) {
            return KanaAppraiser.ZENKAKU_SPACE;
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertZenkakuAsciiToHankakuAscii(char)
    protected static char convertZenkakuAsciiToHankakuAscii(char target)
    {
        if(KanaAppraiser.isZenkakuAscii(target)) {
            return (char)(target - OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII);
        } else if(target == KanaAppraiser.ZENKAKU_SPACE) {
            return KanaAppraiser.HANKAKU_SPACE;
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertZenkakuHiraganaToZenkakuKatakana(char)
    protected static char convertZenkakuHiraganaToZenkakuKatakana(char target)
    {
        if(KanaAppraiser.isZenkakuHiraganaWithKatakanaEquivalent(target)) {
            return (char)(target + OFFSET_ZENKAKU_HIRAGANA_TO_ZENKAKU_KATAKANA);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertZenkakuKatakanaToZenkakuHiragana(char)
    protected static char convertZenkakuKatakanaToZenkakuHiragana(char target)
    {
        if(KanaAppraiser.isZenkakuKatakanaWithHiraganaEquivalent(target)) {
            return (char)(target - OFFSET_ZENKAKU_HIRAGANA_TO_ZENKAKU_KATAKANA);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertUnvoicedHankakuKanaToZenkaku(char)
    protected static char convertUnvoicedHankakuKanaToZenkaku(char target)
    {
        if(MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.containsKey(target)) {
            // Return character from *unvoiced* han-to-zen mapping
            return MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_UNVOICED.get(target);
        }
        else {
            return target;
        }
    }
    //}}}


    //{{{ char convertDiacriticHankakuKanaToZenkaku(char)
    protected static char convertDiacriticHankakuKanaToZenkaku(char target, char diacritic_mark)
    {
        if(diacritic_mark == HANKAKU_VOICED_MARK
        && MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.containsKey(target)) {
            // Use character from *voiced* han-to-zen mapping
            return MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_VOICED.get(target);
        }

        if(diacritic_mark == HANKAKU_ASPIRATED_MARK
        && MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.containsKey(target)) {
            // Use character from *aspirated* han-to-zen mapping
            return MAPPING_HANKAKU_TO_ZENKAKU_KATAKANA_ASPIRATED.get(target);
        }

        // Not a voiced/aspirated hankaku katakana character, use original
        return target;
    }
    //}}}


    //{{{ char convertZenkakuKatakanaToHankakuKatakana(char)
    protected static char convertZenkakuKatakanaToHankakuKatakana(char target)
    {
        if(MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.containsKey(target)) {
            // Return character from mapped from zen-to-han
            return MAPPING_ZENKAKU_TO_HANKAKU_KATAKANA.get(target);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char determineHankakuDiacriticSuffix(char)
    protected static char determineHankakuDiacriticSuffix(char target)
    {
        if(MAPPING_HANKAKU_DIACRITIC_SUFFIXES.containsKey(target)) {
            return MAPPING_HANKAKU_DIACRITIC_SUFFIXES.get(target);
        } else {
            return 0;
        }
    }
    //}}}


    //{{{ char convertHankakuNumberToZenkakuNumber(char)
    protected static char convertHankakuNumberToZenkakuNumber(char target)
    {
        if(KanaAppraiser.isHankakuNumber(target)) {
            // Offset by difference in char-code position
            return (char)(target + OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertZenkakuNumberToHankakuNumber(char)
    protected static char convertZenkakuNumberToHankakuNumber(char target)
    {
        if(KanaAppraiser.isZenkakuNumber(target)) {
            // Offset by difference in char-code position
            return (char)(target - OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertHankakuLetterToZenkakuLetter(char)
    protected static char convertHankakuLetterToZenkakuLetter(char target)
    {
        if(KanaAppraiser.isHankakuLetter(target)) {
            return (char)(target + OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertZenkakuLetterToHankakuLetter(char)
    protected static char convertZenkakuLetterToHankakuLetter(char target)
    {
        if(KanaAppraiser.isZenkakuLetter(target)) {
            return (char)(target - OFFSET_HANKAKU_ASCII_TO_ZENKAKU_ASCII);
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertHankakuSpaceToZenkakuSpace(char)
    protected static char convertHankakuSpaceToZenkakuSpace(char target)
    {
        if(target == KanaAppraiser.HANKAKU_SPACE) {
            return KanaAppraiser.ZENKAKU_SPACE;
        } else {
            return target;
        }
    }
    //}}}


    //{{{ char convertZenkakuSpaceToHankakuSpace(char)
    protected static char convertZenkakuSpaceToHankakuSpace(char target)
    {
        if(target == KanaAppraiser.ZENKAKU_SPACE) {
            return KanaAppraiser.HANKAKU_SPACE;
        } else {
            return target;
        }
    }
    //}}}


    //{{{ boolean isIgnoreChar(char, String)
    protected static boolean isIgnoreChar(char char_to_check, String chars_to_ignore)
    {
        int ignore_char_count = chars_to_ignore.length();
        for(int i = 0; i < ignore_char_count; i++) {
            if(char_to_check == chars_to_ignore.charAt(i)) {
                // Matched
                return true;
            }
        }

        // No matches
        return false;
    }
    //}}}


    //{{{ int createOpsArrayFromString(String)
    private static int createOpsArrayFromString(String php_style_options_string)
    {
        int char_op_count = php_style_options_string.length();
        int conversion_op_flags = 0;
        for(int i = 0; i < char_op_count; i++) {
            char php_style_op_code = php_style_options_string.charAt(i);
            if(LETTER_OP_CODE_LOOKUP.containsKey(php_style_op_code)) {
                conversion_op_flags |= LETTER_OP_CODE_LOOKUP.get(php_style_op_code);
            }
        }
        return conversion_op_flags;
    }
    //}}}
}
