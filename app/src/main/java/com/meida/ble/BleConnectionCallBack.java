package com.meida.ble;

import android.bluetooth.BluetoothGattCharacteristic;

public interface BleConnectionCallBack {

    void onRecive(BluetoothGattCharacteristic data_char);

    void onSuccessSend();

    void onDisconnect();

}
