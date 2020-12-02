package com.example.cors.ntrip;

import android.util.Base64;
import android.util.Log;


import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.Utils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ntrip客户端，连接CORS服务器获取差分数据
 */
public class NtripClient {

    private static final String TAG = NtripClient.class.getSimpleName();

    /**
     * 超时自动重连的时间，单位：毫秒
     */
    private long TIME_OUT = 30000;

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
     * 网络通讯
     */
    private TcpComm comm;


    /**
     * 读数据
     */
    private ScheduledExecutorService readService;

    /**
     * 用于判断是否超时
     */
    private long currentDiffTime;

    private Object readLock = new Object();

    private Object writeLock = new Object();
    private String mHead;
    private ThreadUtils.SimpleTask task = new ThreadUtils.SimpleTask() {
        @Override
        public Object doInBackground() throws Throwable {
            // 连接并登录服务器
            connectServer();
            return null;
        }

        @Override
        public void onSuccess(Object result) {
        }
    };

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return comm != null && comm.isOpen();
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
        String NTRIPResponse = new String(buffer); // Add what we got to the string, in case the response spans more than one packet.
        if (buffer == null || buffer.length > 0) {// Data stream confirmed.
            LogMessage("NTRIP:已经连接到服务器");
            return false;
        } else if (NTRIPResponse.startsWith("ICY 200 OK")) {// Data stream confirmed.
            LogMessage("NTRIP:已经连接到服务器");
            return true;
        } else if (NTRIPResponse.indexOf("401 Unauthorized") > 1) {
            //Log.i("handleMessage", "Invalid Username or Password.");
            LogMessage("NTRIP: 用户名密码错误.");
            return false;
        } else if (NTRIPResponse.startsWith("SOURCETABLE 200 OK")) {
            LogMessage("NTRIP: 监听数据流");
//            NTRIPResponse = NTRIPResponse.substring(20); // Drop the beginning of the data
            return true;
        } else if (NTRIPResponse.length() > 1024) { // We've received 1KB of data but no start command. WTF?
            LogMessage("NTRIP: 无法识别的服务器响应");
            return true;
        }
        return false;
    }

    private void LogMessage(String s) {
        Log.d(TAG, "---" + s);
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


        // 构造命令
        String auth = Base64.encodeToString(
                (userName + ":" + password).getBytes(Charset.forName("UTF-8")), Base64.DEFAULT);
        auth = auth.substring(0, auth.length() - 1);
        mHead = "GET /" + mountPoint + " HTTP/1.1\r\n";
        mHead += "User-Agent: NTRIP XXXXXX\r\n";
        mHead += "Accept: */*\r\nConnection: close\r\n";
        mHead += "Authorization: Basic " + auth + "\r\n";
        mHead += "\r\n";



        ThreadUtils.executeByCpu(task);
    }

    /**
     * 断开连接
     */
    public void disconnect() {

        ThreadUtils.cancel(task);


        // 断开连接
        if (comm != null) {
            comm.close();
            comm = null;
        }
    }

    /**
     * 发送GGA
     * GGA示例：$GNGGA,021102.00,2259.01378,N,11322.05927,E,1,12,0.60,40.7,M,-5.1,M,,*66
     */
    public void sendGGA(String gga) {
        writeToNet(gga.getBytes());
    }

    /**
     * 获取挂载点（耗时操作，最长等待20s）
     *
     * @param ip   服务器地址
     * @param port 服务器端口
     * @return 挂载点列表
     */
    public ArrayList<MountPoint> getMountPoint(String ip, int port) {
        return getMountPoint(ip, port, 20000);
    }

    /**
     * 获取挂载点（耗时操作）
     *
     * @param ip      服务器地址
     * @param port    服务器端口
     * @param timeOut 等待的毫秒值（超出时长，则返回空），建议 >=20000
     * @return 挂载点列表
     */
    public ArrayList<MountPoint> getMountPoint(String ip, int port, long timeOut) {
        ArrayList<MountPoint> mountPoints = null;

        TcpComm comm = new TcpComm(ip, port);
        try {
            // 连接服务器
            boolean isConnect = comm.open();
            if (!isConnect) {
                return null;
            }

            // 发送命令获取源节点
            String cmd = "GET / HTTP/1.1\r\nUser-Agent: NTRIP XXXXXX\r\nAccept: */*\r\nConnection: close\r\n\r\n";
            comm.write(cmd);

            ArrayList<Byte> buffer = new ArrayList<>(4096);
            double d = System.currentTimeMillis();
            // 搜索并解析返回结果
            while (System.currentTimeMillis() - d < timeOut) {
                byte[] newData = new byte[4096];
                // 网络数据需要先判断available，如果大于0，再read，否则会卡住
                int count = comm.available();
                if (count < 1) {
                    continue;
                }

                count = comm.read(newData, 0, newData.length);

                buffer.addAll(Helper.toList(Arrays.copyOfRange(newData, 0, count)));
                String nowData = new String(Helper.toByteArray(buffer), "GB2312");

                if (nowData.indexOf("SOURCETABLE 200 OK") != -1) {
                    if (nowData.indexOf("ENDSOURCETABLE") != -1) {

                        // 暂时先这样处理，比正则表达式来的快
                        String[] item1 = nowData.split("\r\n");
                        mountPoints = new ArrayList<>();
                        for (int i = 0; i < item1.length; i++) {
                            String[] item2 = item1[i].split(";");
                            if (item2 == null || item2.length < 4) {
                                continue;
                            }

                            try {
                                MountPoint node = new MountPoint();
                                int len = item2.length;
                                node.MountPoint = item2[1];
                                node.Identifier = item2[2];
                                node.RefType = item2[3];
                                if (len > 4) {
                                    node.Description = item2[4];
                                }
                                if (len > 5) {
                                    node.Carrier = item2[5];
                                }
                                if (len > 6) {
                                    node.GNSS = item2[6];
                                }
                                if (len > 7) {
                                    node.Network = item2[7];
                                }
                                if (len > 8) {
                                    node.Country = item2[8];
                                }
                                if (len > 9) {
                                    node.Latitude = item2[9];
                                    node.B = Math.toRadians(Helper.toDouble(item2[9]));
                                }
                                if (len > 10) {
                                    node.Longitude = item2[10];
                                    node.L = Math.toRadians(Helper.toDouble(item2[10]));
                                }
                                if (len > 11) {
                                    node.NMEASend = item2[11];
                                }
                                if (len > 12) {
                                    node.Solution = item2[12];
                                }
                                if (len > 13) {
                                    node.Generator = item2[13];
                                }
                                if (len > 14) {
                                    node.Compression = item2[14];
                                }
                                if (len > 15) {
                                    node.Authentication = item2[15];
                                }
                                if (len > 16) {
                                    node.Fee = item2[16];
                                }
                                if (len > 17) {
                                    node.BitRate = item2[17];
                                }

                                mountPoints.add(node);
                            } catch (Exception e) {
                                continue;
                            }
                        }
                        break;
                    }
                }
            }

            return mountPoints;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 确保连接断开
            comm.close();
        }
        return null;
    }

    /**
     * 登录服务器
     */
    private void connectServer() {
        try {

            // 登录服务器
            // 发送登录的数据
            writeToNet(mHead.getBytes());
            while (true) {
                LogMessage("循环");
                Thread.sleep(300);
                byte[] buff = readFromNet();
                // 验证登陆
                if (parseNetworkDataStream(buff)) {//登录成功
                    // 开始读数据
                    currentDiffTime = System.currentTimeMillis();
                    // 读数据
                    byte[] readBuff = readFromNet();
                    if (readBuff != null && readBuff.length > 0) {
                        currentDiffTime = System.currentTimeMillis();
                        for (IDataListener listener : dataListenerList) {
                            listener.onReceived(readBuff);
                        }
                    } else {
                        // 判断是否超时，超时自动重连
                        boolean isTimeout = System.currentTimeMillis() - currentDiffTime > TIME_OUT;
                        if (isTimeout) {
                            disconnect();
                            // 重连
                            // 连接并登录服务器
                            connectServer();
                        }
                    }
                    return;
                } else {
                    Thread.sleep(TIME_OUT);
                    // 登录失败，用户被占用
                    disconnect();
                    return;
                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 从服务器接收数据
     */
    private byte[] readFromNet() {
        try {
            synchronized (readLock) {
                int sum = comm.available();
                if (sum > 0) {
                    byte[] reData = new byte[sum];
                    int result = comm.read(reData, 0, reData.length);
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
     * 向服务器发送数据
     */
    private void writeToNet(byte[] data) {
        try {
            // 连接服务器
            comm = new TcpComm(ip, port);
            if (comm.open() == false) {
                LogMessage("连接服务器错误");
                return;
            }

            if (isConnected() == false) {
                LogMessage("连接服务器错误");
                return;
            }
            synchronized (writeLock) {
                comm.write(data);
            }
        } catch (Exception ex) {
            disconnect();
        }
    }

    /**
     * 设置超时自动重连的时间
     *
     * @param TIME_OUT 时间毫秒
     */
    public void setTimeOut(long TIME_OUT) {
        this.TIME_OUT = TIME_OUT;
    }
}
