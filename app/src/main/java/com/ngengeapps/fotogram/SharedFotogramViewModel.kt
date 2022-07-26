package com.ngengeapps.fotogram

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SharedFotogramViewModel(app:Application):AndroidViewModel(app) {
    private var _primaryBitmap:MutableLiveData<Bitmap?> = MutableLiveData()
    val primaryBitmap:LiveData<Bitmap?> get() = _primaryBitmap

    fun loadBitmap(bitmap:Bitmap?){
        _primaryBitmap.value = bitmap
    }


}