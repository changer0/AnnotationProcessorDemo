package com.lulu.lib_compiler;

import javax.lang.model.element.Element;

/**
 * @author zhanglulu on 2020/10/27.
 * for 注解日志系统
 */
class Logger {

    public static final String TAG = "AnnotationProcessDemo";
    public static void println(String msg) {
        System.out.println(TAG + ": " + msg);
    }

    public static void error(Element element, String msg, Object... formatStr) {
        System.out.println(TAG + String.format(msg, (Object) formatStr));
    }
}
