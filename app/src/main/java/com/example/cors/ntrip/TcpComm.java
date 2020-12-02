package com.example.cors.ntrip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * 网络通讯：TCP传输控制协议（注意：网络操作不能在UI线程中进行）
 */
public class TcpComm {

    /**
     * 连接超时，单位：毫秒
     */
    public static final int CONNECT_TIMEOUT = 10 * 1000;

    private static String NET_LINE = "\r\n";

    private Object writeLock = new Object();

    protected Socket tcpSocket;
    protected String host;
    protected int port;

    private InputStream inputStream;
    private OutputStream outputStream;

    protected boolean isOpen = false;

    /**
     * 是否已打开
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * 构造网络通讯，指定远端设备的网络地址和端口
     */
    public TcpComm(String remoteHost, int port) {
        this.port = port;
        host = remoteHost;
    }

    /**
     * 建立连接
     */
    public boolean open() {
        if (!isOpen) {
            try {
                InetAddress iAddress = InetAddress.getByName(host);
                InetSocketAddress socketAddress = new InetSocketAddress(iAddress, port);
                tcpSocket = new Socket();
                // TCP连接超时设为10秒
                tcpSocket.connect(socketAddress, CONNECT_TIMEOUT);
                tcpSocket.setTcpNoDelay(true);
                inputStream = tcpSocket.getInputStream();
                outputStream = tcpSocket.getOutputStream();
                isOpen = tcpSocket.isConnected();
            } catch (ConnectException e) {
                // 端口号错误
                e.printStackTrace();
                return false;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return isOpen;
    }

    /**
     * 断开连接
     */
    public void close() {
        if (tcpSocket != null && isOpen) {
            try {
                tcpSocket.close();
                inputStream = null;
                outputStream = null;
                tcpSocket = null;
                isOpen = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断可读取的字节长度
     * 网络差分需要该判断，否则在没有差分数据时，inputstream.read会卡住
     */
    public int available() {
        if (inputStream != null) {
            try {
                return inputStream.available();
            } catch (Exception e) {
                return -2;
            }
        }
        return -2;
    }

    /**
     * 读输入流
     * @param buffer 缓存字节数组
     * @return 正常读到的字节数。如果是-1，则表示已经读到了流的末尾，
     *         对于socket通讯而言，-1并不意味着socket连接已经断开，而只是另一端长时间没有数据
     *         发送过来；是-2表示读输入流抛出异常，这种情况有可能是socket连接已经终止或没有socket连接。
     */
    public int read(byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    /**
     * 读输入流
     * @param buffer 缓存字节数组
     * @param startPos 开始位置
     * @param count 最大读取长度
     * @return 正常读到的字节数。如果是-1，则表示已经读到了流的末尾，
     *         对于socket通讯而言，-1并不意味着socket连接已经断开，而只是另一端长时间没有数据
     *         发送过来；是-2表示读输入流抛出异常，这种情况有可能是socket连接已经终止或没有socket连接。
     */
    public int read(byte[] buffer, int startPos, int count) {

        synchronized (inputStream) {
            if (inputStream != null) {
                try {
                    int length = inputStream.available();
                    if (length > 0) {
                        return inputStream.read(buffer, startPos, Math.min(length, count));
                    }
                    return 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    return -2;
                }
            }
        }
        return -2;
    }

    /**
     * 写输出流
     * @param buffer 输出字节数组
     */
    public boolean write(byte[] buffer) {
        return write(buffer, 0, buffer.length);
    }

    /**
     * 发送字符数据
     * @param strSend 发送字符串
     * @return 发送是否成功
     */
    public final boolean write(String strSend) {
        try {
            byte[] buffer = strSend.getBytes("US-ASCII");
            return write(buffer, 0, buffer.length);
        } catch (UnsupportedEncodingException e) {
        }
        return false;
    }

    /**
     * 在发送数据后追加换行符发送数据
     * @param strSend 发送字符串
     * @return 发送是否成功
     */
    public final boolean writeLine(String strSend) {
        strSend = strSend + NET_LINE;
        return write(strSend);
    }

    /**
     * 写输出流
     * @param buffer 输出字节数组
     * @param startPos 开始位置
     * @param count 最大写入长度
     */
    public boolean write(byte[] buffer, int startPos, int count) {
        synchronized (writeLock) {
            if (outputStream != null) {
                try {
                    outputStream.write(buffer, startPos, count);
                    outputStream.flush();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}