package com.example.cors.ntrip;

/**
 * 作者 : lei
 * 时间 : 2020/12/02.
 * 邮箱 :416587959@qq.com
 * 描述 :数据接收监听器
 */

public interface IDataListener {
    /**
     * 接收到数据
     * @param buffer 数据
     */
    void onReceived(byte[] buffer);
}