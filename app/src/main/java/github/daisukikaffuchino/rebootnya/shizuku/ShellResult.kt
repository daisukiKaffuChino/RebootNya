package github.daisukikaffuchino.rebootnya.shizuku

import android.os.Parcel
import android.os.Parcelable

data class ShellResult(
    val exitCode: Int,
    val output: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(exitCode)
        parcel.writeString(output)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ShellResult> {
        override fun createFromParcel(parcel: Parcel) = ShellResult(parcel)
        override fun newArray(size: Int) = arrayOfNulls<ShellResult?>(size)
    }
}