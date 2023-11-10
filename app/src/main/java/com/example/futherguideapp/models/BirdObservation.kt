package com.example.futherguideapp.models

import android.os.Parcel
import android.os.Parcelable

class BirdObservation (
        var comName         : String,
        var sciName         : String,
        var locName         : String,
        var howMany         : Int,
        var lat             : Double,
        var lng             : Double,
        ):Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readString()!!,
                parcel.readString()!!,
                parcel.readString()!!,
                parcel.readInt(),
                parcel.readDouble(),
                parcel.readDouble()
                ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeString(comName)
                parcel.writeString(sciName)
                parcel.writeString(locName)
                parcel.writeInt(howMany)
                parcel.writeDouble(lat)
                parcel.writeDouble(lng)
        }

        override fun describeContents(): Int {
                return 0
        }

        companion object CREATOR : Parcelable.Creator<BirdObservation> {
                override fun createFromParcel(parcel: Parcel): BirdObservation {
                        return BirdObservation(parcel)
                }

                override fun newArray(size: Int): Array<BirdObservation?> {
                        return arrayOfNulls(size)
                }
        }
}