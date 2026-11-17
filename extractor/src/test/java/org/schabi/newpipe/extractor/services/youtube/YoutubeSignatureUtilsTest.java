package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.utils.JavaScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeSignatureUtilsTest {
    @Test
    void deobfuscatesSignatureUsingPlayerUrlFunction() throws Exception {
        // The URL constructor applies the signature transform internally; it is not a
        // standalone string transform and does not use the legacy global string array.
        final String player = """
                var _yt_player={};(function(g){var window=this;
                g.url=function(){var values={};return {set:function(k,v){values[k]=v},get:function(k){return values[k]}}};
                unrelated=function(){return "untouched"};$url=function(url,key,value){var result=new g.url(url);result.set("alr","yes");value&&(result.set(key,value.split("").reverse().join("")));return result};
                })(_yt_player);
                """;
        assertEquals("fedcba", deobfuscate(player, "abcdef"));
    }

    @Test
    void stillDeobfuscatesLegacySignatures() throws Exception {
        final String player = """
                var AA='split;join'.split(";");
                var BB={rv:function(a){a.reverse()}};
                CC=function(a){a=a[AA[0]]("");BB["rv"](a);return a[AA[1]]("")};
                c&&(c=CC(decodeURIComponent(c)));
                """;
        assertEquals("fedcba", deobfuscate(player, "abcdef"));
    }

    @Test
    void rejectsUnknownPlayerStructure() {
        assertThrows(ParsingException.class,
                () -> YoutubeSignatureUtils.getDeobfuscationCode("var unrelated = 1;"));
    }

    private static String deobfuscate(final String player, final String signature) throws Exception {
        return JavaScript.run(YoutubeSignatureUtils.getDeobfuscationCode(player),
                YoutubeSignatureUtils.DEOBFUSCATION_FUNCTION_NAME, signature);
    }
}
