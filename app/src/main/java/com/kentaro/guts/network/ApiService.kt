package com.kentaro.guts.network

import com.kentaro.guts.data.PasswordAuthRequest
import com.kentaro.guts.data.PasswordValidationResponse
import com.kentaro.guts.data.UserValidationResponse
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import okhttp3.ResponseBody

interface ApiService {
    
    @FormUrlEncoded
    @POST("accounts/p/40-10002227248/signin/v2/lookup/{username}")
    suspend fun validateUser(
        @Path("username") username: String,
        @Header("accept") accept: String = "*/*",
        @Header("accept-language") acceptLanguage: String = "en-US,en;q=0.9",
        @Header("content-type") contentType: String = "application/x-www-form-urlencoded;charset=UTF-8",
        @Header("sec-ch-ua") secChUa: String = "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"",
        @Header("sec-ch-ua-mobile") secChUaMobile: String = "?1",
        @Header("sec-ch-ua-platform") secChUaPlatform: String = "\"Android\"",
        @Header("sec-fetch-dest") secFetchDest: String = "empty",
        @Header("sec-fetch-mode") secFetchMode: String = "cors",
        @Header("sec-fetch-site") secFetchSite: String = "same-origin",
        @Header("x-zcsrf-token") xZcsrfToken: String = "iamcsrcoo=3dea6395-0540-44ea-8de7-544256dd7549",
        @Header("cookie") cookie: String,
        @Header("Referer") referer: String = "https://academia.srmist.edu.in/accounts/p/10002227248/signin?hide_fp=true&servicename=ZohoCreator&service_language=en&css_url=/49910842/academia-academic-services/downloadPortalCustomCss/login&dcc=true&serviceurl=https%3A%2F%2Facademia.srmist.edu.in%2Fportal%2Facademia-academic-services%2FredirectFromLogin",
        @Header("Referrer-Policy") referrerPolicy: String = "strict-origin-when-cross-origin",
        @Field("mode") mode: String = "primary",
        @Field("cli_time") cliTime: Long = System.currentTimeMillis(),
        @Field("servicename") servicename: String = "ZohoCreator",
        @Field("service_language") serviceLanguage: String = "en",
        @Field("serviceurl") serviceurl: String = "https%3A%2F%2Facademia.srmist.edu.in%2Fportal%2Facademia-academic-services%2FredirectFromLogin"
    ): Response<UserValidationResponse>

    @POST("accounts/p/40-10002227248/signin/v2/primary/{identifier}/password")
    suspend fun validatePassword(
        @Path("identifier") identifier: String,
        @Query("digest") digest: String,
        @Query("cli_time") cliTime: Long,
        @Query("servicename") serviceName: String,
        @Query("service_language") serviceLanguage: String,
        @Query("serviceurl") serviceUrl: String,
        @Header("accept") accept: String,
        @Header("accept-language") acceptLanguage: String,
        @Header("content-type") contentType: String,
        @Header("sec-fetch-dest") secFetchDest: String,
        @Header("sec-fetch-mode") secFetchMode: String,
        @Header("sec-fetch-site") secFetchSite: String,
        @Header("x-zcsrf-token") xZcsrfToken: String,
        @Header("cookie") cookie: String,
        @Header("Referer") referer: String,
        @Header("Referrer-Policy") referrerPolicy: String,
        @Body body: RequestBody
    ): Response<PasswordValidationResponse>

    @GET("srm_university/academia-academic-services/page/My_Attendance")
    suspend fun fetchAttendance(
        @Header("accept") accept: String = "*/*",
        @Header("accept-language") acceptLanguage: String = "en-US,en;q=0.9",
        @Header("content-type") contentType: String = "application/x-www-form-urlencoded; charset=UTF-8",
        @Header("sec-fetch-dest") secFetchDest: String = "empty",
        @Header("sec-fetch-mode") secFetchMode: String = "cors",
        @Header("sec-fetch-site") secFetchSite: String = "same-origin",
        @Header("x-requested-with") xRequestedWith: String = "XMLHttpRequest",
        @Header("cookie") cookie: String,
        @Header("Referer") referer: String = "https://academia.srmist.edu.in/",
        @Header("Referrer-Policy") referrerPolicy: String = "strict-origin-when-cross-origin"
    ): Response<okhttp3.ResponseBody>

    @GET
    suspend fun fetchCalendar(
        @Url url: String,
        @Header("accept") accept: String = "*/*",
        @Header("accept-language") acceptLanguage: String = "en-US,en;q=0.9",
        @Header("content-type") contentType: String = "application/x-www-form-urlencoded; charset=UTF-8",
        @Header("sec-ch-ua") secChUa: String = "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"",
        @Header("sec-ch-ua-mobile") secChUaMobile: String = "?1",
        @Header("sec-ch-ua-platform") secChUaPlatform: String = "\"Android\"",
        @Header("sec-fetch-dest") secFetchDest: String = "empty",
        @Header("sec-fetch-mode") secFetchMode: String = "cors",
        @Header("sec-fetch-site") secFetchSite: String = "same-origin",
        @Header("x-requested-with") xRequestedWith: String = "XMLHttpRequest",
        @Header("cookie") cookie: String,
        @Header("Referer") referer: String = "https://academia.srmist.edu.in/",
        @Header("Referrer-Policy") referrerPolicy: String = "strict-origin-when-cross-origin"
    ): Response<okhttp3.ResponseBody>

    @GET
    suspend fun fetchCourseDetails(
        @Url url: String,
        @Header("accept") accept: String = "*/*",
        @Header("accept-language") acceptLanguage: String = "en-US,en;q=0.9",
        @Header("content-type") contentType: String = "application/x-www-form-urlencoded; charset=UTF-8",
        @Header("sec-ch-ua") secChUa: String = "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"",
        @Header("sec-ch-ua-mobile") secChUaMobile: String = "?1",
        @Header("sec-ch-ua-platform") secChUaPlatform: String = "\"Android\"",
        @Header("sec-fetch-dest") secFetchDest: String = "empty",
        @Header("sec-fetch-mode") secFetchMode: String = "cors",
        @Header("sec-fetch-site") secFetchSite: String = "same-origin",
        @Header("x-requested-with") xRequestedWith: String = "XMLHttpRequest",
        @Header("cookie") cookie: String,
        @Header("Referer") referer: String = "https://academia.srmist.edu.in/",
        @Header("Referrer-Policy") referrerPolicy: String = "strict-origin-when-cross-origin"
    ): Response<okhttp3.ResponseBody>
}

data class PasswordAuthRequest(
    val passwordauth: PasswordAuth
)

data class PasswordAuth(
    val password: String
)