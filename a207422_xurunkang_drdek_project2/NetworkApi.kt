package com.example.a207422_xurunkang_drdek_project1



import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// 1. 定义 API 返回的数据结构 (我们要抓取商品状态、名称和品牌)
data class ProductResponse(
    val status: Int,
    val product: ProductDetails?
)

data class ProductDetails(
    val product_name: String?,
    val brands: String?
)

// 2. 定义 Retrofit 请求接口
interface OpenFoodFactsApi {
    // 动态替换 URL 中的条形码
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): ProductResponse
}

// 3. 创建全局单例的 Retrofit 引擎
object NetworkModule {
    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}