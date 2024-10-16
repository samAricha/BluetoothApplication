package com.teka.bluetoothapplication.bluetooth_module

import android.os.Parcel
import android.os.Parcelable

data class BtDeviceModel(
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

    companion object CREATOR : Parcelable.Creator<BtDeviceModel> {
        override fun createFromParcel(parcel: Parcel): BtDeviceModel {
            return BtDeviceModel(parcel)
        }

        override fun newArray(size: Int): Array<BtDeviceModel?> {
            return arrayOfNulls(size)
        }
    }
}
