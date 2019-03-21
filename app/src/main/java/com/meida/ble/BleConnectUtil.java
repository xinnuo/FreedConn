package com.meida.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.UUID;

public class BleConnectUtil {

    /**********开始连接Service**********/
    private String serviceUuidStr, writeCharactUuid, notifyCharactUuid;
    public static BluetoothGattCharacteristic mBluetoothGattCharacteristic, mBluetoothGattCharacteristicNotify;

    public static BluetoothAdapter mBluetoothAdapter;
    public BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice device;

    public String mDeviceAddress = "";
    public boolean mConnected;
    /**
     * 操作间要有至少15ms的间隔
     */
    private static final int DELAY_TIME = 15;
    private Activity context;
    private BleDeviceCallBack mBleCallBack;

    Handler handler = new Handler();

    public BleConnectUtil(Activity context) {
        this.context = context;
        init();
    }

    private void init() {
        //服务uuid
        serviceUuidStr = "0000fff0-0000-1000-8000-00805f9b34fb";

        //写通道 uuid
        writeCharactUuid = "0000fff3-0000-1000-8000-00805f9b34fb";
        //通知通道 uuid
        notifyCharactUuid = "0000fff2-0000-1000-8000-00805f9b34fb";

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mConnected = false;
    }

    /**
     * 判断是否可见
     */
    public void bluetoothIsAble(BleDeviceCallBack mBleCallBack) {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            context.startActivity(enable);
        } else {
            this.mBleCallBack = mBleCallBack;
            scanLeDevice();
        }
    }


    /**
     * 搜索Ble设备
     */
    public void scanLeDevice() {
        if (mBluetoothAdapter == null) return;
        //扫描所有设备
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * 停止扫描
     */
    public void stopScan() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 蓝牙扫描回调接口
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() == null) return;
            mBleCallBack.callbleBack(device);
        }
    };

    public void connectBle(BluetoothDevice device) {
        //获取所需地址
        mDeviceAddress = device.getAddress();
        new connectThread().start();
    }

    /**
     * 连接并且读取数据线程
     */
    private class connectThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                //连接
                if (!connect(mDeviceAddress, 10000, 2)) {
                    disConnect();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            EventBus.getDefault().postSticky(new EventMsg(Constants.BLE_CONNECTION_FINISH_MSG));
                        }
                    });
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public String last_mac = "";

    // 连接设备
    public boolean connect(String mac, int sectime, int reset_times) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }
        disConnect();
        for (int i = 0; i < reset_times; i++) {
            initTimeFlag(WORK_onServicesDiscovered);

            if ((mBluetoothGatt != null) && mac.equals(last_mac)) {
                // 当前已经连接好了
                if (mConnected == true) {
                    return true;
                }
                mBluetoothGatt.connect();
            } else {
                disConnect(); // 新设备进行连接
                device = mBluetoothAdapter.getRemoteDevice(mac);
                if (device == null) return false;

                mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
            }

            // 连接超时
            if (startTimeOut(sectime)) {
                disConnect();
                continue;
            }

            mConnected = true;
            last_mac = mac;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    // 断开连接
    public boolean disConnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mConnected = false;
            mDeviceAddress = "";
            return true;
        }
        return false;
    }

    /**
     * 销毁连接
     */
    public void close() {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 查看连接状态
     */
    public boolean isConnected() {
        return mConnected;
    }

    /**
     * BLE回调操作
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // 连接成功
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                if (work_witch == WORK_onConnectionStateChange) {
                    work_ok_flag = true;
                }
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mConnected) {
                    mConnected = false;
                }
                EventBus.getDefault().postSticky(Constants.BLE_CONNECTION_FINISH_MSG);
                if (callback != null) {
                    callback.onDisconnect();
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("onServicesDiscovered");
                if (work_witch == WORK_onServicesDiscovered) {
                    work_ok_flag = true;
                }
                //发现设备，遍历服务，初始化特征
                initBLE(gatt);
            } else {
                System.out.println("onServicesDiscovered fail-->" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (work_witch == WORK_onCharacteristicRead) {
                    work_ok_flag = true;
                }
                if (callback != null) {
                    callback.onRecive(characteristic);
                }
            } else {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (work_witch == WORK_onCharacteristicChanged) {
                work_ok_flag = true;
            }

            if (callback != null) {
                callback.onRecive(characteristic);
            }
        }

        /**
         * 收到BLE终端写入数据回调
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (work_witch == WORK_onCharacteristicWrite) {
                    work_ok_flag = true;
                }
                if (callback != null) {
                    callback.onSuccessSend();
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (work_witch == WORK_onDescriptorWrite) {
                    work_ok_flag = true;
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (work_witch == WORK_onReadRemoteRssi) {
                    work_ok_flag = true;
                }
                rssi_value = (rssi_value + rssi) / 2;
                // rssi_value = rssi;
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if ((status == BluetoothGatt.GATT_SUCCESS)
                    && (work_witch == WORK_onDescriptorRead)) {
                work_ok_flag = true;
            }
        }
    };

    //初始化特征
    public void initBLE(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        //遍历所有服务
        for (BluetoothGattService BluetoothGattService : gatt.getServices()) {
            //遍历所有特征
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : BluetoothGattService.getCharacteristics()) {
                String str = bluetoothGattCharacteristic.getUuid().toString();
                if (str.equals(writeCharactUuid)) {
                    mBluetoothGattCharacteristic = bluetoothGattCharacteristic;
                } else if (str.equals(notifyCharactUuid)) {
                    mBluetoothGattCharacteristicNotify = bluetoothGattCharacteristic;
                }
            }
        }
        //判断是否获取到特征
        if ((null == mBluetoothGattCharacteristic) || (null == mBluetoothGattCharacteristicNotify)) {
            //连接失败
            mConnected = false;
        } else {
            setNotify(mBluetoothGattCharacteristicNotify);
            setEnableNotify(mBluetoothGattCharacteristicNotify, true);
            //连接成功
            mConnected = true;
            try {
                // 刚刚使能上需要等待一下才能
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        EventBus.getDefault().postSticky(new EventMsg(Constants.BLE_CONNECTION_FINISH_MSG));
    }

    public List<BluetoothGattService> getServiceList() {
        return mBluetoothGatt.getServices();
    }

    /**
     * 设置可通知
     */
    public boolean setNotify(BluetoothGattCharacteristic data_char) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        if (data_char == null) {
            return false;
        }

        if (!isConnected()) {
            return false;
        }

        // 查看是否带有可通知属性
        if (0 != (data_char.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            mBluetoothGatt.setCharacteristicNotification(data_char, true);
            BluetoothGattDescriptor descriptor = data_char.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        } else if (0 != (data_char.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
            mBluetoothGatt.setCharacteristicNotification(data_char, true);
            BluetoothGattDescriptor descriptor = data_char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        return true;
    }

    /**
     * 设置允许通知
     */
    public boolean setEnableNotify(BluetoothGattCharacteristic data_char, boolean enable) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        if (data_char == null) {
            return false;
        }

        if (!isConnected()) {
            return false;
        }
        mBluetoothGatt.setCharacteristicNotification(data_char, enable);

        return true;
    }

    /**
     * 读取信息
     */
    public boolean readData(BluetoothGattCharacteristic data_char) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        if (data_char == null) {
            return false;
        }

        if (!isConnected()) {
            return false;
        }

        mBluetoothGatt.readCharacteristic(data_char);

        return true;
    }

    /**
     * 发送并带返回的命令
     */
    public boolean sendCmd(BluetoothGattCharacteristic data_char, int milsec) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        if (data_char == null) {
            return false;
        }

        if (!isConnected()) {
            return false;
        }

        initTimeFlag(WORK_onCharacteristicChanged);

        mBluetoothGatt.setCharacteristicNotification(data_char, true);
        mBluetoothGatt.writeCharacteristic(data_char);

        if (startTimeOut(milsec)) {
            System.out.println("startTimeOut");
            return false;
        }

        mBluetoothGatt.setCharacteristicNotification(data_char, false);

        try { // 发送数据一定要有一些延迟
            Thread.sleep(DELAY_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    // 发送数据
    public boolean sendData(BluetoothGattCharacteristic data_char) {
        if (!mBluetoothAdapter.isEnabled()) {
            // 没有打开蓝牙
            return false;
        }

        if (data_char == null) {
            return false;
        }

        if (!isConnected()) {
            return false;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.setCharacteristicNotification(data_char, true);
            mBluetoothGatt.writeCharacteristic(data_char);
        } else {
            return false;
        }
        try { // 发送数据一定要有一些延迟
            Thread.sleep(DELAY_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 读属性值
     */
    public boolean readDescriptor(BluetoothGattDescriptor descriptor, int milsec) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        if (descriptor == null) {
            return false;
        }

        if (!isConnected()) {
            return false;
        }

        initTimeFlag(WORK_onDescriptorRead);

        mBluetoothGatt.readDescriptor(descriptor);

        if (startTimeOut(milsec)) {
            return false;
        }

        try { // 发送数据一定要有一些延迟
            Thread.sleep(DELAY_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 读取
     */
    private int rssi_value;

    public int getRssi(int milsec) {
        // 没有打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            return 0;
        }
        initTimeFlag(WORK_onReadRemoteRssi);

        mBluetoothGatt.readRemoteRssi();

        if (startTimeOut(milsec)) {
            return 0;
        }

        return rssi_value;
    }

    // 回调方法
    private BleConnectionCallBack callback;

    // 设置回调
    public void setCallback(BleConnectionCallBack callback) {
        this.callback = callback;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    // 初始化定时变量
    private int work_witch = 0;
    private final int WORK_onConnectionStateChange = 1;
    private final int WORK_onServicesDiscovered = 2;
    private final int WORK_onCharacteristicRead = 4;
    private final int WORK_onCharacteristicChanged = 5;
    private final int WORK_onCharacteristicWrite = 6;
    private final int WORK_onDescriptorWrite = 7;
    private final int WORK_onReadRemoteRssi = 8;
    private final int WORK_onDescriptorRead = 9;

    private void initTimeFlag(int work_index) {
        work_witch = work_index;
        timeout_flag = false;
        work_ok_flag = false;
    }

    // 开始计时
    private boolean startTimeOut(int minsec) {
        handl.sendEmptyMessageDelayed(HANDLE_TIMEOUT, minsec);
        while (!work_ok_flag) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (exit_flag) {
                return true;
            }
            if (timeout_flag) {
                return true;
            }
        }
        handl.removeMessages(HANDLE_TIMEOUT);

        return false;
    }

    // 强制退出
    private boolean exit_flag = false;

    public void exit() {
        disConnect();
        handl.removeMessages(HANDLE_TIMEOUT);
        exit_flag = true;
    }

    // 事件处理
    private static final int HANDLE_TIMEOUT = 0;
    private boolean timeout_flag = false;
    private boolean work_ok_flag = false;

    @SuppressLint("HandlerLeak")
    private Handler handl = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLE_TIMEOUT) timeout_flag = true;
        }
    };

}
