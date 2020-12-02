package com.example.cors.ntrip;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 挂载点
 */
public class MountPoint implements Parcelable {
    /**
     * CORS源节点号或者源节点名称
     */
    public String MountPoint = "";
    /**
     * CORS源节点标识
     */
    public String Identifier = "";
    /**
     * CORS源节点差分格式（电文格式）
     */
    public String RefType = "";
    /**
     * CORS源节点差分格式说明
     */
    public String Description = "";
    /**
     * 载波相位<br />
     * <dl>
     * <dd> 0：No(e.g DGPS) </dd>
     * <dd> 1：Yes L1(e.g RTK )</dd>
     * <dd> 2：Yes L1+L2(e.g RTK) </dd>
     * </dl>
     */
    public String Carrier = "";
    /**
     * 全球导航卫星系统名称
     */
    public String GNSS = "";
    /**
     * 网络名称
     */
    public String Network = "";
    /**
     * 3个字符的国家代码
     */
    public String Country = "";
    /**
     * 源节点近似位置纬度，正方向为北（度，2位小数）
     */
    public String Latitude = "";
    /**
     * 源节点近似位置经度，正方向为东（度，2位小数）
     */
    public String Longitude = "";
    /**
     * 源节点近似位置纬度，单位：弧度
     */
    public double B;
    /**
     * 源节点近似位置经度，单位：弧度
     */
    public double L;
    /**
     * 是否发送NMEA数据<br />
     * <dl>
     * <dd> 0：User must not send nmea to caster </dd>
     * <dd> 1：User nust send nmea to caster </dd>
     * </dl>
     */
    public String NMEASend = "";
    /**
     * 模型<br />
     * <dl>
     * <dd> 0：用户获取到的是单基站数据（User get data from single referenced station) </dd>
     * <dd> 1；用户获取到的是VRS网络差分数据，即虚拟基准站（Data generated from networked referenced stations） </dd>
     * </dl>
     */
    public String Solution = "";
    /**
     * 生成器（是由硬件还是软件产生数据流）
     */
    public String Generator = "";
    /**
     * 压缩算法
     */
    public String Compression = "";
    /**
     * 认证<br />
     * <dl>
     * <dd> N：None（无） </dd>
     * <dd> B：Basic（基础） </dd>
     * <dd> D：Digest（摘要） </dd>
     * </dl>
     */
    public String Authentication = "";
    /**
     * 是否需要付费<br />
     * <dl>
     * <dd> N：No User Fee </dd>
     * <dd> Y：Usage is charged </dd>
     * </dl>
     */
    public String Fee = "";
    /**
     * 波特率
     */
    public String BitRate = "";

    public MountPoint() { }

    protected MountPoint(Parcel in) {
        MountPoint = in.readString();
        Identifier = in.readString();
        RefType = in.readString();
        Description = in.readString();
        Carrier = in.readString();
        GNSS = in.readString();
        Network = in.readString();
        Country = in.readString();
        Latitude = in.readString();
        Longitude = in.readString();
        B = in.readDouble();
        L = in.readDouble();
        NMEASend = in.readString();
        Solution = in.readString();
        Generator = in.readString();
        Compression = in.readString();
        Authentication = in.readString();
        Fee = in.readString();
        BitRate = in.readString();
    }

    public static final Creator<MountPoint> CREATOR = new Creator<MountPoint>() {
        @Override
        public MountPoint createFromParcel(Parcel in) {
            return new MountPoint(in);
        }

        @Override
        public MountPoint[] newArray(int size) {
            return new MountPoint[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(MountPoint);
        parcel.writeString(Identifier);
        parcel.writeString(RefType);
        parcel.writeString(Description);
        parcel.writeString(Carrier);
        parcel.writeString(GNSS);
        parcel.writeString(Network);
        parcel.writeString(Country);
        parcel.writeString(Latitude);
        parcel.writeString(Longitude);
        parcel.writeDouble(B);
        parcel.writeDouble(L);
        parcel.writeString(NMEASend);
        parcel.writeString(Solution);
        parcel.writeString(Generator);
        parcel.writeString(Compression);
        parcel.writeString(Authentication);
        parcel.writeString(Fee);
        parcel.writeString(BitRate);
    }
}