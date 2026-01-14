package com.yodgorbek.trendoraai.backend.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// -------------------- MODELS (Backend Response) --------------------

@Serializable
data class CoinMin(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    @SerialName("current_price") val currentPrice: Double,
    @SerialName("market_cap") val marketCap: Long,
    @SerialName("market_cap_rank") val marketCapRank: Int,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double?
)

@Serializable
data class CoinDetailFull(
    val id: String,
    val name: String,
    val symbol: String,
    val image: ImageUrl?,
    val description: Description?,
    @SerialName("market_data") val marketData: MarketData?
)

@Serializable data class ImageUrl(val large: String?)
@Serializable data class Description(val en: String?)

@Serializable
data class MarketData(
    @SerialName("current_price") val currentPrice: Map<String, Double>,
    @SerialName("market_cap") val marketCap: Map<String, Long>,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double?
)

@Serializable
data class HistoryResponse(val prices: List<List<Double>>)

@Serializable
data class AiResponse(
    val trend: String,
    val confidence: Int,
    val explanation: String,
    @SerialName("predicted_prices") val predictedPrices: List<Double>
)

@Serializable
data class CoinFullResponse(
    val coin: CoinDetailFull,
    val history: HistoryResponse,
    val ai: AiResponse
)

// -------------------- COINCAP INTERNAL DTOS --------------------

@Serializable
data class CoinCapListResponse(val data: List<CoinCapAsset>, val timestamp: Long)

@Serializable
data class CoinCapDetailResponse(val data: CoinCapAsset, val timestamp: Long)

@Serializable
data class CoinCapAsset(
    val id: String,
    val rank: String,
    val symbol: String,
    val name: String,
    val supply: String?,
    val maxSupply: String?,
    val marketCapUsd: String?,
    val volumeUsd24Hr: String?,
    val priceUsd: String?,
    val changePercent24Hr: String?,
    val vwap24Hr: String?
)

@Serializable
data class CoinCapHistoryResponse(val data: List<CoinCapHistoryPoint>, val timestamp: Long)

@Serializable
data class CoinCapHistoryPoint(
    val priceUsd: String,
    val time: Long
)

// -------- GROQ DTO --------

@Serializable
data class GroqReq(
    val model: String = "llama3-70b-8192",
    val messages: List<GMessage>,
    @SerialName("response_format") val responseFormat: GFormat = GFormat("json_object")
)

@Serializable data class GMessage(val role: String, val content: String)
@Serializable data class GFormat(val type: String)
@Serializable data class GroqRes(val choices: List<GChoice>)
@Serializable data class GChoice(val message: GMessage)

@Serializable
data class GroqPredictionResponse(
    val trend: String,
    val confidence: Int,
    val explanation: String,
    @SerialName("predicted_prices") val predictedPrices: List<Double>
)

// -------------------- SERVICE --------------------

object CoinService {

    private val client = HttpClient(Apache) {
        engine {
            followRedirects = true
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private const val BASE_URL = "https://api.coincap.io/v2"

    // ---- CACHES ----
    private val coinListCache = CacheEntry<List<CoinMin>>(60_000)
    private val coinDetailCache = ConcurrentHashMap<String, CacheEntry<CoinDetailFull>>()
    private val historyCache = ConcurrentHashMap<String, CacheEntry<HistoryResponse>>()
    private val aiCache = ConcurrentHashMap<String, CacheEntry<AiResponse>>()

    // ---------------- COINS ----------------

    suspend fun getCoins(): List<CoinMin> {
        if (coinListCache.isValid()) return coinListCache.data!!

        return try {
            val response: CoinCapListResponse = client.get("$BASE_URL/assets?limit=50") {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 TrendoraAI/1.0")
            }.body()
            
            val coins = response.data.map { asset ->
                CoinMin(
                    id = asset.id,
                    symbol = asset.symbol,
                    name = asset.name,
                    image = "https://assets.coincap.io/assets/icons/${asset.symbol.lowercase()}@2x.png",
                    currentPrice = asset.priceUsd?.toDoubleOrNull() ?: 0.0,
                    marketCap = asset.marketCapUsd?.toDoubleOrNull()?.toLong() ?: 0L,
                    marketCapRank = asset.rank.toIntOrNull() ?: 0,
                    priceChangePercentage24h = asset.changePercent24Hr?.toDoubleOrNull()
                )
            }

            coinListCache.update(coins)
            coins
        } catch (e: Exception) {
            println("CoinCap List Error: ${e.message}")
            e.printStackTrace() // Print full stack trace to logs
            // Return empty list, but log error.
            // If cache exists (even expired), maybe return it?
            coinListCache.data ?: emptyList()
        }
    }

    suspend fun getCoinDetail(id: String): CoinDetailFull? {
        val cache = coinDetailCache[id]
        if (cache != null && cache.isValid()) return cache.data

        return try {
            val response: CoinCapDetailResponse = client.get("$BASE_URL/assets/$id") {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 TrendoraAI/1.0")
            }.body()
            val asset = response.data

            val coin = CoinDetailFull(
                id = asset.id,
                name = asset.name,
                symbol = asset.symbol,
                image = ImageUrl("https://assets.coincap.io/assets/icons/${asset.symbol.lowercase()}@2x.png"),
                description = Description("Description not available via CoinCap"),
                marketData = MarketData(
                    currentPrice = mapOf("usd" to (asset.priceUsd?.toDoubleOrNull() ?: 0.0)),
                    marketCap = mapOf("usd" to (asset.marketCapUsd?.toDoubleOrNull()?.toLong() ?: 0L)),
                    priceChangePercentage24h = asset.changePercent24Hr?.toDoubleOrNull()
                )
            )

            coinDetailCache[id] = CacheEntry(60_000, coin)
            coin
        } catch (e: Exception) {
            println("CoinCap Detail Error: ${e.message}")
            e.printStackTrace()
            cache?.data
        }
    }

    suspend fun getHistory(id: String, days: String): HistoryResponse {
        val key = "$id-$days"
        val cache = historyCache[key]
        if (cache != null && cache.isValid()) return cache.data!!

        return try {
            val interval = when(days) {
                "1" -> "h1"
                "7" -> "h6"
                "30" -> "h12"
                else -> "d1"
            }

            val response: CoinCapHistoryResponse = client.get("$BASE_URL/assets/$id/history") {
                parameter("interval", interval)
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 TrendoraAI/1.0")
            }.body()

            val prices = response.data.map { point ->
                listOf(point.time.toDouble(), point.priceUsd.toDouble())
            }

            val history = HistoryResponse(prices)
            historyCache[key] = CacheEntry(300_000, history)
            history
        } catch (e: Exception) {
            println("History Error: ${e.message}")
            cache?.data ?: HistoryResponse(emptyList())
        }
    }

    // ---------------- AI ----------------

    suspend fun getAiPrediction(id: String, days: String): AiResponse {
        val key = "$id-$days"
        val cache = aiCache[key]
        if (cache != null && cache.isValid()) return cache.data!!

        val history = getHistory(id, days)
        val prices = history.prices.map { it[1] }.takeLast(30)

        // If not enough data
        if (prices.isEmpty()) {
            return AiResponse("Neutral", 0, "Insufficient data", emptyList())
        }

        val prompt = """
            Analyze crypto price history: $prices.
            Predict trend for next 30 days.
            Return JSON:
            {
              "trend": "Bullish" | "Bearish" | "Neutral",
              "confidence": 0-100,
              "explanation": "short text",
              "predicted_prices": [next 5 prices]
            }
        """.trimIndent()

        return try {
            val response: GroqRes =
                client.post("https://api.groq.com/openai/v1/chat/completions") {
                    header("Authorization", "Bearer ${System.getenv("GROQ_API_KEY")}")
                    contentType(ContentType.Application.Json)
                    setBody(GroqReq(messages = listOf(GMessage("user", prompt))))
                }.body()

            val content = response.choices.first().message.content
            val dto = Json.decodeFromString<GroqPredictionResponse>(content)

            val result = AiResponse(dto.trend, dto.confidence, dto.explanation, dto.predictedPrices)
            aiCache[key] = CacheEntry(6 * 60 * 60 * 1000, result)
            result
        } catch (e: Exception) {
            println("Groq Error: ${e.message}")
            AiResponse("Neutral", 50, "AI temporarily unavailable", emptyList())
        }
    }

    suspend fun getCoinFullData(id: String, days: String): CoinFullResponse {
        val coin = getCoinDetail(id) ?: throw Exception("Coin not found")
        val history = getHistory(id, days)
        val ai = getAiPrediction(id, days)

        return CoinFullResponse(coin, history, ai)
    }
}

// ---------------- CACHE ----------------

class CacheEntry<T>(private val ttlMs: Long, var data: T? = null) {
    private var timestamp = 0L

    fun isValid(): Boolean = data != null && System.currentTimeMillis() - timestamp < ttlMs

    fun update(newData: T) {
        data = newData
        timestamp = System.currentTimeMillis()
    }
}
