package com.lxbluem;

import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.function.Supplier;

import static org.junit.Assert.*;

//@Ignore
public class GeneralQuickTest {
    @Test
    public void stringutils() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("nope"));
    }

    @Test(expected = DecodeException.class)
    public void jsonMapper() {
        String content = "{\"text\": \"lala\"}";
        DecodeTestingClass decoded = Json.decodeValue(content, DecodeTestingClass.class);
        assertEquals("lala", decoded.text);

        DecodeTestingClass decoded2 = Json.decodeValue("", DecodeTestingClass.class);
        assertEquals("", decoded2.text);

    }

    @Data
    private static class DecodeTestingClass {
        String text;
    }

    @Test
    public void nextValue() {
        ListIterator<String> iterator = Arrays.asList("1", "2", "3").listIterator();
        Supplier<String> sup = iterator::next;

        assertEquals("1", sup.get());
        assertEquals("2", sup.get());
        assertEquals("3", sup.get());
    }

    @Test
    public void JsonObject_map_to_Object() {
        JsonObject object = new JsonObject().put("base", "aaa").put("detail", "bbb");
        BaseDetail message = object.mapTo(BaseDetail.class);
        assertEquals("aaa", message.getBase());
        assertEquals("bbb", message.getDetail());

        BaseDetail rev = BaseDetail.builder().base("bbbase").detail("dddetail").build();
        System.out.println(rev);
    }

    @Getter
    @NoArgsConstructor
    @ToString(callSuper = true)
    private static class BaseDetail extends Base {
        private String detail;

        @Builder
        public BaseDetail(String base, String detail) {
            super(base);
            this.detail = detail;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Base {
        private String base;
    }

    @Test
    public void promises() {
        Promise<String> promise = Promise.promise();
        promise.future().onComplete(s -> System.out.println(s.result()));
        promise.complete("lala");
    }

    @Test
    public void arrayofstrings() {
        String[] str = new String[]{"a", "b", "c"};
        String collect = String.join(" ", str);
        System.out.println(collect);
    }

    @Test
    public void stringUtils_containsOnly() {
        String valid = "0123456789";

        assertTrue(StringUtils.containsOnly("92", valid));
        assertTrue(StringUtils.containsOnly("", valid));
        assertFalse(StringUtils.containsOnly(null, valid));
        assertFalse(StringUtils.containsOnly("undefined", valid));
    }
}