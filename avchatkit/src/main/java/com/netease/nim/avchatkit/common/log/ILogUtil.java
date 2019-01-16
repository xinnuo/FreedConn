package com.netease.nim.avchatkit.common.log;

public interface ILogUtil {
    void ui(String msg);

    void e(String tag, String msg);

    void i(String tag, String msg);

    void d(String tag, String msg);
}
