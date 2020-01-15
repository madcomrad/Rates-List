package com.mad.network

import com.google.gson.annotations.SerializedName

data class RatesModel(@SerializedName("base") val base: String,
                      @SerializedName("date") val date: String,
                      @SerializedName("rates") val rates: Map<String, Float>)