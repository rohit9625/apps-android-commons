package fr.free.nrw.commons.customselector.model

sealed class Response<T>(val data: T? = null, val error: String? = null) {
    class Loading<T> : Response<T>()
    class Success<T>(data: T) : Response<T>(data)
    class Error<T>(data: T? = null, error: String) : Response<T>(data, error)
}