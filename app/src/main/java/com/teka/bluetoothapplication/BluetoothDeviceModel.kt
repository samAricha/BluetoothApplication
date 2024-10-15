package com.teka.bluetoothapplication

import android.os.Parcel
import android.os.Parcelable

data class BluetoothDeviceModel(
    val name: String?,
    val address: String
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(address)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BluetoothDeviceModel> {
        override fun createFromParcel(parcel: Parcel): BluetoothDeviceModel {
            return BluetoothDeviceModel(parcel)
        }

        override fun newArray(size: Int): Array<BluetoothDeviceModel?> {
            return arrayOfNulls(size)
        }
    }
}
