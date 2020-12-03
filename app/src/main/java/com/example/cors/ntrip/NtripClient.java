package com.example.cors.ntrip;

import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

/**
 * ntrip客户端，连接CORS服务器获取差分数据
 */
public class NtripClient {

    private static final String TAG = NtripClient.class.getSimpleName();

    /**
     * 超时自动重连的时间，单位：毫秒
     * 长时间无数据相应,就断开连接重新连接
     */
    private long TIME_OUT = 30000;
    /**
     * 接受不到数据重新连接的阈值
     */
    private long RECONNECT_INTERVALS_TIME = 30000;

    /**
     * 数据采集频率
     */
    private long FREQ_TIME = 300;
    /**
     * 服务器地址
     */
    private String ip;

    /**
     * 服务器端口
     */
    private int port;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 挂载点
     */
    private String mountPoint;


    /**
     * 用于判断是否超时
     */
    private long currentDiffTime;
//
//    /**
//     * 连接,断开连接,连接失败,用户名密码错误,监听数据
//     */
//    //默认状态
//    int CONNECT_STATUS = MSG_DISCONNECT;
//    //断开连接中
//    static final int MSG_DISCONNECT = 0;
//    //连接中
//    static final int MSG_CONNECTING = 1;
//    //连接错误
//    static final int MSG_CONNECT_ERR = 2;
//    //用户名密码错误
//    static final int MSG_CONNECT_ERR_401 = 3;
//    //监听数据中
//    static final int MSG_CONNECTING_LISTEN = 4;


    private static NtripClient instance;
    private Object readLock = new Object();

    private Object writeLock = new Object();

    private boolean isRun = false;


    private NtripClient() {

    }

    /**
     * 单例
     *
     * @return
     */
    public static NtripClient getInstance() {
        if (instance == null) {
            instance = new NtripClient();
        }
        return instance;
    }


    /**
     * 数据监听
     */
    private List<IDataListener> dataListenerList = new ArrayList<>();

    /**
     * 添加数据监听
     */
    public void addDataListener(IDataListener listener) {
        if (dataListenerList.contains(listener) == false) {
            dataListenerList.add(listener);
        }
    }

    /**
     * 移除数据监听
     */
    public void removeDataListener(IDataListener listener) {
        if (dataListenerList.contains(listener)) {
            dataListenerList.remove(listener);
        }
    }

    /**
     * 解码
     *
     * @param buffer 返回的数据,只能解码登录的校验数据
     */
    public boolean parseNetworkDataStream(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {// Data stream confirmed.
            LogMessage("NTRIP:未监听到数据" + buffer);
            return false;
        }
        String NTRIPResponse = new String(buffer); // Add what we got to the string, in case the response spans more than one packet.
        if (NTRIPResponse.startsWith("ICY 200 OK")) {// Data stream confirmed.
            LogMessage("NTRIP:已经连接到服务器");
            return true;
        } else if (NTRIPResponse.indexOf("401 Unauthorized") > 1) {
            //Log.i("handleMessage", "Invalid Username or Password.");
            LogMessage("NTRIP: 用户名密码错误.");
            return false;
        } else if (NTRIPResponse.startsWith("SOURCETABLE 200 OK")) {
            LogMessage("NTRIP: 准备监听数据流");
//            NTRIPResponse = NTRIPResponse.substring(20); // Drop the beginning of the data
            return true;
        } else if (NTRIPResponse.length() > 0) { // We've received 1KB of data but no start command. WTF?
            LogMessage("NTRIP: 监听到数据");
            return true;
        }
        return false;
    }

    private boolean isPrintLog = true;

    private void LogMessage(String s) {
        if (isPrintLog) {
            for (IDataListener listener : dataListenerList) {
                listener.onReceived((s+"\n").getBytes());
                Log.d(TAG, "---" + s);
            }
        }

    }

    /**
     * 连接服务器
     *
     * @param ip         服务器地址
     * @param port       服务器端口
     * @param userName   用户名
     * @param password   密码
     * @param mountPoint 挂载点
     */
    public void connect(String ip, int port, String userName, String password, String mountPoint) {

        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.mountPoint = mountPoint;
        if (isRun) {
            //任务已经运行不允许再次运行
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                // 连接并登录服务器
                connectServer();
            }
        }).start();

    }

    /**
     * 断开连接
     */
    public void disconnect() {
        isRun = false;

    }


    /**
     * 连接超时，单位：毫秒
     */
    public static final int CONNECT_TIMEOUT = 10 * 1000;

    /**
     * 登录服务器
     */
    private void connectServer() {
        // 开始读数据
        currentDiffTime = SystemClock.uptimeMillis();
        LogMessage("连接服务器");
        // 构造命令
        isRun = true;
        try {
            // 登录服务器
            // 发送登录的数据

            String auth = Base64.encodeToString(
                    (userName + ":" + password).getBytes(Charset.forName("UTF-8")), Base64.DEFAULT);
            auth = auth.substring(0, auth.length() - 1);
            String head = "GET /" + mountPoint + " HTTP/1.1\r\n";
            head += "User-Agent: NTRIP XXXXXX\r\n";
            head += "Accept: */*\r\nConnection: close\r\n";
            head += "Authorization: Basic " + auth + "\r\n";
            head += "\r\n";


            InetAddress iAddress = InetAddress.getByName(ip);
            InetSocketAddress socketAddress = new InetSocketAddress(iAddress, port);
            Socket tcpSocket = new Socket();
            // TCP连接超时设为10秒
            tcpSocket.connect(socketAddress, CONNECT_TIMEOUT);
            tcpSocket.setTcpNoDelay(true);
            InputStream inputStream = tcpSocket.getInputStream();
            OutputStream outputStream = tcpSocket.getOutputStream();
            //此连接状态一直不会变,只要一开始true,哪怕断网后也是一值true
            boolean isOpen = tcpSocket.isConnected();
            if (!isOpen) {
                //连接失败一定时间后重连
                reconnect();
                return;
            }
            //设置读取数据流的超时时间,如果达到时间没有读取到,抛异常
            tcpSocket.setSoTimeout(20 * 1000);
            outputStream.write(head.getBytes());
            while (isRun) {
                Thread.sleep(300);
                byte[] readBuff = readFromNet(inputStream);

                // 验证数据
                if (parseNetworkDataStream(readBuff)) {//解析数据
                    // 开始读数据
                    currentDiffTime = SystemClock.uptimeMillis();
                    if (readBuff != null && readBuff.length > 0) {
                        for (IDataListener listener : dataListenerList) {
                            listener.onReceived(readBuff);
                        }
                    }
                } else {//长时间未收到数据,或者数据为空,断开重连\
                    long time = SystemClock.uptimeMillis() - currentDiffTime;
                    if (time > TIME_OUT) {
                        LogMessage(TIME_OUT + "ms未收到数据达到断开阈值");
                        reconnect();
                    }
                }
            }

        } catch (ConnectException e) {
            //连接失败一定时间后重连
            LogMessage("错误:ConnectException"+e.getMessage());
            reconnect();
            // 端口号错误
            e.printStackTrace();
        } catch (UnknownHostException e) {
            LogMessage("错误:UnknownHostException"+e.getMessage());
            //连接失败一定时间后重连
            reconnect();
            //host错误
            e.printStackTrace();
        } catch (IOException e) {
            LogMessage("错误:IOException"+e.getMessage());
            //连接失败一定时间后重连
            reconnect();
            //连接错误
            e.printStackTrace();
        } catch (Exception e) {
            LogMessage("错误:Exception"+e.getMessage());
            //连接失败一定时间后重连
            reconnect();
            //未知错误
            e.printStackTrace();
        }
    }

    /**
     * 间隔一定时间后
     * 重新连接
     * 增加重启间隔
     */
    private void reconnect() {
        try {
            LogMessage(RECONNECT_INTERVALS_TIME + "ms后重新连接");
            disconnect();
            Thread.sleep(RECONNECT_INTERVALS_TIME);
            connectServer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 从服务器接收数据
     *
     * @param inputStream
     */
    private byte[] readFromNet(InputStream inputStream) {
        try {
            synchronized (readLock) {
                int sum = available(inputStream);

                if (sum > 0) {
                    byte[] reData = new byte[sum];
                    int result = read(reData, 0, reData.length, inputStream);
                    if (result > 0) {
                        return reData;
                    }
                }
            }
        } catch (Exception ex) {
            disconnect();
        }
        return null;
    }

    /**
     * 判断可读取的字节长度
     * 网络差分需要该判断，否则在没有差分数据时，inputstream.read会卡住
     *
     * @param inputStream
     */
    public int available(InputStream inputStream) {
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
     *
     * @param buffer      缓存字节数组
     * @param startPos    开始位置
     * @param count       最大读取长度
     * @param inputStream
     * @return 正常读到的字节数。如果是-1，则表示已经读到了流的末尾，
     * 对于socket通讯而言，-1并不意味着socket连接已经断开，而只是另一端长时间没有数据
     * 发送过来；是-2表示读输入流抛出异常，这种情况有可能是socket连接已经终止或没有socket连接。
     */
    public int read(byte[] buffer, int startPos, int count, InputStream inputStream) {

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
     * 设置超时自动重连的时间
     * 长时间无数据相应,就断开连接重新连接
     *
     * @param time_out 时间毫秒
     */
    public void setTimeOut(long time_out) {
        this.TIME_OUT = time_out;
    }


    /**
     * 接受不到数据重新连接的时间阈值
     *
     * @param reconnect_intervals_time 单位毫秒,超过此时间自动断开重连
     */
    public void setReconnectIntervalsTime(long reconnect_intervals_time) {
        this.RECONNECT_INTERVALS_TIME = reconnect_intervals_time;
    }

    /**
     * 数据采集频率,会影响监听的回调的执行频率,
     *
     * @param freq_time 频率间隔时间 尽量小于1000ms,单位毫秒
     */
    public void setFreqTime(long freq_time) {
        this.FREQ_TIME = freq_time;
    }

    /**
     * 是否打印日志信息,
     * @param printLog true 回调会接受日志信息,falsh 不会接受也不会打印log
     */
    public void setPrintLog(boolean printLog) {
        isPrintLog = printLog;
    }
}
