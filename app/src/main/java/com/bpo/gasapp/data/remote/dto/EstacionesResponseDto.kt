package com.bpo.gasapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of the official Spanish fuel-price API (MITECO/MINETUR).
 * All numeric values arrive as strings with comma decimal separators
 * (e.g. "1,459") and missing values come as empty strings.
 */
@Serializable
data class EstacionesResponseDto(
    @SerialName("Fecha") val fecha: String = "",
    @SerialName("ResultadoConsulta") val resultado: String = "",
    @SerialName("ListaEESSPrecio") val estaciones: List<EstacionDto> = emptyList()
)

@Serializable
data class EstacionDto(
    @SerialName("IDEESS") val id: String = "",
    @SerialName("Rótulo") val rotulo: String = "",
    @SerialName("Latitud") val latitud: String = "",
    @SerialName("Longitud (WGS84)") val longitud: String = "",
    @SerialName("Dirección") val direccion: String = "",
    @SerialName("Localidad") val localidad: String = "",
    @SerialName("Municipio") val municipio: String = "",
    @SerialName("Provincia") val provincia: String = "",
    @SerialName("Horario") val horario: String = "",
    @SerialName("Precio Gasolina 95 E5") val gasolina95: String = "",
    @SerialName("Precio Gasolina 98 E5") val gasolina98: String = "",
    @SerialName("Precio Gasoleo A") val diesel: String = "",
    @SerialName("Precio Gasoleo Premium") val dieselPremium: String = ""
)
