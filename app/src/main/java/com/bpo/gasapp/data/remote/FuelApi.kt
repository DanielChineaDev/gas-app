package com.bpo.gasapp.data.remote

import com.bpo.gasapp.data.remote.dto.EstacionesResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface FuelApi {

    /** All stations in Spain in a single call (~12k rows). */
    @GET("EstacionesTerrestres/")
    suspend fun getAllStations(): EstacionesResponseDto

    /** Stations filtered by province id (IDProvincia), lighter payload. */
    @GET("EstacionesTerrestres/FiltroProvincia/{idProvincia}")
    suspend fun getStationsByProvince(@Path("idProvincia") idProvincia: String): EstacionesResponseDto

    companion object {
        const val BASE_URL =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/"
    }
}
