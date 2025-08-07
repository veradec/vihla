package com.kentaro.guts.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.kentaro.guts.data.*
import com.kentaro.guts.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.kentaro.guts.data.TimetableResponse



class AuthRepository(private val context: Context? = null) {
    
    private val gson = Gson()
    private val baseUrl = "https://academia.srmist.edu.in/"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // SharedPreferences for caching
    private val prefs: SharedPreferences? = context?.getSharedPreferences("auth_cache", Context.MODE_PRIVATE)
    private val CACHED_CREDENTIALS_KEY = "cached_credentials"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    private val apiService = retrofit.create(ApiService::class.java)
    
    // Cookies from validateUser.js
    private val userValidationCookies = "zalb_74c3a1eecc=50830239914cba1225506e915a665a91; " +
            "zccpn=68703e7d-ccf0-42ba-92b2-9c87c7a0c8ae; " +
            "JSESSIONID=739937C3826C1F58C37186170B4F4B36; " +
            "cli_rgn=IN; " +
            "_ga=GA1.3.1846200817.1748679237; " +
            "_gid=GA1.3.734795940.1748679237; " +
            "_gat=1; " +
            "_ga_HQWPLLNMKY=GS2.3.s1748679237\$o1\$g0\$t1748679237\$j60\$l0\$h0; " +
            "zalb_f0e8db9d3d=983d6a65b2f29022f18db52385bfc639; " +
            "iamcsr=3dea6395-0540-44ea-8de7-544256dd7549; " +
            "_zcsr_tmp=3dea6395-0540-44ea-8de7-544256dd7549; " +
            "stk=4ec13d42454007681bd4337cf126baec"
    
    // Cookies from validatePassword.js
    private val passwordValidationCookies = "zalb_74c3a1eecc=4cad43ac9848cc7edd20d2313fcde774; " +
            "zccpn=a6fa7bc8-11c7-44ad-8be8-0aa6b04fad8a; " +
            "JSESSIONID=3BD0053672AF3D628D983A15AA469D07; " +
            "cli_rgn=IN; " +
            "_ga=GA1.3.2061081340.1748689001; " +
            "_gid=GA1.3.1677956689.1748689001; " +
            "_ga_HQWPLLNMKY=GS2.3.s1748689001\$o1\$g0\$t1748689001\$j60\$l0\$h0; " +
            "zalb_f0e8db9d3d=7ad3232c36fdd9cc324fb86c2c0a58ad; " +
            "iamcsr=fae2d8fa-e5a1-4cb0-a5ee-cc40af87e89f; " +
            "_zcsr_tmp=fae2d8fa-e5a1-4cb0-a5ee-cc40af87e89f; " +
            "stk=d6559e9a58e77dbea9e24adf3bb57941"
    
    private fun logToFile(message: String, type: String = "INFO") {
        try {
            val timestamp = dateFormat.format(Date())
            val logEntry = "[$timestamp] [$type] $message\n"
            
            // Log to Android logcat
            Log.d("AuthRepository", logEntry.trim())
            
            // Log to file if context is available
            context?.let { ctx ->
                val logFile = File(ctx.filesDir, "auth_logs.txt")
                FileWriter(logFile, true).use { writer ->
                    writer.write(logEntry)
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to write to log file: ${e.message}")
        }
    }
    

    
    // Converted from validateUser.js
    suspend fun validateUser(username: String): UserValidationResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        logToFile("=== USER VALIDATION REQUEST [$requestId] ===")
        logToFile("Username: $username")
        logToFile("Request URL: ${baseUrl}accounts/p/40-10002227248/signin/v2/lookup/$username")
        logToFile("Cookies: $userValidationCookies")
        
        try {
            val response = apiService.validateUser(
                username = username,
                cookie = userValidationCookies
            )
            
            logToFile("Response Code: ${response.code()}")
            logToFile("Response Headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                logToFile("Response Body: ${gson.toJson(responseBody)}")
                
                if (responseBody?.lookup != null) {
                    val data = UserValidationData(
                        identifier = responseBody.lookup.identifier,
                        digest = responseBody.lookup.digest,
                        status_code = responseBody.status_code,
                        message = responseBody.message
                    )
                    
                    logToFile("SUCCESS: User validated successfully")
                    return@withContext UserValidationResult(data = data)
                } else {
                    logToFile("ERROR: Invalid response - missing identifier or digest")
                    return@withContext UserValidationResult(
                        error = "Invalid response: missing identifier or digest"
                    )
                }
            } else {
                logToFile("ERROR: HTTP Error ${response.code()}")
                return@withContext UserValidationResult(
                    error = "HTTP Error: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            logToFile("EXCEPTION: ${e.message}")
            return@withContext UserValidationResult(
                error = "Internal Server Error",
                errorReason = e.message
            )
        } finally {
            logToFile("=== END USER VALIDATION [$requestId] ===\n")
        }
    }

    // Converted from validatePassword.js - exact match
    suspend fun validatePassword(identifier: String, digest: String, password: String): PasswordValidationResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        logToFile("=== PASSWORD VALIDATION REQUEST [$requestId] ===")
        logToFile("Identifier: $identifier")
        logToFile("Digest: $digest")
        logToFile("Password: [HIDDEN]")
        logToFile("Request URL: ${baseUrl}accounts/p/40-10002227248/signin/v2/primary/$identifier/password")
        logToFile("Cookies: $passwordValidationCookies")

        try {
            // Request body
            val body = """{"passwordauth":{"password":"$password"}}"""
            val requestBody = body.toRequestBody("application/json".toMediaType())

            // Headers
            val headers = mapOf(
                "accept" to "*/*",
                "accept-language" to "en-US,en;q=0.9",
                "content-type" to "application/json",
                "sec-fetch-dest" to "empty",
                "sec-fetch-mode" to "cors",
                "sec-fetch-site" to "same-origin",
                "x-zcsrf-token" to "iamcsrcoo=fae2d8fa-e5a1-4cb0-a5ee-cc40af87e89f",
                "cookie" to passwordValidationCookies,
                "Referer" to "https://academia.srmist.edu.in/accounts/p/10002227248/signin?hide_fp=true&servicename=ZohoCreator&service_language=en&css_url=/49910842/academia-academic-services/downloadPortalCustomCss/login&dcc=true&serviceurl=https%3A%2F%2Facademia.srmist.edu.in%2Fportal%2Facademia-academic-services%2FredirectFromLogin",
                "Referrer-Policy" to "strict-origin-when-cross-origin"
            )

            // Query parameters
            val cliTime = System.currentTimeMillis()
            val serviceName = "ZohoCreator"
            val serviceLanguage = "en"
            val serviceUrl = "https://academia.srmist.edu.in/portal/academia-academic-services/redirectFromLogin"

            logToFile("Request Headers: $headers")
            logToFile("Request Body: $body")
            logToFile("CLI Time: $cliTime")

            val response = apiService.validatePassword(
                identifier = identifier,
                digest = digest,
                cliTime = cliTime,
                serviceName = serviceName,
                serviceLanguage = serviceLanguage,
                serviceUrl = serviceUrl,
                accept = headers["accept"]!!,
                acceptLanguage = headers["accept-language"]!!,
                contentType = headers["content-type"]!!,
                secFetchDest = headers["sec-fetch-dest"]!!,
                secFetchMode = headers["sec-fetch-mode"]!!,
                secFetchSite = headers["sec-fetch-site"]!!,
                xZcsrfToken = headers["x-zcsrf-token"]!!,
                cookie = headers["cookie"]!!,
                referer = headers["Referer"]!!,
                referrerPolicy = headers["Referrer-Policy"]!!,
                body = requestBody
            )
            logToFile("Response Code: ${response.code()}")
            logToFile("Response Headers: ${response.headers()}")

            if (response.isSuccessful) {
                val responseBody = response.body()
                logToFile("Raw Response Body: ${gson.toJson(responseBody)}")

                if (responseBody?.status_code == 201) {
                    val setCookieHeader = response.headers()["set-cookie"]
                    logToFile("Raw Set-Cookie Header: $setCookieHeader")
                    logToFile("All Response Headers: ${response.headers()}")

                    if (setCookieHeader != null) {
                        // Handle multiple Set-Cookie headers
                        val setCookieHeaders = response.headers().values("set-cookie")
                        logToFile("All Set-Cookie Headers: $setCookieHeaders")
                        
                        val extractedCookies = StringBuilder()

                        for (header in setCookieHeaders) {
                            // Extract the cookie name and value (before any attributes)
                            val cookieMatch = header.split(";")[0].trim()
                            
                            // Skip cookies that are being cleared (empty value or Max-Age=0)
                            if (cookieMatch.isNotEmpty() && !cookieMatch.endsWith("=")) {
                                if (extractedCookies.isNotEmpty()) {
                                    extractedCookies.append("; ")
                                }
                                extractedCookies.append(cookieMatch)
                            }
                        }

                        val extractedCookiesString = extractedCookies.toString()
                        logToFile("Extracted Valid Cookies: $extractedCookiesString")

                        val data = PasswordValidationData(
                            cookies = extractedCookiesString,
                            statusCode = 201
                        )

                        logToFile("SUCCESS: Authentication successful")
                        logToFile("Status Code: 201")
                        
                        // Save credentials to cache
                        saveCachedCredentials(digest, extractedCookiesString, identifier)
                        
                        logToFile("=== END PASSWORD VALIDATION [$requestId] ===\n")

                        return@withContext PasswordValidationResult(data = data, isAuthenticated = true)
                    } else {
                        logToFile("ERROR: Couldn't get cookie from response header")
                        logToFile("=== END PASSWORD VALIDATION [$requestId] ===\n")

                        return@withContext PasswordValidationResult(
                            error = "Couldn't able to get cookie from response header"
                        )
                    }
                } else {
                    val captchaRequired = responseBody?.localized_message?.lowercase()?.contains("captcha") == true

                    logToFile("Authentication failed")
                    logToFile("Raw Failed Response Body: ${gson.toJson(responseBody)}")
                    logToFile("Status Code: ${responseBody?.status_code}")
                    logToFile("Message: ${responseBody?.localized_message}")
                    logToFile("Captcha Required: $captchaRequired")
                    if (captchaRequired) {
                        logToFile("Captcha Digest: ${responseBody?.cdigest}")
                    }

                    val data = PasswordValidationData(
                        statusCode = responseBody?.status_code ?: 0,
                        message = responseBody?.localized_message,
                        captcha = CaptchaData(
                            required = captchaRequired,
                            digest = if (captchaRequired) responseBody?.cdigest else null
                        )
                    )

                    logToFile("=== END PASSWORD VALIDATION [$requestId] ===\n")

                    return@withContext PasswordValidationResult(data = data, isAuthenticated = false)
                }
            } else {
                logToFile("ERROR: HTTP Error ${response.code()}")
                logToFile("Error Body: ${response.errorBody()?.string()}")
                logToFile("=== END PASSWORD VALIDATION [$requestId] ===\n")

                return@withContext PasswordValidationResult(
                    error = "HTTP Error: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            logToFile("EXCEPTION: ${e.message}")
            logToFile("Stack Trace: ${e.stackTraceToString()}")
            logToFile("=== END PASSWORD VALIDATION [$requestId] ===\n")

            return@withContext PasswordValidationResult(
                error = "Internal Server Error",
                errorReason = e.message
            )
        }
    }
    // Function to get log file content
    fun getLogFileContent(): String {
        return try {
            context?.let { ctx ->
                val logFile = File(ctx.filesDir, "auth_logs.txt")
                if (logFile.exists()) {
                    logFile.readText()
                } else {
                    "No log file found"
                }
            } ?: "Context not available"
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }
    
    // Function to clear log file
    fun clearLogFile() {
        try {
            context?.let { ctx ->
                val logFile = File(ctx.filesDir, "auth_logs.txt")
                if (logFile.exists()) {
                    logFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to clear log file: ${e.message}")
        }
    }
    
    // Fetch attendance function
    suspend fun fetchAttendance(): AttendanceResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        logToFile("=== FETCH ATTENDANCE REQUEST [$requestId] ===")
        
        try {
            // Get cached credentials
            val cachedCredentials = getCachedCredentials()
            if (cachedCredentials == null) {
                logToFile("ERROR: No cached credentials found")
                return@withContext AttendanceResult(
                    error = "No cached credentials found. Please login first."
                )
            }
            
            logToFile("Using cached cookies: ${cachedCredentials.cookies}")
            logToFile("Request URL: ${baseUrl}srm_university/academia-academic-services/page/My_Attendance")
            
            val response = apiService.fetchAttendance(
                cookie = cachedCredentials.cookies
            )
            
            logToFile("Response Code: ${response.code()}")
            logToFile("Response Headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                
                if (responseBody != null) {
                    // Get the response as string (only once)
                    val responseString = responseBody.string()
                    logToFile("Response String length: ${responseString.length}")
                    logToFile("Response String preview: ${responseString.take(500)}...")
                    
                    // Parse the HTML response directly from the raw response body
                    val parsedResult = parseAttendance(responseString)
                    
                    if (parsedResult.error != null) {
                        logToFile("ERROR: Failed to parse attendance data")
                        logToFile("=== END FETCH ATTENDANCE [$requestId] ===\n")
                        return@withContext AttendanceResult(error = parsedResult.error)
                    } else {
                        logToFile("SUCCESS: All tables data parsed successfully")
                        
                        // Use the tables data from the parsed result
                        val jsonData = parsedResult.tablesData
                        logToFile("JSON data length: ${jsonData?.length ?: 0}")
                        logToFile("JSON preview: ${jsonData?.take(200) ?: "null"}...")
                        
                        // Cache the attendance data for 200 seconds
                        if (jsonData != null) {
                            saveCachedAttendanceData(jsonData)
                            
                            // Also cache table 2 (student information) specifically
                            try {
                                val allTablesResult = gson.fromJson(jsonData, AllTablesResult::class.java)
                                if (allTablesResult?.tables != null && allTablesResult.tables.size > 1) {
                                    val table2 = allTablesResult.tables[1]
                                    val table2Json = gson.toJson(table2)
                                    saveCachedTable2Data(table2Json)
                                    logToFile("Table 2 data (student info) cached separately")
                                }
                            } catch (e: Exception) {
                                logToFile("Failed to cache table 2 data: ${e.message}")
                            }
                        }
                        
                        logToFile("=== END FETCH ATTENDANCE [$requestId] ===\n")
                        return@withContext AttendanceResult(data = jsonData)
                    }
                } else {
                    logToFile("ERROR: Empty response body")
                    logToFile("=== END FETCH ATTENDANCE [$requestId] ===\n")
                    return@withContext AttendanceResult(
                        error = "Empty response from server"
                    )
                }
            } else {
                logToFile("ERROR: HTTP Error ${response.code()}")
                logToFile("Error Body: ${response.errorBody()?.string()}")
                logToFile("=== END FETCH ATTENDANCE [$requestId] ===\n")
                return@withContext AttendanceResult(
                    error = "HTTP Error: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            logToFile("EXCEPTION: ${e.message}")
            logToFile("Stack Trace: ${e.stackTraceToString()}")
            logToFile("=== END FETCH ATTENDANCE [$requestId] ===\n")
            return@withContext AttendanceResult(
                error = "Internal Server Error",
                errorReason = e.message
            )
        }
    }
    
    // Caching methods
    fun saveCachedCredentials(digest: String, cookies: String, identifier: String) {
        try {
            val cachedCredentials = CachedCredentials(digest, cookies, identifier)
            val json = gson.toJson(cachedCredentials)
            prefs?.edit()?.putString(CACHED_CREDENTIALS_KEY, json)?.commit()
            logToFile("Cached credentials saved successfully")
        } catch (e: Exception) {
            logToFile("Failed to save cached credentials: ${e.message}")
        }
    }
    
    fun getCachedCredentials(): CachedCredentials? {
        return try {
            val json = prefs?.getString(CACHED_CREDENTIALS_KEY, null)
            if (json != null) {
                gson.fromJson(json, CachedCredentials::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached credentials: ${e.message}")
            null
        }
    }
    
    fun clearCachedCredentials() {
        try {
            prefs?.edit()?.remove(CACHED_CREDENTIALS_KEY)?.apply()
            logToFile("Cached credentials cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached credentials: ${e.message}")
        }
    }
    
    fun hasCachedCredentials(): Boolean {
        return getCachedCredentials() != null
    }
    
    // Cache table data
    fun saveCachedTableData(tableData: String) {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs?.edit()
                ?.putString("cached_table_data", tableData)
                ?.putString("cached_table_data_timestamp", currentDate)
                ?.apply()
            logToFile("Table data cached successfully on $currentDate")
        } catch (e: Exception) {
            logToFile("Failed to save cached table data: ${e.message}")
        }
    }
    
    fun getCachedTableData(): String? {
        return try {
            prefs?.getString("cached_table_data", null)
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached table data: ${e.message}")
            null
        }
    }
    
    fun clearCachedTableData() {
        try {
            prefs?.edit()
                ?.remove("cached_table_data")
                ?.remove("cached_table_data_timestamp")
                ?.apply()
            logToFile("Cached table data cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached table data: ${e.message}")
        }
    }
    
    fun isAttendanceDataCached(): Boolean {
        return try {
            val cachedData = prefs?.getString("cached_table_data", null)
            cachedData != null
        } catch (e: Exception) {
            logToFile("Failed to check if attendance data is cached: ${e.message}")
            false
        }
    }
    
    fun isAttendanceDataCachedToday(): Boolean {
        return try {
            val cachedTimestamp = prefs?.getString("cached_table_data_timestamp", null)
            if (cachedTimestamp == null) return false
            
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = cachedTimestamp == currentDate
            logToFile("Attendance data cached on: $cachedTimestamp, today: $currentDate, isToday: $isToday")
            isToday
        } catch (e: Exception) {
            logToFile("Failed to check if attendance data is cached today: ${e.message}")
            false
        }
    }
    
    // Cache attendance data for 200 seconds
    fun saveCachedAttendanceData(attendanceData: String) {
        try {
            val currentTime = System.currentTimeMillis()
            prefs?.edit()
                ?.putString("cached_attendance_data", attendanceData)
                ?.putLong("cached_attendance_timestamp", currentTime)
                ?.apply()
            logToFile("Attendance data cached successfully at $currentTime")
        } catch (e: Exception) {
            logToFile("Failed to save cached attendance data: ${e.message}")
        }
    }
    
    fun getCachedAttendanceData(): String? {
        return try {
            prefs?.getString("cached_attendance_data", null)
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached attendance data: ${e.message}")
            null
        }
    }
    
    fun isAttendanceDataCachedWithin200s(): Boolean {
        return try {
            val cachedTimestamp = prefs?.getLong("cached_attendance_timestamp", 0L) ?: 0L
            if (cachedTimestamp == 0L) return false
            
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - cachedTimestamp
            val isWithin200s = timeDifference < 200_000L // 200 seconds in milliseconds
            
            logToFile("Attendance data cached at: $cachedTimestamp, current time: $currentTime, difference: ${timeDifference / 1000}s, within 200s: $isWithin200s")
            isWithin200s
        } catch (e: Exception) {
            logToFile("Failed to check if attendance data is cached within 200s: ${e.message}")
            false
        }
    }
    
    fun clearCachedAttendanceData() {
        try {
            prefs?.edit()
                ?.remove("cached_attendance_data")
                ?.remove("cached_attendance_timestamp")
                ?.apply()
            logToFile("Cached attendance data cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached attendance data: ${e.message}")
        }
    }
    
    // Parse attendance HTML response - now extracts all tables
    private fun parseAttendance(response: String): ParsedAttendanceResult {
        return try {
            logToFile("Parsing attendance HTML response")
            logToFile("Response length: ${response.length}")
            logToFile("Response preview: ${response.take(500)}...")
            
            // First try to extract sanitized HTML using regex
            val match = Pattern.compile("pageSanitizer\\.sanitize\\('(.*)'\\);", Pattern.DOTALL).matcher(response)
            if (match.find()) {
                logToFile("Found pageSanitizer pattern - extracting encoded HTML")
                val encodedHtml = match.group(1) ?: ""
                logToFile("Extracted encoded HTML length: ${encodedHtml.length}")
                
                // Decode the HTML
                val decodedHtml = encodedHtml
                    .replace(Regex("\\\\x([0-9A-Fa-f]{2})")) { result ->
                        val hex = result.groupValues[1]
                        hex.toInt(16).toChar().toString()
                    }
                    .replace("\\\\", "")
                    .replace("\\'", "'")

                logToFile("Decoded HTML length: ${decodedHtml.length}")
                
                // Parse with Jsoup and extract all tables
                val doc: Document = Jsoup.parse(decodedHtml)
                return extractAllTables(doc)
            } else {
                logToFile("No pageSanitizer pattern found - parsing raw HTML directly")
                // Parse the raw HTML directly
                val doc: Document = Jsoup.parse(response)
                return extractAllTables(doc)
            }
            
        } catch (error: Exception) {
            logToFile("ERROR parsing attendance: ${error.message}")
            logToFile("Stack trace: ${error.stackTraceToString()}")
            ParsedAttendanceResult(error = "Failed to parse attendance: ${error.message}", status = 500)
        }
    }
    

    
    // Extract all tables from HTML document
    private fun extractAllTables(doc: Document): ParsedAttendanceResult {
        return try {
            logToFile("Extracting all tables from HTML document")
            
            val allTables = doc.select("table")
            logToFile("Found ${allTables.size} tables in the document")
            
            if (allTables.isEmpty()) {
                logToFile("WARNING: No tables found in HTML")
                logToFile("Document title: ${doc.title()}")
                logToFile("Document body preview: ${doc.body()?.text()?.take(200)}...")
                return ParsedAttendanceResult(error = "No tables found in HTML", status = 500)
            }
            
            val tableDataList = mutableListOf<TableData>()
            
            // Extract all tables
            for (tableIndex in allTables.indices) {
                val table = allTables[tableIndex]
                val tableNumber = tableIndex + 1 // 1-based for display
                logToFile("Processing table ${tableNumber} (index ${tableIndex})")
                
                // Extract all data rows (including header row)
                val rows = table.select("tr")
                val dataRows = mutableListOf<List<String>>()
                
                // Include all rows (including headers)
                for (i in 0 until rows.size) {
                    val row = rows[i]
                    val cells = row.select("td, th")
                    val rowData = cells.map { it.text().trim() }
                    
                    if (rowData.isNotEmpty()) {
                        dataRows.add(rowData)
                    }
                }
                
                // Print all rows for this table
                dataRows.forEachIndexed { rowIndex, rowData ->
                    logToFile("Table ${tableNumber} Row ${rowIndex + 1}: $rowData")
                }
                
                val tableData = TableData(
                    tableIndex = tableIndex,
                    headers = emptyList(), // No headers
                    rows = dataRows,
                    tableAttributes = emptyMap() // No attributes
                )
                
                tableDataList.add(tableData)
            }
            
            logToFile("Successfully extracted ${tableDataList.size} tables")
            
            // Log attendance data in the requested format
            logAttendanceData(tableDataList)
            
            // Convert to JSON for the response
            val allTablesResult = AllTablesResult(tables = tableDataList, status = 200)
            val jsonData = gson.toJson(allTablesResult)
            
            logToFile("JSON data length: ${jsonData.length}")
            logToFile("JSON preview: ${jsonData.take(200)}...")
            
            return ParsedAttendanceResult(
                attendance = null,
                tablesData = jsonData,
                status = 200
            )
            
        } catch (error: Exception) {
            logToFile("ERROR extracting tables: ${error.message}")
            logToFile("Stack trace: ${error.stackTraceToString()}")
            return ParsedAttendanceResult(error = "Failed to extract tables: ${error.message}", status = 500)
        }
    }
    
    // Log attendance data in the requested format
    private fun logAttendanceData(tableDataList: List<TableData>) {
        try {
            logToFile("=== ATTENDANCE DATA SUMMARY ===")
            println("=== ATTENDANCE DATA SUMMARY ===")
            
            // Find the attendance table (usually table 3, index 2)
            val attendanceTable = tableDataList.getOrNull(2) // Table 3 (0-based index 2)
            
            if (attendanceTable != null && attendanceTable.rows.size > 1) {
                logToFile("Found attendance table with ${attendanceTable.rows.size} rows")
                println("Found attendance table with ${attendanceTable.rows.size} rows")
                
                // Skip header row and process each course
                attendanceTable.rows.drop(1).forEachIndexed { rowIndex, rowData ->
                    if (rowData.size >= 9) {
                        val courseCode = rowData.getOrNull(0) ?: ""
                        val courseTitle = rowData.getOrNull(1) ?: ""
                        val totalClasses = rowData.getOrNull(6) ?: "0"  // Total classes conducted
                        val hoursAbsent = rowData.getOrNull(7) ?: "0"  // Classes you were absent
                        val attendancePercentage = rowData.getOrNull(8) ?: "0%"
                        
                        // Calculate classes needed for 75% or margin
                        val classesNeededOrMargin = calculateClassesNeededOrMarginCorrected(totalClasses, hoursAbsent)
                        
                        // Debug output to verify calculations
                        val total = totalClasses.toIntOrNull() ?: 0
                        val absent = hoursAbsent.toIntOrNull() ?: 0
                        val present = total - absent
                        val percentage = if (total > 0) (present.toDouble() / total * 100) else 0.0
                        println("DEBUG: $courseCode - Total: $total, Absent: $absent, Present: $present, Percentage: ${String.format("%.2f", percentage)}%")
                        
                        // Format: Subject code - Total Classes - Hours Absent - Hours needed or margin [number]
                        val logEntry = "$courseCode - $totalClasses - $hoursAbsent - $classesNeededOrMargin"
                        logToFile(logEntry)
                        println("ATTENDANCE LOG: $logEntry")
                    }
                }
            } else {
                logToFile("No attendance table found or insufficient data")
                println("No attendance table found or insufficient data")
            }
            
            logToFile("=== END ATTENDANCE DATA SUMMARY ===")
            println("=== END ATTENDANCE DATA SUMMARY ===")
            
        } catch (e: Exception) {
            logToFile("ERROR logging attendance data: ${e.message}")
            println("ERROR logging attendance data: ${e.message}")
        }
    }
    
    // Calculate classes needed for 75% or margin (corrected for total classes vs absent)
    private fun calculateClassesNeededOrMarginCorrected(totalClasses: String, hoursAbsent: String): String {
        return try {
            val total = totalClasses.toIntOrNull() ?: 0
            val absent = hoursAbsent.toIntOrNull() ?: 0
            val present = total - absent
            
            if (total == 0) return "No data"
            
            val currentAttendance = present.toDouble() / total
            
            if (currentAttendance >= 0.75) {
                // Calculate margin (classes you can miss)
                // Formula: (present - 0.75 * total) / 0.25
                val margin = ((present - 0.75 * total) / 0.25).toInt()
                "Margin [${maxOf(0, margin)}]"
            } else {
                // Calculate classes needed
                // We need to find how many more classes YOU need to attend to reach 75%
                // Current: present out of total
                // Target: 75% attendance
                // Required: 0.75 * total classes attended
                val requiredPresent = (0.75 * total).toInt()
                val needed = maxOf(0, requiredPresent - present)
                "Hours needed [${maxOf(1, needed)}]"
            }
        } catch (e: Exception) {
            "Error"
        }
    }
    
    // Calculate classes needed for 75% or margin (old function for backward compatibility)
    private fun calculateClassesNeededOrMargin(hoursPresent: String, hoursAbsent: String): String {
        return try {
            val present = hoursPresent.toIntOrNull() ?: 0
            val absent = hoursAbsent.toIntOrNull() ?: 0
            val totalClasses = present + absent
            
            if (totalClasses == 0) return "No data"
            
            val currentAttendance = present.toDouble() / totalClasses
            
            if (currentAttendance >= 0.75) {
                // Calculate margin (classes you can miss)
                // Formula: (present - 0.75 * total) / 0.25
                val margin = ((present - 0.75 * totalClasses) / 0.25).toInt()
                "Margin [${maxOf(0, margin)}]"
            } else {
                // Calculate classes needed
                // We need to find how many more classes to attend to reach 75%
                // Let x be additional classes needed
                // (present + x) / (total + x) >= 0.75
                // present + x >= 0.75 * (total + x)
                // present + x >= 0.75 * total + 0.75 * x
                // present >= 0.75 * total + 0.75 * x - x
                // present >= 0.75 * total - 0.25 * x
                // 0.25 * x >= 0.75 * total - present
                // x >= (0.75 * total - present) / 0.25
                val needed = ((0.75 * totalClasses - present) / 0.25).toInt()
                "Hours needed [${maxOf(1, needed)}]"
            }
        } catch (e: Exception) {
            "Error"
        }
    }
    
    // Log attendance data in the requested format for legacy AttendanceDetail objects
    fun logAttendanceDataLegacy(attendanceList: List<AttendanceDetail>?) {
        try {
            logToFile("=== LEGACY ATTENDANCE DATA SUMMARY ===")
            println("=== LEGACY ATTENDANCE DATA SUMMARY ===")
            
            if (attendanceList != null && attendanceList.isNotEmpty()) {
                logToFile("Found ${attendanceList.size} courses")
                println("Found ${attendanceList.size} courses")
                
                attendanceList.forEach { course ->
                    val classesNeededOrMargin = if (course.getCurrentAttendancePercentage() >= 75.0) {
                        val margin = course.getMarginFor75Percent()
                        "Margin [$margin]"
                    } else {
                        val needed = course.getClassesNeededFor75Percent()
                        "Hours needed [$needed]"
                    }
                    
                    val logEntry = "${course.courseCode} - ${course.courseConducted} - ${course.courseAbsent} - $classesNeededOrMargin"
                    logToFile(logEntry)
                    println("ATTENDANCE LOG: $logEntry")
                }
            } else {
                logToFile("No attendance data found")
                println("No attendance data found")
            }
            
            logToFile("=== END LEGACY ATTENDANCE DATA SUMMARY ===")
            println("=== END LEGACY ATTENDANCE DATA SUMMARY ===")
            
        } catch (e: Exception) {
            logToFile("ERROR logging legacy attendance data: ${e.message}")
            println("ERROR logging legacy attendance data: ${e.message}")
        }
    }
    
    // Fetch calendar function using dynamic URL
    suspend fun fetchCalendar(): CalendarResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        logToFile("=== FETCH CALENDAR REQUEST [$requestId] ===")
        
        try {
            // Check if calendar data is cached for today
            if (isCalendarDataCachedToday()) {
                logToFile("Calendar data is cached for today - using cached data")
                val cachedData = getCachedCalendarData()
                if (cachedData != null) {
                    val parsedResult = parseCalendar(cachedData)
                    if (parsedResult.error == null) {
                        logToFile("SUCCESS: Using cached calendar data")
                        return@withContext CalendarResult(data = cachedData, parsedData = parsedResult)
                    } else {
                        logToFile("ERROR: Failed to parse cached calendar data")
                    }
                }
            }
            
            // Get cached credentials
            val cachedCredentials = getCachedCredentials()
            if (cachedCredentials == null) {
                logToFile("ERROR: No cached credentials found")
                return@withContext CalendarResult(
                    error = "No cached credentials found. Please login first."
                )
            }
            
            // Generate dynamic URL
            val dynamicUrl = DynamicUrl.calendarDynamicUrl()
            logToFile("Generated calendar URL: $dynamicUrl")
            logToFile("Using cached cookies: ${cachedCredentials.cookies}")
            
            val response = apiService.fetchCalendar(
                url = dynamicUrl,
                cookie = cachedCredentials.cookies
            )
            
            logToFile("Response Code: ${response.code()}")
            logToFile("Response Headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                
                if (responseBody != null) {
                    val responseString = responseBody.string()
                    logToFile("Response String length: ${responseString.length}")
                    logToFile("Response String preview: ${responseString.take(500)}...")
                    
                    // Parse the calendar HTML response
                    val parsedResult = parseCalendar(responseString)
                    
                    if (parsedResult.error != null) {
                        logToFile("ERROR: Failed to parse calendar data")
                        logToFile("=== END FETCH CALENDAR [$requestId] ===\n")
                        return@withContext CalendarResult(error = parsedResult.error)
                    } else {
                        logToFile("SUCCESS: Calendar data parsed successfully")
                        
                        // Cache the calendar data
                        saveCachedCalendarData(responseString)
                        
                        logToFile("=== END FETCH CALENDAR [$requestId] ===\n")
                        return@withContext CalendarResult(data = responseString, parsedData = parsedResult)
                    }
                } else {
                    logToFile("ERROR: Empty response body")
                    logToFile("=== END FETCH CALENDAR [$requestId] ===\n")
                    return@withContext CalendarResult(
                        error = "Empty response from server"
                    )
                }
            } else {
                logToFile("ERROR: HTTP Error ${response.code()}")
                logToFile("Error Body: ${response.errorBody()?.string()}")
                logToFile("=== END FETCH CALENDAR [$requestId] ===\n")
                return@withContext CalendarResult(
                    error = "HTTP Error: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            logToFile("EXCEPTION: ${e.message}")
            logToFile("Stack Trace: ${e.stackTraceToString()}")
            logToFile("=== END FETCH CALENDAR [$requestId] ===\n")
            return@withContext CalendarResult(
                error = "Internal Server Error",
                errorReason = e.message
            )
        }
    }
    
    // Parse calendar HTML response
    private fun parseCalendar(response: String): ParsedCalendarResult {
        return try {
            logToFile("Parsing calendar HTML response")
            logToFile("Response length: ${response.length}")
            logToFile("Response preview: ${response.take(500)}...")
            
            // First try to extract sanitized HTML using regex
            val match = Pattern.compile("pageSanitizer\\.sanitize\\('(.*)'\\);", Pattern.DOTALL).matcher(response)
            if (match.find()) {
                logToFile("Found pageSanitizer pattern - extracting encoded HTML")
                val encodedHtml = match.group(1) ?: ""
                logToFile("Extracted encoded HTML length: ${encodedHtml.length}")
                
                // Decode the HTML
                val decodedHtml = encodedHtml
                    .replace(Regex("\\\\x([0-9A-Fa-f]{2})")) { result ->
                        val hex = result.groupValues[1]
                        hex.toInt(16).toChar().toString()
                    }
                    .replace("\\\\", "")
                    .replace("\\'", "'")

                logToFile("Decoded HTML length: ${decodedHtml.length}")
                
                // Parse with Jsoup
                val doc: Document = Jsoup.parse(decodedHtml)
                return extractCalendarData(doc)
            } else {
                logToFile("No pageSanitizer pattern found - parsing raw HTML directly")
                // Parse the raw HTML directly
                val doc: Document = Jsoup.parse(response)
                return extractCalendarData(doc)
            }
            
        } catch (error: Exception) {
            logToFile("ERROR parsing calendar: ${error.message}")
            logToFile("Stack trace: ${error.stackTraceToString()}")
            ParsedCalendarResult(error = "Failed to parse calendar: ${error.message}", status = 500)
        }
    }
    
    // Extract calendar data from HTML document
    private fun extractCalendarData(doc: Document): ParsedCalendarResult {
        return try {
            logToFile("Extracting calendar data from HTML document")
            logToFile("Document HTML preview: ${doc.html().take(1000)}...")
            
            // Try multiple approaches to find calendar data
            
            // Approach 1: Look for zmlvalue in div.zc-pb-embed-placeholder-content
            val placeholderDiv = doc.select("div.zc-pb-embed-placeholder-content").first()
            if (placeholderDiv != null) {
                val zmlvalue = placeholderDiv.attr("zmlvalue")
                if (zmlvalue.isNotEmpty()) {
                    logToFile("Found zmlvalue with length: ${zmlvalue.length}")
                    val innerDoc: Document = Jsoup.parse(zmlvalue)
                    val result = extractFromInnerDocument(innerDoc)
                    if (result.months != null && result.months.isNotEmpty()) {
                        return result
                    }
                }
            }
            
            // Approach 2: Look for any table with calendar-like structure
            logToFile("Trying direct table extraction")
            val result = extractFromInnerDocument(doc)
            if (result.months != null && result.months.isNotEmpty()) {
                return result
            }
            
            // Approach 3: Look for any div containing calendar data
            logToFile("Trying to find calendar data in any div")
            val allDivs = doc.select("div")
            for (div in allDivs) {
                val divText = div.text()
                if (divText.contains("Aug") || divText.contains("Sep") || divText.contains("Oct") || 
                    divText.contains("Nov") || divText.contains("Dec") || divText.contains("Jan") ||
                    divText.contains("Feb") || divText.contains("Mar") || divText.contains("Apr") ||
                    divText.contains("May") || divText.contains("Jun") || divText.contains("Jul")) {
                    logToFile("Found div with month names: ${divText.take(200)}...")
                    val innerDoc: Document = Jsoup.parse(div.html())
                    val result = extractFromInnerDocument(innerDoc)
                    if (result.months != null && result.months.isNotEmpty()) {
                        return result
                    }
                }
            }
            
            logToFile("ERROR: Could not find calendar data in any format")
            return ParsedCalendarResult(error = "Could not find calendar data", status = 500)
            
        } catch (error: Exception) {
            logToFile("ERROR extracting calendar data: ${error.message}")
            logToFile("Stack trace: ${error.stackTraceToString()}")
            ParsedCalendarResult(error = "Failed to extract calendar data: ${error.message}", status = 500)
        }
    }
    
    // Extract calendar data from inner document
    private fun extractFromInnerDocument(doc: Document): ParsedCalendarResult {
        return try {
            logToFile("Extracting from inner document")
            
            // Find any table that might contain calendar data
            val tables = doc.select("table")
            logToFile("Found ${tables.size} tables")
            
            for (tableIndex in tables.indices) {
                val table = tables[tableIndex]
                logToFile("Analyzing table $tableIndex")
                
                val rows = table.select("tr")
                if (rows.size < 2) continue // Need at least header + data row
                
                val headerRow = rows.firstOrNull()
                if (headerRow == null) continue
                val headerCells = headerRow.select("th, td")
                
                // Look for month names in header
                val monthNames = mutableListOf<String>()
                headerCells.forEach { cell ->
                    val text = cell.text().trim()
                    if (text.isNotEmpty() && text != "Dt" && text != "Day" && 
                        (text.contains("'") || text.matches(Regex(".*\\d{4}.*")))) {
                        monthNames.add(text)
                        logToFile("Found potential month: $text")
                    }
                }
                
                if (monthNames.isNotEmpty()) {
                    logToFile("Found ${monthNames.size} months in table $tableIndex")
                    val monthsData = mutableListOf<MonthData>()
                    
                    // Create month data objects
                    monthNames.forEach { monthName ->
                        monthsData.add(MonthData(month = monthName, days = mutableListOf()))
                    }
                    
                    // Extract day data from remaining rows
                    val dataRows = rows.drop(1)
                    dataRows.forEach { row ->
                        val cells = row.select("td")
                        
                        // Process each month's data
                        monthsData.forEachIndexed { monthIndex, monthData ->
                            val offset = monthIndex * 5 // Assuming 5 columns per month
                            
                            if (offset + 3 < cells.size) {
                                val date = cells.getOrNull(offset + 0)?.text()?.trim() ?: ""
                                val day = cells.getOrNull(offset + 1)?.text()?.trim() ?: ""
                                val event = cells.getOrNull(offset + 2)?.text()?.trim() ?: ""
                                val dayOrder = cells.getOrNull(offset + 3)?.text()?.trim() ?: ""
                                
                                if (date.isNotEmpty() && date.matches(Regex("\\d+"))) {
                                    val dayData = DayData(
                                        date = date,
                                        day = day,
                                        event = event,
                                        dayOrder = dayOrder
                                    )
                                    (monthData.days as MutableList).add(dayData)
                                    logToFile("Added day data for ${monthData.month}: $date $day $event $dayOrder")
                                }
                            }
                        }
                    }
                    
                    if (monthsData.any { it.days.isNotEmpty() }) {
                        logToFile("Successfully extracted calendar data with ${monthsData.size} months")
                        return ParsedCalendarResult(months = monthsData, status = 200)
                    }
                }
            }
            
            logToFile("No valid calendar data found in any table")
            return ParsedCalendarResult(error = "No valid calendar data found", status = 500)
            
        } catch (error: Exception) {
            logToFile("ERROR extracting from inner document: ${error.message}")
            return ParsedCalendarResult(error = "Failed to extract from inner document: ${error.message}", status = 500)
        }
    }
    
    // Fetch course details function using dynamic URL
    suspend fun fetchCourseDetails(): CourseResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        logToFile("=== FETCH COURSE DETAILS REQUEST [$requestId] ===")
        
        try {
            // Get cached credentials
            val cachedCredentials = getCachedCredentials()
            if (cachedCredentials == null) {
                logToFile("ERROR: No cached credentials found")
                return@withContext CourseResult(
                    error = "No cached credentials found. Please login first."
                )
            }
            
            // Generate dynamic URL
            val dynamicUrl = DynamicUrl.courseDynamicUrl()
            logToFile("Generated course URL: $dynamicUrl")
            logToFile("Using cached cookies: ${cachedCredentials.cookies}")
            
            val response = apiService.fetchCourseDetails(
                url = dynamicUrl,
                cookie = cachedCredentials.cookies
            )
            
            logToFile("Response Code: ${response.code()}")
            logToFile("Response Headers: ${response.headers()}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                
                if (responseBody != null) {
                    val responseString = responseBody.string()
                    logToFile("Response String length: ${responseString.length}")
                    logToFile("Response String preview: ${responseString.take(500)}...")
                    
                    // Parse the HTML response to extract all tables
                    val parsedResult = parseCourseTables(responseString)
                    
                    if (parsedResult.error != null) {
                        logToFile("ERROR: Failed to parse course data")
                        logToFile("=== END FETCH COURSE DETAILS [$requestId] ===\n")
                        return@withContext CourseResult(error = parsedResult.error)
                    } else {
                        logToFile("SUCCESS: All course tables parsed successfully")
                        
                        // Use the tables data from the parsed result
                        val jsonData = parsedResult.tablesData
                        logToFile("JSON data length: ${jsonData?.length ?: 0}")
                        logToFile("JSON preview: ${jsonData?.take(200) ?: "null"}...")
                        
                        // Cache the parsed table data
                        if (jsonData != null) {
                            saveCachedCourseData(jsonData)
                        }
                        
                        logToFile("=== END FETCH COURSE DETAILS [$requestId] ===\n")
                        return@withContext CourseResult(data = jsonData)
                    }
                } else {
                    logToFile("ERROR: Empty response body")
                    logToFile("=== END FETCH COURSE DETAILS [$requestId] ===\n")
                    return@withContext CourseResult(
                        error = "Empty response from server"
                    )
                }
            } else {
                logToFile("ERROR: HTTP Error ${response.code()}")
                logToFile("Error Body: ${response.errorBody()?.string()}")
                logToFile("=== END FETCH COURSE DETAILS [$requestId] ===\n")
                return@withContext CourseResult(
                    error = "HTTP Error: ${response.code()}"
                )
            }
        } catch (e: Exception) {
            logToFile("EXCEPTION: ${e.message}")
            logToFile("Stack Trace: ${e.stackTraceToString()}")
            logToFile("=== END FETCH COURSE DETAILS [$requestId] ===\n")
            return@withContext CourseResult(
                error = "Internal Server Error",
                errorReason = e.message
            )
        }
    }
    
    // Cache calendar data with daily timestamp
    fun saveCachedCalendarData(calendarData: String) {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs?.edit()
                ?.putString("cached_calendar_data", calendarData)
                ?.putString("cached_calendar_data_timestamp", currentDate)
                ?.apply()
            logToFile("Calendar data cached successfully on $currentDate")
        } catch (e: Exception) {
            logToFile("Failed to save cached calendar data: ${e.message}")
        }
    }
    
    fun getCachedCalendarData(): String? {
        return try {
            prefs?.getString("cached_calendar_data", null)
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached calendar data: ${e.message}")
            null
        }
    }
    
    fun isCalendarDataCachedToday(): Boolean {
        return try {
            val cachedTimestamp = prefs?.getString("cached_calendar_data_timestamp", null)
            if (cachedTimestamp == null) return false
            
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = cachedTimestamp == currentDate
            logToFile("Calendar data cached on: $cachedTimestamp, today: $currentDate, isToday: $isToday")
            isToday
        } catch (e: Exception) {
            logToFile("Failed to check if calendar data is cached today: ${e.message}")
            false
        }
    }
    
    // Cache course data
    fun saveCachedCourseData(courseData: String) {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs?.edit()
                ?.putString("cached_course_data", courseData)
                ?.putString("cached_course_data_timestamp", currentDate)
                ?.apply()
            logToFile("Course data cached successfully on $currentDate")
        } catch (e: Exception) {
            logToFile("Failed to save cached course data: ${e.message}")
        }
    }
    
    fun getCachedCourseData(): String? {
        return try {
            prefs?.getString("cached_course_data", null)
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached course data: ${e.message}")
            null
        }
    }
    
    fun clearCachedCourseData() {
        try {
            prefs?.edit()
                ?.remove("cached_course_data")
                ?.remove("cached_course_data_timestamp")
                ?.apply()
            logToFile("Cached course data cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached course data: ${e.message}")
        }
    }
    
    fun isCourseDataCached(): Boolean {
        return try {
            val cachedData = prefs?.getString("cached_course_data", null)
            cachedData != null
        } catch (e: Exception) {
            logToFile("Failed to check if course data is cached: ${e.message}")
            false
        }
    }
    
    fun isCourseDataCachedToday(): Boolean {
        return try {
            val cachedTimestamp = prefs?.getString("cached_course_data_timestamp", null)
            if (cachedTimestamp == null) return false
            
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = cachedTimestamp == currentDate
            logToFile("Course data cached on: $cachedTimestamp, today: $currentDate, isToday: $isToday")
            isToday
        } catch (e: Exception) {
            logToFile("Failed to check if course data is cached today: ${e.message}")
            false
        }
    }
    
    // Parse course HTML response - extracts all tables
    private fun parseCourseTables(response: String): ParsedCourseResult {
        return try {
            logToFile("Parsing course HTML response")
            logToFile("Response length: ${response.length}")
            logToFile("Response preview: ${response.take(500)}...")
            
            // First try to extract sanitized HTML using regex
            val match = Pattern.compile("pageSanitizer\\.sanitize\\('(.*)'\\);", Pattern.DOTALL).matcher(response)
            if (match.find()) {
                logToFile("Found pageSanitizer pattern - extracting encoded HTML")
                val encodedHtml = match.group(1) ?: ""
                logToFile("Extracted encoded HTML length: ${encodedHtml.length}")
                
                // Decode the HTML
                val decodedHtml = encodedHtml
                    .replace(Regex("\\\\x([0-9A-Fa-f]{2})")) { result ->
                        val hex = result.groupValues[1]
                        hex.toInt(16).toChar().toString()
                    }
                    .replace("\\\\", "")
                    .replace("\\'", "'")

                logToFile("Decoded HTML length: ${decodedHtml.length}")
                
                // Parse with Jsoup and extract all tables
                val doc: Document = Jsoup.parse(decodedHtml)
                return extractAllCourseTables(doc)
            } else {
                logToFile("No pageSanitizer pattern found - parsing raw HTML directly")
                // Parse the raw HTML directly
                val doc: Document = Jsoup.parse(response)
                return extractAllCourseTables(doc)
            }
            
        } catch (error: Exception) {
            logToFile("ERROR parsing course tables: ${error.message}")
            logToFile("Stack trace: ${error.stackTraceToString()}")
            ParsedCourseResult(error = "Failed to parse course tables: ${error.message}", status = 500)
        }
    }
    
    // Extract all tables from course HTML document
    private fun extractAllCourseTables(doc: Document): ParsedCourseResult {
        return try {
            logToFile("Extracting all tables from course HTML document")
            
            val allTables = doc.select("table")
            logToFile("Found ${allTables.size} tables in the course document")
            
            if (allTables.isEmpty()) {
                logToFile("WARNING: No tables found in course HTML")
                logToFile("Document title: ${doc.title()}")
                logToFile("Document body preview: ${doc.body()?.text()?.take(200)}...")
                return ParsedCourseResult(error = "No tables found in course HTML", status = 500)
            }
            
            val tableDataList = mutableListOf<TableData>()
            
            // Extract all tables
            for (tableIndex in allTables.indices) {
                val table = allTables[tableIndex]
                val tableNumber = tableIndex + 1 // 1-based for display
                logToFile("Processing course table ${tableNumber} (index ${tableIndex})")
                
                // Extract all data rows (including header row)
                val rows = table.select("tr")
                val dataRows = mutableListOf<List<String>>()
                
                // Include all rows (including headers)
                for (i in 0 until rows.size) {
                    val row = rows[i]
                    val cells = row.select("td, th")
                    val rowData = cells.map { it.text().trim() }
                    
                    if (rowData.isNotEmpty()) {
                        dataRows.add(rowData)
                    }
                }
                
                // Print all rows for this table
                dataRows.forEachIndexed { rowIndex, rowData ->
                    logToFile("Course Table ${tableNumber} Row ${rowIndex + 1}: $rowData")
                }
                
                val tableData = TableData(
                    tableIndex = tableIndex,
                    headers = emptyList(), // No headers
                    rows = dataRows,
                    tableAttributes = emptyMap() // No attributes
                )
                
                tableDataList.add(tableData)
            }
            
            logToFile("Successfully extracted ${tableDataList.size} course tables")
            
            // Convert to JSON for the response
            val allTablesResult = AllTablesResult(tables = tableDataList, status = 200)
            val jsonData = gson.toJson(allTablesResult)
            
            logToFile("Course JSON data length: ${jsonData.length}")
            logToFile("Course JSON preview: ${jsonData.take(200)}...")
            
            return ParsedCourseResult(
                tablesData = jsonData,
                status = 200
            )
            
        } catch (error: Exception) {
            logToFile("ERROR extracting course tables: ${error.message}")
            logToFile("Stack trace: ${error.stackTraceToString()}")
            return ParsedCourseResult(error = "Failed to extract course tables: ${error.message}", status = 500)
        }
    }

    suspend fun fetchTimetable(): TimetableResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if timetable data is cached for today
                if (isTimetableDataCachedToday()) {
                    logToFile("Timetable data is cached for today - using cached data")
                    val cachedData = getCachedTimetableData()
                    if (cachedData != null) {
                        logToFile("SUCCESS: Using cached timetable data")
                        return@withContext TimetableResult(
                            data = cachedData,
                            error = null,
                            status = 200
                        )
                    } else {
                        logToFile("ERROR: Failed to retrieve cached timetable data")
                    }
                }
                
                // Get cached credentials
                val cachedCredentials = getCachedCredentials()
                if (cachedCredentials == null) {
                    logToFile("ERROR: No cached credentials found")
                    return@withContext TimetableResult(
                        data = null,
                        error = "No cached credentials found. Please login first.",
                        status = -1
                    )
                }
                
                val timetableUrl = DynamicUrl.timetableDynamicUrl()
                println("DEBUG: Using timetable URL: $timetableUrl")
                logToFile("Fetching fresh timetable data from: $timetableUrl")
                
                val request = Request.Builder()
                    .url(timetableUrl)
                    .addHeader("Cookie", cachedCredentials.cookies)
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .addHeader("Sec-Ch-Ua", "\"Chromium\";v=\"136\", \"Google Chrome\";v=\"136\", \"Not.A/Brand\";v=\"99\"")
                    .addHeader("Sec-Ch-Ua-Mobile", "?1")
                    .addHeader("Sec-Ch-Ua-Platform", "\"Android\"")
                    .addHeader("Sec-Fetch-Dest", "empty")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Referer", "https://academia.srmist.edu.in/")
                    .addHeader("Referrer-Policy", "strict-origin-when-cross-origin")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseString = response.body?.string() ?: ""
                    println("DEBUG: Timetable response received, length: ${responseString.length}")
                    logToFile("Timetable response received, length: ${responseString.length}")
                    
                    // Cache the timetable data
                    cacheTimetableData(responseString)
                    
                    TimetableResult(
                        data = responseString,
                        error = null,
                        status = response.code
                    )
                } else {
                    logToFile("ERROR: Timetable HTTP ${response.code}: ${response.message}")
                    TimetableResult(
                        data = null,
                        error = "HTTP ${response.code}: ${response.message}",
                        status = response.code
                    )
                }
            } catch (e: Exception) {
                logToFile("ERROR: Timetable network error: ${e.message}")
                TimetableResult(
                    data = null,
                    error = "Network error: ${e.message}",
                    status = -1
                )
            }
        }
    }

    fun parseTimetable(response: String): TimetableResponse? {
        try {
            println("DEBUG: Raw response length: ${response.length}")
            println("DEBUG: Raw response preview: ${response.take(500)}...")
            
            // 1. Extract sanitized HTML using regex
            val match = Regex("pageSanitizer\\.sanitize\\('(.*)'\\);", RegexOption.DOT_MATCHES_ALL).find(response)
            if (match == null || match.groups[1]?.value == null) {
                return TimetableResponse(error = "Failed to extract timetable details", status = 500)
            }

            val encodedHtml = match.groups[1]?.value ?: ""
            println("DEBUG: Encoded HTML length: ${encodedHtml.length}")

            // 2. Decode the HTML
            val decodedHtml = encodedHtml
                .replace(Regex("\\\\x([0-9A-Fa-f]{2})")) { result ->
                    val hex = result.groupValues[1]
                    hex.toInt(16).toChar().toString()
                }
                .replace("\\\\", "")
                .replace("\\'", "'")

            println("DEBUG: Decoded HTML length: ${decodedHtml.length}")

            // 3. Parse with Jsoup
            val doc = Jsoup.parse(decodedHtml)

            // 4. Extract all tables from the HTML
            val allTables = doc.select("table")
            println("DEBUG: Found ${allTables.size} tables in HTML")
            
            // 5. Convert all tables to HTML strings for display
            val tableHtmlList = allTables.mapIndexed { index, table ->
                "Table ${index + 1}:\n${table.outerHtml()}"
            }
            
            // 6. Join all tables into a single HTML string
            val allTablesHtml = tableHtmlList.joinToString("\n\n")
            
            // 7. Create a simple response with all tables
            val result = TimetableResponse(
                status = 200, 
                rawHtml = allTablesHtml,
                tableCount = allTables.size
            )
            
            // 8. Cache the timetable data
            cacheTimetableData(response)
            
            println("DEBUG: Extracted ${allTables.size} tables")
            return result
        } catch (e: Exception) {
            println("ERROR parsing timetable: ${e.message}")
            e.printStackTrace()
            return TimetableResponse(error = "Failed to parse timetable: ${e.message}", status = 500)
        }
    }
    
    fun cacheTimetableData(timetableData: String) {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs?.edit()
                ?.putString("cached_timetable_data", timetableData)
                ?.putString("cached_timetable_data_timestamp", currentDate)
                ?.apply()
            logToFile("Timetable data cached successfully on $currentDate")
        } catch (e: Exception) {
            logToFile("Failed to save cached timetable data: ${e.message}")
        }
    }
    
    fun getCachedTimetableData(): String? {
        return try {
            prefs?.getString("cached_timetable_data", null)
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached timetable data: ${e.message}")
            null
        }
    }
    
    fun isTimetableDataCached(): Boolean {
        return try {
            val cachedData = prefs?.getString("cached_timetable_data", null)
            cachedData != null
        } catch (e: Exception) {
            logToFile("Failed to check if timetable data is cached: ${e.message}")
            false
        }
    }
    
    fun isTimetableDataCachedToday(): Boolean {
        return try {
            val cachedTimestamp = prefs?.getString("cached_timetable_data_timestamp", null)
            if (cachedTimestamp == null) return false
            
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = cachedTimestamp == currentDate
            logToFile("Timetable data cached on: $cachedTimestamp, today: $currentDate, isToday: $isToday")
            isToday
        } catch (e: Exception) {
            logToFile("Failed to check if timetable data is cached today: ${e.message}")
            false
        }
    }
    
    fun clearCachedTimetableData() {
        try {
            prefs?.edit()
                ?.remove("cached_timetable_data")
                ?.remove("cached_timetable_data_timestamp")
                ?.apply()
            logToFile("Cached timetable data cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached timetable data: ${e.message}")
        }
    }
    
    fun clearCachedCalendarData() {
        try {
            prefs?.edit()
                ?.remove("cached_calendar_data")
                ?.remove("cached_calendar_data_timestamp")
                ?.apply()
            logToFile("Cached calendar data cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached calendar data: ${e.message}")
        }
    }
    
    // Cache table 2 (student information) specifically
    fun saveCachedTable2Data(table2Data: String) {
        try {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            prefs?.edit()
                ?.putString("cached_table2_data", table2Data)
                ?.putString("cached_table2_data_timestamp", currentDate)
                ?.apply()
            logToFile("Table 2 data (student info) cached successfully on $currentDate")
        } catch (e: Exception) {
            logToFile("Failed to save cached table 2 data: ${e.message}")
        }
    }
    
    fun getCachedTable2Data(): String? {
        return try {
            prefs?.getString("cached_table2_data", null)
        } catch (e: Exception) {
            logToFile("Failed to retrieve cached table 2 data: ${e.message}")
            null
        }
    }
    
    fun isTable2DataCached(): Boolean {
        return try {
            val cachedData = prefs?.getString("cached_table2_data", null)
            cachedData != null
        } catch (e: Exception) {
            logToFile("Failed to check if table 2 data is cached: ${e.message}")
            false
        }
    }
    
    fun isTable2DataCachedToday(): Boolean {
        return try {
            val cachedTimestamp = prefs?.getString("cached_table2_data_timestamp", null)
            if (cachedTimestamp == null) return false
            
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isToday = cachedTimestamp == currentDate
            logToFile("Table 2 data cached on: $cachedTimestamp, today: $currentDate, isToday: $isToday")
            isToday
        } catch (e: Exception) {
            logToFile("Failed to check if table 2 data is cached today: ${e.message}")
            false
        }
    }
    
    fun clearCachedTable2Data() {
        try {
            prefs?.edit()
                ?.remove("cached_table2_data")
                ?.remove("cached_table2_data_timestamp")
                ?.apply()
            logToFile("Cached table 2 data cleared")
        } catch (e: Exception) {
            logToFile("Failed to clear cached table 2 data: ${e.message}")
        }
    }
} 